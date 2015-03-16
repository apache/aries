/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.aries.subsystem.ContentHandler;
import org.apache.aries.subsystem.core.archive.ExportPackageCapability;
import org.apache.aries.subsystem.core.archive.ExportPackageHeader;
import org.apache.aries.subsystem.core.archive.ProvideCapabilityCapability;
import org.apache.aries.subsystem.core.archive.ProvideCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemExportServiceCapability;
import org.apache.aries.subsystem.core.archive.SubsystemExportServiceHeader;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartAction extends AbstractAction {
	private static final Logger logger = LoggerFactory.getLogger(StartAction.class);

	private final Coordination coordination;
	private final BasicSubsystem instigator;

	public StartAction(BasicSubsystem instigator, BasicSubsystem requestor, BasicSubsystem target) {
		this(instigator, requestor, target, null);
	}

	public StartAction(BasicSubsystem instigator, BasicSubsystem requestor, BasicSubsystem target, Coordination coordination) {
		super(requestor, target, false);
		this.instigator = instigator;
		this.coordination = coordination;
	}

	@Override
	public Object run() {
		State state = target.getState();
		// The following states are illegal.
		if (EnumSet.of(State.INSTALL_FAILED, State.UNINSTALLED, State.UNINSTALLING).contains(state))
			throw new SubsystemException("Cannot stop from state " + state);
		// The following states must wait.
		if (EnumSet.of(State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING).contains(state)) {
			waitForStateChange(state);
			return new StartAction(instigator, requestor, target, coordination).run();
		}
		// The following states mean the requested state has already been attained.
		if (State.ACTIVE.equals(state))
			return null;
		// Always start if target is content of requestor.
		if (!Utils.isContent(requestor, target)) {
			// Aways start if target is a dependency of requestor.
			if (!Utils.isDependency(requestor, target)) {
				// Always start if instigator equals target (explicit start).
				if (!instigator.equals(target)) {
					// Don't start if instigator is root (restart) and target is not ready.
					if (instigator.isRoot() && !target.isReadyToStart()) {
						return null;
					}
				}
			}
		}
		Coordination coordination = this.coordination;
		if (coordination == null)
			coordination = Utils.createCoordination(target);
		try {
			// Resolve if necessary.
			if (State.INSTALLED.equals(state))
				resolve(target);
			target.setState(State.STARTING);
			// TODO Need to hold a lock here to guarantee that another start
			// operation can't occur when the state goes to RESOLVED.
			// Start the subsystem.
			List<Resource> resources = new ArrayList<Resource>(Activator.getInstance().getSubsystems().getResourcesReferencedBy(target));
			SubsystemContentHeader header = target.getSubsystemManifest().getSubsystemContentHeader();
			if (header != null)
				Collections.sort(resources, new StartResourceComparator(header));
			for (Resource resource : resources)
				startResource(resource, coordination);
			target.setState(State.ACTIVE);
		} catch (Throwable t) {
			coordination.fail(t);
			// TODO Need to reinstate complete isolation by disconnecting the
			// region and transition to INSTALLED.
		} finally {
			try {
				// Don't end the coordination if the subsystem being started
				// (i.e. the target) did not begin it.
				if (coordination.getName().equals(Utils.computeCoordinationName(target)))
					coordination.end();
			} catch (CoordinationException e) {
				target.setState(State.RESOLVED);
				Throwable t = e.getCause();
				if (t instanceof SubsystemException)
					throw (SubsystemException)t;
				throw new SubsystemException(t);
			}
		}
		return null;
	}

	private static Collection<Bundle> getBundles(BasicSubsystem subsystem) {
		Collection<Resource> constituents = Activator.getInstance().getSubsystems().getConstituents(subsystem);
		ArrayList<Bundle> result = new ArrayList<Bundle>(constituents.size());
		for (Resource resource : constituents) {
			if (resource instanceof BundleRevision)
				result.add(((BundleRevision)resource).getBundle());
		}
		result.trimToSize();
		return result;
	}

	private static void resolve(BasicSubsystem subsystem) {
		// Don't propagate a RESOLVING event if this is a persisted subsystem
		// that is already RESOLVED.
		if (State.INSTALLED.equals(subsystem.getState()))
			subsystem.setState(State.RESOLVING);
		try {
			// The root subsystem should follow the same event pattern for
			// state transitions as other subsystems. However, an unresolvable
			// root subsystem should have no effect, so there's no point in
			// actually doing the resolution work.
			if (!subsystem.isRoot()) {
				for (Subsystem child : Activator.getInstance().getSubsystems().getChildren(subsystem))
					resolve((BasicSubsystem)child);

				FrameworkWiring frameworkWiring = Activator.getInstance().getBundleContext().getBundle(0)
						.adapt(FrameworkWiring.class);

				// TODO I think this is insufficient. Do we need both
				// pre-install and post-install environments for the Resolver?
				Collection<Bundle> bundles = getBundles(subsystem);
				if (!frameworkWiring.resolveBundles(bundles)) {
					//work out which bundles could not be resolved
					Collection<Bundle> unresolved = new ArrayList<Bundle>();
					StringBuilder diagnostics = new StringBuilder();
					diagnostics.append(String.format("Unable to resolve bundles for subsystem/version/id %s/%s/%s:\n", 
							subsystem.getSymbolicName(), subsystem.getVersion(), subsystem.getSubsystemId()));
					String fmt = "%d : STATE %s : %s : %s : %s";
					for(Bundle bundle:bundles){
						if((bundle.getState() & Bundle.RESOLVED) != Bundle.RESOLVED) {
							unresolved.add(bundle);
						}
						String state = null;
						switch(bundle.getState()) {
							case Bundle.ACTIVE :
								state = "ACTIVE";
								break;
							case Bundle.INSTALLED :
								state = "INSTALLED";
								break;
							case Bundle.RESOLVED :
								state = "RESOLVED";
								break;
							case Bundle.STARTING :
								state = "STARTING";
								break;
							case Bundle.STOPPING :
								state = "STOPPING";
								break;
							case Bundle.UNINSTALLED :
								state = "UNINSTALLED";
								break;
							default :
								//convert common states to text otherwise default to just showing the ID
								state = "[" + Integer.toString(bundle.getState()) + "]";
								break;
						}
						diagnostics.append(String.format(fmt, bundle.getBundleId(), state, 
								bundle.getSymbolicName(), bundle.getVersion().toString(), 
								bundle.getLocation()));
						diagnostics.append("\n");
					}
					logger.error(diagnostics.toString());
					throw new SubsystemException("Framework could not resolve the bundles: " + unresolved);
				}
				setExportIsolationPolicy(subsystem);
			}
			// No need to propagate a RESOLVED event if this is a persisted
			// subsystem already in the RESOLVED state.
			if (State.RESOLVING.equals(subsystem.getState()))
				subsystem.setState(State.RESOLVED);
		}
		catch (Throwable t) {
			subsystem.setState(State.INSTALLED);
			if (t instanceof SubsystemException)
				throw (SubsystemException)t;
			throw new SubsystemException(t);
		}
	}

	private static void setExportIsolationPolicy(BasicSubsystem subsystem) throws InvalidSyntaxException, IOException, BundleException, URISyntaxException, ResolutionException {
		if (!subsystem.isComposite())
			return;
		Region from = ((BasicSubsystem)subsystem.getParents().iterator().next()).getRegion();
		Region to = subsystem.getRegion();
		RegionFilterBuilder builder = from.getRegionDigraph().createRegionFilterBuilder();
		setExportIsolationPolicy(builder, subsystem.getDeploymentManifest().getExportPackageHeader(), subsystem);
		setExportIsolationPolicy(builder, subsystem.getDeploymentManifest().getProvideCapabilityHeader(), subsystem);
		setExportIsolationPolicy(builder, subsystem.getDeploymentManifest().getSubsystemExportServiceHeader(), subsystem);
		RegionFilter regionFilter = builder.build();
		if (regionFilter.getSharingPolicy().isEmpty())
			return;
		if (logger.isDebugEnabled())
			logger.debug("Establishing region connection: from=" + from
					+ ", to=" + to + ", filter=" + regionFilter);
		from.connectRegion(to, regionFilter);
	}

	private static void setExportIsolationPolicy(RegionFilterBuilder builder, ExportPackageHeader header, BasicSubsystem subsystem) throws InvalidSyntaxException {
		if (header == null)
			return;
		String policy = RegionFilter.VISIBLE_PACKAGE_NAMESPACE;
		for (ExportPackageCapability capability : header.toCapabilities(subsystem)) {
			StringBuilder filter = new StringBuilder("(&");
			for (Entry<String, Object> attribute : capability.getAttributes().entrySet())
				filter.append('(').append(attribute.getKey()).append('=').append(attribute.getValue()).append(')');
			filter.append(')');
			if (logger.isDebugEnabled())
				logger.debug("Allowing " + policy + " of " + filter);
			builder.allow(policy, filter.toString());
		}
	}

	private static void setExportIsolationPolicy(RegionFilterBuilder builder, ProvideCapabilityHeader header, BasicSubsystem subsystem) throws InvalidSyntaxException {
		if (header == null)
			return;
		for (ProvideCapabilityHeader.Clause clause : header.getClauses()) {
			ProvideCapabilityCapability capability = new ProvideCapabilityCapability(clause, subsystem);
			String policy = capability.getNamespace();
			StringBuilder filter = new StringBuilder("(&");
			for (Entry<String, Object> attribute : capability.getAttributes().entrySet())
				filter.append('(').append(attribute.getKey()).append('=').append(attribute.getValue()).append(')');
			filter.append(')');
			if (logger.isDebugEnabled())
				logger.debug("Allowing " + policy + " of " + filter);
			builder.allow(policy, filter.toString());
		}
	}

	private static void setExportIsolationPolicy(RegionFilterBuilder builder, SubsystemExportServiceHeader header, BasicSubsystem subsystem) throws InvalidSyntaxException {
		if (header == null)
			return;
		String policy = RegionFilter.VISIBLE_SERVICE_NAMESPACE;
		for (SubsystemExportServiceHeader.Clause clause : header.getClauses()) {
			SubsystemExportServiceCapability capability = new SubsystemExportServiceCapability(clause, subsystem);
			String filter = capability.getDirectives().get(SubsystemExportServiceCapability.DIRECTIVE_FILTER);
			if (logger.isDebugEnabled())
				logger.debug("Allowing " + policy + " of " + filter);
			builder.allow(policy, filter.toString());
		}
	}

	private void startBundleResource(Resource resource, Coordination coordination) throws BundleException {
		if (target.isRoot())
			// Starting the root subsystem should not affect bundles within the
			// root region.
			return;
		if (Utils.isRegionContextBundle(resource))
			// The region context bundle was persistently started elsewhere.
			return;
		final Bundle bundle = ((BundleRevision)resource).getBundle();

		if ((bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0)
			return;

		if (logger.isDebugEnabled()) {
			int bundleStartLevel = bundle.adapt(BundleStartLevel.class).getStartLevel();
			Bundle systemBundle=Activator.getInstance().getBundleContext().getBundle(0);
			int fwStartLevel = systemBundle.adapt(FrameworkStartLevel.class).getStartLevel();
			logger.debug("StartAction: starting bundle " + bundle.getSymbolicName()
				+ " " + bundle.getVersion().toString()
				+ " bundleStartLevel=" + bundleStartLevel
				+ " frameworkStartLevel=" + fwStartLevel);
		}

		bundle.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);

		if (logger.isDebugEnabled()) {
			logger.debug("StartAction: bundle " + bundle.getSymbolicName()
				+ " " + bundle.getVersion().toString()
				+ " started correctly");
		}

		if (coordination == null)
			return;
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
			}

			public void failed(Coordination coordination) throws Exception {
				bundle.stop();
			}
		});
	}

	private void startResource(Resource resource, Coordination coordination) throws BundleException, IOException {
		String type = ResourceHelper.getTypeAttribute(resource);
		if (SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type)) {
			startSubsystemResource(resource, coordination);
	    } else if (IdentityNamespace.TYPE_BUNDLE.equals(type)) {
			startBundleResource(resource, coordination);
	    } else if (IdentityNamespace.TYPE_FRAGMENT.equals(type)) {
			// Fragments are not started.
		} else {
		    if (!startCustomHandler(resource, type, coordination))
		        throw new SubsystemException("Unsupported resource type: " + type);
		}
	}

    private boolean startCustomHandler(Resource resource, String type, Coordination coordination) {
        ServiceReference<ContentHandler> customHandlerRef = CustomResources.getCustomContentHandler(target, type);
        if (customHandlerRef != null) {
            ContentHandler customHandler = target.getBundleContext().getService(customHandlerRef);
            if (customHandler != null) {
                try {
                    customHandler.start(ResourceHelper.getSymbolicNameAttribute(resource), type, target, coordination);
                    return true;
                } finally {
                    target.getBundleContext().ungetService(customHandlerRef);
                }
            }
        }
        return false;
    }

	private void startSubsystemResource(Resource resource, Coordination coordination) throws IOException {
		final BasicSubsystem subsystem = (BasicSubsystem)resource;
		// Subsystems that are content resources of another subsystem must have
		// their autostart setting set to started.
		if (Utils.isContent(this.target, subsystem))
			subsystem.setAutostart(true);
		new StartAction(instigator, target, subsystem, coordination).run();
		if (coordination == null)
			return;
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
			}

			public void failed(Coordination coordination) throws Exception {
				new StopAction(target, subsystem, !subsystem.isRoot()).run();
			}
		});
	}
}

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
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.aries.subsystem.ContentHandler;
import org.apache.aries.subsystem.core.archive.ExportPackageCapability;
import org.apache.aries.subsystem.core.archive.ExportPackageHeader;
import org.apache.aries.subsystem.core.archive.ProvideCapabilityCapability;
import org.apache.aries.subsystem.core.archive.ProvideCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemExportServiceCapability;
import org.apache.aries.subsystem.core.archive.SubsystemExportServiceHeader;
import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
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
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartAction extends AbstractAction {
	public static enum Restriction {
		NONE,
		INSTALL_ONLY,
		RESOLVE_ONLY
	}
	
	private static final Logger logger = LoggerFactory.getLogger(StartAction.class);

	private final Coordination coordination;
	private final BasicSubsystem instigator;
	private final Restriction restriction;

	public StartAction(BasicSubsystem instigator, BasicSubsystem requestor, BasicSubsystem target) {
		this(instigator, requestor, target, Restriction.NONE);
	}

	public StartAction(BasicSubsystem instigator, BasicSubsystem requestor, BasicSubsystem target, Restriction restriction) {
		this(instigator, requestor, target, null, restriction);
	}
	
	public StartAction(BasicSubsystem instigator, BasicSubsystem requestor, BasicSubsystem target, Coordination coordination) {
		this(instigator, requestor, target, coordination, Restriction.NONE);
	}
	
	public StartAction(BasicSubsystem instigator, BasicSubsystem requestor, BasicSubsystem target, Coordination coordination, Restriction restriction) {
		super(requestor, target, false);
		this.instigator = instigator;
		this.coordination = coordination;
		this.restriction = restriction;
	}
	
	private static boolean isTargetStartable(BasicSubsystem instigator, BasicSubsystem requestor, BasicSubsystem target) {
		State state = target.getState();
	    // The following states are illegal.
	    if (EnumSet.of(State.INSTALL_FAILED, State.UNINSTALLED).contains(state))
	        throw new SubsystemException("Cannot start from state " + state);
	    // The following states mean the requested state has already been attained.
	    if (State.ACTIVE.equals(state))
	        return false;
		// Always start if target is content of requestor.
	    if (!Utils.isContent(requestor, target)) {
	        // Always start if target is a dependency of requestor.
	        if (!Utils.isDependency(requestor, target)) {
	            // Always start if instigator equals target (explicit start).
	            if (!instigator.equals(target)) {
	                // Don't start if instigator is root (restart) and target is not ready.
	                if (instigator.isRoot() && !target.isReadyToStart()) {
	                    return false;
	                }
	            }
	        }
	    }
	    return true;
	}
	
	private void installDependencies(BasicSubsystem target, Coordination coordination) throws Exception {
		for (Subsystem parent : target.getParents()) {
			AccessController.doPrivileged(new StartAction(instigator, target, (BasicSubsystem)parent, coordination, Restriction.INSTALL_ONLY));
		}
		installDependencies(Collections.<Subsystem>singletonList(target), coordination);
		for (Subsystem child : Activator.getInstance().getSubsystems().getChildren(target)) {
			AccessController.doPrivileged(new StartAction(instigator, target, (BasicSubsystem)child, coordination, Restriction.INSTALL_ONLY));
		}
	}
	
	private static void installDependencies(Collection<Subsystem> subsystems, Coordination coordination) throws Exception {
		for (Subsystem subsystem : subsystems) {
			if (State.INSTALLING.equals(subsystem.getState())) {
				BasicSubsystem bs = (BasicSubsystem)subsystem;
				bs.computeDependenciesPostInstallation(coordination);
				new InstallDependencies().install(bs, null, coordination);
				bs.setState(State.INSTALLED);
			}
		}
	}
	
	private Coordination createCoordination() {
		Coordination coordination = this.coordination;
	    if (coordination == null) {
	        coordination = Utils.createCoordination(target);
	    }
	    return coordination;
	}
	
	private static LinkedHashSet<BasicSubsystem> computeAffectedSubsystems(BasicSubsystem target) {
		LinkedHashSet<BasicSubsystem> result = new LinkedHashSet<BasicSubsystem>();
		Subsystems subsystems = Activator.getInstance().getSubsystems();
		for (Resource dep : subsystems.getResourcesReferencedBy(target)) {
			if (dep instanceof BasicSubsystem 
					&& !subsystems.getChildren(target).contains(dep)) {
				result.add((BasicSubsystem)dep);
			}
			else if (dep instanceof BundleRevision) {
				BundleConstituent constituent = new BundleConstituent(null, (BundleRevision)dep);
				if (!target.getConstituents().contains(constituent)) {
					for (BasicSubsystem constituentOf : subsystems.getSubsystemsByConstituent(
							new BundleConstituent(null, (BundleRevision)dep))) {
						result.add(constituentOf);
					}
				}
			}
		}
		for (Subsystem child : subsystems.getChildren(target)) {
			result.add((BasicSubsystem)child);
		}
		for (Resource resource : target.getResource().getSharedContent()) {
			for (BasicSubsystem constituentOf : subsystems.getSubsystemsByConstituent(
					resource instanceof BundleRevision ? new BundleConstituent(null, (BundleRevision)resource) : resource)) {
				result.add(constituentOf);
			}
		}
		result.add(target);
		return result;
	}

	@Override
	public Object run() {
		// Protect against re-entry now that cycles are supported.
		if (!LockingStrategy.set(State.STARTING, target)) {
			return null;
		}
		try {
			Collection<BasicSubsystem> subsystems;
			// We are now protected against re-entry.
			// If necessary, install the dependencies.
	    	if (State.INSTALLING.equals(target.getState()) && !Utils.isProvisionDependenciesInstall(target)) {
	    		// Acquire the global write lock while installing dependencies.
				LockingStrategy.writeLock();
				try {
					// We are now protected against installs, starts, stops, and uninstalls.
		    		// We need a separate coordination when installing 
					// dependencies because cleaning up the temporary export 
					// sharing policies must be done while holding the write lock.
		    		Coordination c = Utils.createCoordination(target);
		    		try {
		    			installDependencies(target, c);
		    			// Associated subsystems must be computed after all dependencies 
						// are installed because some of the dependencies may be 
						// subsystems. This is safe to do while only holding the read
						// lock since we know that nothing can be added or removed.
						subsystems = computeAffectedSubsystems(target);
						for (BasicSubsystem subsystem : subsystems) {
							if (State.INSTALLING.equals(subsystem.getState())
									&& !Utils.isProvisionDependenciesInstall(subsystem)) {
								installDependencies(subsystem, c);
							}
						}
						// Downgrade to the read lock in order to prevent 
		    			// installs and uninstalls but allow starts and stops.
						LockingStrategy.readLock();
		    		}
		    		catch (Throwable t) {
		    			c.fail(t);
		    		}
		    		finally {
		    			// This will clean up the temporary export sharing
		    			// policies. Must be done while holding the write lock.
		    			c.end();
		    		}
				}
				finally {
					// Release the global write lock as soon as possible.
					LockingStrategy.writeUnlock();
				}
	    	}
	    	else {
	    		// Acquire the read lock in order to prevent installs and
	    		// uninstalls but allow starts and stops.
	    		LockingStrategy.readLock();
	    	}
	    	try {
	    		// We now hold the read lock and are protected against installs
	    		// and uninstalls.
	    		if (Restriction.INSTALL_ONLY.equals(restriction)) {
					return null;
				}
	    		// Compute associated subsystems here in case (1) they weren't
	    		// computed previously while holding the write lock or (2) they
	    		// were computed previously and more were subsequently added. 
				// This is safe to do while only holding the read lock since we
				// know that nothing can be added or removed.
				subsystems = computeAffectedSubsystems(target);
				// Acquire the global mutual exclusion lock while acquiring the
				// state change locks of affected subsystems.
				LockingStrategy.lock();
				try {
					// We are now protected against cycles.
					// Acquire the state change locks of affected subsystems.
					LockingStrategy.lock(subsystems);
				}
				finally {
					// Release the global mutual exclusion lock as soon as possible.
					LockingStrategy.unlock();
				}
				Coordination coordination = this.coordination;
				try {
					coordination = createCoordination();
					// We are now protected against other starts and stops of the affected subsystems.
					if (!isTargetStartable(instigator, requestor, target)) {
						return null;
					}
					
					// Resolve if necessary.
					if (State.INSTALLED.equals(target.getState()))
						resolve(instigator, target, target, coordination, subsystems);
					if (Restriction.RESOLVE_ONLY.equals(restriction))
						return null;
					target.setState(State.STARTING);
					// Be sure to set the state back to RESOLVED if starting fails.
					coordination.addParticipant(new Participant() {
						@Override
						public void ended(Coordination coordination) throws Exception {
							// Nothing.
						}

						@Override
						public void failed(Coordination coordination) throws Exception {
							target.setState(State.RESOLVED);
						}
					});
					for (BasicSubsystem subsystem : subsystems) {
						if (!target.equals(subsystem)) {
							startSubsystemResource(subsystem, coordination);
						}
					}
					List<Resource> resources = new ArrayList<Resource>(Activator.getInstance().getSubsystems().getResourcesReferencedBy(target));
					SubsystemContentHeader header = target.getSubsystemManifest().getSubsystemContentHeader();
					if (header != null)
						Collections.sort(resources, new StartResourceComparator(header));
					for (Resource resource : resources)
						startResource(resource, coordination);
					target.setState(State.ACTIVE);
					
				}
				catch (Throwable t) {
					// We catch exceptions and fail the coordination here to
					// ensure we are still holding the state change locks when
					// the participant sets the state to RESOLVED.
					coordination.fail(t);
				}
				finally {
					try {
						// Don't end a coordination that was not begun as part
						// of this start action.
						if (coordination.getName().equals(Utils.computeCoordinationName(target))) {
							coordination.end();
						}
					}
					finally {
						// Release the state change locks of affected subsystems.
						LockingStrategy.unlock(subsystems);
					}
				}
	    	}
	    	finally {
				// Release the read lock.
				LockingStrategy.readUnlock();
			}
		}
		catch (CoordinationException e) {
			Throwable t = e.getCause();
			if (t == null) {
				throw new SubsystemException(e);
			}
			if (t instanceof SecurityException) {
				throw (SecurityException)t;
			}
			if (t instanceof SubsystemException) {
				throw (SubsystemException)t;
			}
			throw new SubsystemException(t);
		}
		finally {
			// Protection against re-entry no longer required.
			LockingStrategy.unset(State.STARTING, target);
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
	
	private static void emitResolvingEvent(BasicSubsystem subsystem) {
		// Don't propagate a RESOLVING event if this is a persisted subsystem
		// that is already RESOLVED.
		if (State.INSTALLED.equals(subsystem.getState()))
			subsystem.setState(State.RESOLVING);
	}
	
	private static void emitResolvedEvent(BasicSubsystem subsystem) {
		// No need to propagate a RESOLVED event if this is a persisted
		// subsystem already in the RESOLVED state.
		if (State.RESOLVING.equals(subsystem.getState()))
			subsystem.setState(State.RESOLVED);
	}
	
	private static void resolveSubsystems(BasicSubsystem instigator, BasicSubsystem target, Coordination coordination, Collection<BasicSubsystem> subsystems) throws Exception {
		for (BasicSubsystem subsystem : subsystems) {
			resolveSubsystem(instigator, target, subsystem, coordination);
		}
	}
	
	private static void resolveSubsystem(BasicSubsystem instigator, BasicSubsystem target, BasicSubsystem subsystem, Coordination coordination) throws Exception {
		State state = subsystem.getState();
		if (State.INSTALLED.equals(state)) {
			if (target.equals(subsystem)) {
				resolve(instigator, target, subsystem, coordination, Collections.<BasicSubsystem>emptyList());
			}
			else {
				AccessController.doPrivileged(new StartAction(instigator, target, subsystem, coordination, Restriction.RESOLVE_ONLY));
			}
		}
	}
	
	private static void resolveBundles(BasicSubsystem subsystem) {
		FrameworkWiring frameworkWiring = Activator.getInstance().getBundleContext().getBundle(0)
				.adapt(FrameworkWiring.class);
		// TODO I think this is insufficient. Do we need both
		// pre-install and post-install environments for the Resolver?
		Collection<Bundle> bundles = getBundles(subsystem);
		if (!frameworkWiring.resolveBundles(bundles)) {
			handleFailedResolution(subsystem, bundles, frameworkWiring);
		}
	}

	private static void resolve(BasicSubsystem instigator, BasicSubsystem target, BasicSubsystem subsystem, Coordination coordination, Collection<BasicSubsystem> subsystems) {
		emitResolvingEvent(subsystem);
		try {
			// The root subsystem should follow the same event pattern for
			// state transitions as other subsystems. However, an unresolvable
			// root subsystem should have no effect, so there's no point in
			// actually doing the resolution work.
			if (!subsystem.isRoot()) {
				setExportIsolationPolicy(subsystem, coordination);
				resolveSubsystems(instigator, target, coordination, subsystems);
				resolveBundles(subsystem);
			}
			emitResolvedEvent(subsystem);
		}
		catch (Throwable t) {
			subsystem.setState(State.INSTALLED);
			if (t instanceof SubsystemException)
				throw (SubsystemException)t;
			throw new SubsystemException(t);
		}
	}

	private static void setExportIsolationPolicy(final BasicSubsystem subsystem, Coordination coordination) throws InvalidSyntaxException {
		if (!subsystem.isComposite())
			return;
		final Region from = ((BasicSubsystem)subsystem.getParents().iterator().next()).getRegion();
		final Region to = subsystem.getRegion();
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
		try {
			from.connectRegion(to, regionFilter);
		}
		catch (BundleException e) {
			// TODO Assume this means that the export sharing policy has already
			// been set. Bad assumption?
			return;
		}
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination coordination) throws Exception {
				// It may be necessary to rollback the export sharing policy
				// even when the coordination did not fail. For example, this
				// might have been a subsystem whose export sharing policy was
				// set just in case it offered dependencies for some other
				// subsystem.
				unsetExportIsolationPolicyIfNecessary();
			}

			@Override
			public void failed(Coordination coordination) throws Exception {
				// Nothing to do because a coordination is always ended.
			}
			
			private void unsetExportIsolationPolicyIfNecessary() throws BundleException, InvalidSyntaxException {
				if (!EnumSet.of(State.INSTALLING, State.INSTALLED).contains(subsystem.getState())) {
					// The subsystem is either RESOLVED or ACTIVE and therefore
					// does not require a rollback.
					return;
				}
				// The subsystem is either INSTALLING or INSTALLED and therefore
				// requires a rollback since the export sharing policy must only
				// be set upon entering the RESOLVED state.
				RegionUpdater updater = new RegionUpdater(from, to);
				updater.addRequirements(null);
			}
		});
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
			Set<Entry<String, Object>> entrySet = capability.getAttributes().entrySet();
			StringBuilder filter = new StringBuilder();
			if (entrySet.size() > 1) {
				filter.append("(&");
			}
			for (Entry<String, Object> attribute : capability.getAttributes().entrySet()) {
				filter.append('(').append(attribute.getKey()).append('=').append(attribute.getValue()).append(')');
			}
			if (entrySet.size() > 1) {
				filter.append(')');
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Allowing policy {} with filter {}", policy, filter);
			}
			if (filter.length() == 0) {
				builder.allowAll(policy);
			}
			else {
				builder.allow(policy, filter.toString());
			}
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
			builder.allow(policy, filter);
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

	private void startSubsystemResource(Resource resource, final Coordination coordination) throws IOException {
		final BasicSubsystem subsystem = (BasicSubsystem)resource;
		if (!isTargetStartable(instigator, target, subsystem)) {
			 return;
		}
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
	
	private static void handleFailedResolution(BasicSubsystem subsystem, Collection<Bundle> bundles, FrameworkWiring wiring) {
			logFailedResolution(subsystem, bundles);
			throw new SubsystemException("Framework could not resolve the bundles: " + bundles);
	}
	
	private static void logFailedResolution(BasicSubsystem subsystem, Collection<Bundle> bundles) {
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
	}
	
	static void setExportPolicyOfAllInstallingSubsystemsWithProvisionDependenciesResolve(Coordination coordination) throws InvalidSyntaxException {
		for (BasicSubsystem subsystem : Activator.getInstance().getSubsystems().getSubsystems()) {
			if (!State.INSTALLING.equals(subsystem.getState())
					|| Utils.isProvisionDependenciesInstall(subsystem)) {
				continue;
			}
			setExportIsolationPolicy(subsystem, coordination);
		}
	}
}

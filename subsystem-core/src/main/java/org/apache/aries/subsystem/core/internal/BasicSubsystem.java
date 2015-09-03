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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.aries.subsystem.AriesSubsystem;
import org.apache.aries.subsystem.core.archive.AriesSubsystemParentsHeader;
import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.io.IOUtils;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicSubsystem implements Resource, AriesSubsystem {
	private static final Logger logger = LoggerFactory.getLogger(BasicSubsystem.class);
	
	public static final String ROOT_SYMBOLIC_NAME = "org.osgi.service.subsystem.root";
	public static final Version ROOT_VERSION = Version.parseVersion("1.0.0");
	public static final String ROOT_LOCATION = "subsystem://?"
			+ SubsystemConstants.SUBSYSTEM_SYMBOLICNAME + '='
			+ ROOT_SYMBOLIC_NAME + '&' + SubsystemConstants.SUBSYSTEM_VERSION
			+ '=' + ROOT_VERSION;
	
	private volatile Bundle regionContextBundle;
	
	private DeploymentManifest deploymentManifest;    
	private SubsystemResource resource;
	private SubsystemManifest subsystemManifest;
	
	private final IDirectory directory;
	
	public BasicSubsystem(SubsystemResource resource) throws URISyntaxException, IOException, BundleException, InvalidSyntaxException {
		this(resource, null);
	}
	
	public BasicSubsystem(SubsystemResource resource, InputStream deploymentManifest) throws URISyntaxException, IOException, BundleException, InvalidSyntaxException {
		this.resource = resource;
		final File file = new File(Activator.getInstance().getBundleContext().getDataFile(""), Long.toString(resource.getId()));
		file.mkdirs();
		Coordination coordination = Activator.getInstance().getCoordinator().peek();
		if (coordination != null) {
			coordination.addParticipant(new Participant() {
				@Override
				public void ended(Coordination c) throws Exception {
					// Nothing
				}

				@Override
				public void failed(Coordination c) throws Exception {
					IOUtils.deleteRecursive(file);
				}
			});
		}
		directory = FileSystem.getFSRoot(file);
		setSubsystemManifest(resource.getSubsystemManifest());
		SubsystemManifestValidator.validate(this, getSubsystemManifest());
		setDeploymentManifest(new DeploymentManifest.Builder()
				.manifest(resource.getSubsystemManifest())
				.manifest(deploymentManifest == null ? resource.getDeploymentManifest() : new DeploymentManifest(deploymentManifest))
				.location(resource.getLocation())
				.autostart(false)
				.id(resource.getId())
				.lastId(SubsystemIdentifier.getLastId())
				.region(resource.getRegion().getName())
				.state(State.INSTALLING)
				.build());
		setTranslations();
	}
	
	public BasicSubsystem(File file) throws IOException, URISyntaxException, ResolutionException {
		this(FileSystem.getFSRoot(file));
	}
	
	public BasicSubsystem(IDirectory directory) throws IOException, URISyntaxException, ResolutionException {
		this.directory = directory;
		setDeploymentManifest(new DeploymentManifest.Builder().manifest(getDeploymentManifest()).build());
	}
	
	/* BEGIN Resource interface methods. */
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof BasicSubsystem))
			return false;
		BasicSubsystem that = (BasicSubsystem)o;
		return getLocation().equals(that.getLocation());
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		SubsystemManifest manifest = getSubsystemManifest();
		List<Capability> result = manifest.toCapabilities(this);
		if (namespace != null)
			for (Iterator<Capability> i = result.iterator(); i.hasNext();)
				if (!i.next().getNamespace().equals(namespace))
					i.remove();
		// TODO Somehow, exposing the capabilities of content resources of a
		// feature is causing an infinite regression of feature2 installations
		// in FeatureTest.testSharedContent() under certain conditions.
		if (isScoped() || IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace))
			return result;
		SubsystemContentHeader header = manifest.getSubsystemContentHeader();
		for (Resource constituent : getConstituents())
			if (header.contains(constituent))
				for (Capability capability : constituent.getCapabilities(namespace))
					result.add(new BasicCapability(capability, this));
		return result;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		SubsystemManifest manifest = getSubsystemManifest();
		List<Requirement> result = manifest.toRequirements(this);
		if (namespace != null)
			for (Iterator<Requirement> i = result.iterator(); i.hasNext();)
				if (!i.next().getNamespace().equals(namespace))
					i.remove();
		if (isScoped())
			return result;
		SubsystemContentHeader header = manifest.getSubsystemContentHeader();
		for (Resource constituent : getConstituents())
			if (header.contains(constituent))
				for (Requirement requirement : constituent.getRequirements(namespace))
					result.add(new BasicRequirement(requirement, this));
		return result;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + getLocation().hashCode();
		return result;
	}
	
	/* END Resource interface methods. */
	
	/* BEGIN Subsystem interface methods. */

	@Override
	public BundleContext getBundleContext() {
		SecurityManager.checkContextPermission(this);
		return AccessController.doPrivileged(new GetBundleContextAction(this));
	}

	@Override
	public Collection<Subsystem> getChildren() {
		return Activator.getInstance().getSubsystems().getChildren(this);
	}

	@Override
	public Map<String, String> getSubsystemHeaders(Locale locale) {
		SecurityManager.checkMetadataPermission(this);
		return AccessController.doPrivileged(new GetSubsystemHeadersAction(this, locale));
	}

	@Override
	public String getLocation() {
		SecurityManager.checkMetadataPermission(this);
		return getDeploymentManifestHeaderValue(DeploymentManifest.ARIESSUBSYSTEM_LOCATION);
	}

	@Override
	public Collection<Subsystem> getParents() {
		AriesSubsystemParentsHeader header = getDeploymentManifest().getAriesSubsystemParentsHeader();
		if (header == null)
			return Collections.emptyList();
		Collection<Subsystem> result = new ArrayList<Subsystem>(header.getClauses().size());
		for (AriesSubsystemParentsHeader.Clause clause : header.getClauses()) {
			BasicSubsystem subsystem = Activator.getInstance().getSubsystems().getSubsystemById(clause.getId());
			if (subsystem == null)
				continue;
			result.add(subsystem);
		}
		return result;
	}

	@Override
	public Collection<Resource> getConstituents() {
		return Activator.getInstance().getSubsystems().getConstituents(this);
	}

	@Override
	public State getState() {
		return State.valueOf(getDeploymentManifestHeaderValue(DeploymentManifest.ARIESSUBSYSTEM_STATE));
	}

	@Override
	public long getSubsystemId() {
		return Long.parseLong(getDeploymentManifestHeaderValue(DeploymentManifest.ARIESSUBSYSTEM_ID));
	}

	@Override
	public String getSymbolicName() {
		return getSubsystemManifest().getSubsystemSymbolicNameHeader().getSymbolicName();
	}

	@Override
	public String getType() {
		return getSubsystemManifest().getSubsystemTypeHeader().getType();
	}

	@Override
	public Version getVersion() {
		return getSubsystemManifest().getSubsystemVersionHeader().getVersion();
	}

	@Override
	public AriesSubsystem install(String location) {
		return install(location, (InputStream)null);
	}

	@Override
	public AriesSubsystem install(String location, InputStream content) {
		return install(location, content, null);
	}

	@Override
	public void start() {
		SecurityManager.checkExecutePermission(this);
		// Changing the autostart setting must be privileged because of file IO.
		// It cannot be done within SartAction because we only want to change it
		// on an explicit start operation but StartAction is also used for
		// implicit operations.
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				setAutostart(true);
				return null;
			}
		});
		AccessController.doPrivileged(new StartAction(this, this, this));
	}

	@Override
	public void stop() {
		SecurityManager.checkExecutePermission(this);
		// Changing the autostart setting must be privileged because of file IO.
		// It cannot be done within StopAction because we only want to change it
		// on an explicit stop operation but StopAction is also used for
		// implicit operations.
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				setAutostart(false);
				return null;
			}
		});
		AccessController.doPrivileged(new StopAction(this, this, !isRoot()));
	}

	@Override
	public void uninstall() {
		SecurityManager.checkLifecyclePermission(this);
		AccessController.doPrivileged(new UninstallAction(this, this, false));
	}
	
	/* END Subsystem interface methods. */
	
	void addedConstituent(Resource resource, boolean referenced) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Adding constituent {} to deployment manifest...", resource);
			synchronized (this) {
				setDeploymentManifest(new DeploymentManifest.Builder()
						.manifest(getDeploymentManifest()).content(resource, referenced).build());
			}
			if (logger.isDebugEnabled())
				logger.debug("Added constituent {} to deployment manifest", resource);
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	void addedParent(BasicSubsystem subsystem, boolean referenceCount) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Adding parent {} to deployment manifest...", subsystem.getSymbolicName());
			synchronized (this) {
				setDeploymentManifest(new DeploymentManifest.Builder()
						.manifest(getDeploymentManifest()).parent(subsystem, referenceCount).build());
			}
			if (logger.isDebugEnabled())
				logger.debug("Added parent {} to deployment manifest", subsystem.getSymbolicName());
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	synchronized DeploymentManifest getDeploymentManifest() {
		if (deploymentManifest == null) {
			try {
				deploymentManifest = new DeploymentManifest(directory.getFile("OSGI-INF/DEPLOYMENT.MF").open());
			}
			catch (Throwable t) {
				throw new SubsystemException(t);
			}
		}
		return deploymentManifest;
	}
	
	File getDirectory() {
		try {
			return new File(directory.toURL().toURI());
		}
		catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	Region getRegion() {
	    Bundle bundle = regionContextBundle; // volatile variable
	    if (bundle == null) {
	        // At best, RegionDigraph.getRegion(String) is linear time.
	        // Continue to call this when necessary, however, as a fail safe.
	        return Activator.getInstance().getRegionDigraph().getRegion(getRegionName());
	    }
	    // RegionDigraph.getRegion(Bundle) is constant time.
	    return Activator.getInstance().getRegionDigraph().getRegion(bundle);
	}
	
	String getRegionName() {
		DeploymentManifest manifest = getDeploymentManifest();
		Header<?> header = manifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_REGION);
		if (header == null)
			return null;
		return header.getValue();
	}
	
	synchronized SubsystemResource getResource() {
		if (resource == null) {
			try {
				resource = new SubsystemResource(directory);
			}
			catch (Exception e) {
				throw new SubsystemException(e);
			}
			Collection<DeployedContentHeader.Clause> missingResources = resource.getMissingResources();
			if (!missingResources.isEmpty()) {
				if (isRoot())
					// We don't care if the root subsystem has missing resources
					// because they are either (1) extraneous bundles outside of
					// the subsystems API or (2) provisioned dependencies of
					// other subsystems. Those that fall in the latter category
					// will be detected by the dependent subsystems.
					removedContent(missingResources);
				else
					// If a non-root subsystem has missing dependencies, let's
					// fail fast for now.
					throw new SubsystemException("Missing resources: " + missingResources);
			}
		}
		return resource;
	}
	
	synchronized SubsystemManifest getSubsystemManifest() {
		if (subsystemManifest == null) {
			try {
				subsystemManifest = new SubsystemManifest(directory.getFile("OSGI-INF/SUBSYSTEM.MF").open());
			}
			catch (Throwable t) {
				throw new SubsystemException(t);
			}
		}
		return subsystemManifest;
	}
	
	boolean isApplication() {
		return getSubsystemManifest().getSubsystemTypeHeader().isApplication();
	}
	
	boolean isAutostart() {
		DeploymentManifest manifest = getDeploymentManifest();
		Header<?> header = manifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_AUTOSTART);
		return Boolean.valueOf(header.getValue());
	}
	
	boolean isComposite() {
		return getSubsystemManifest().getSubsystemTypeHeader().isComposite();
	}
	
	boolean isFeature() {
		return getSubsystemManifest().getSubsystemTypeHeader().isFeature();
	}
	
	boolean isReadyToStart() {
		if (isRoot())
			return true;
		for (Subsystem parent : getParents())
			if (EnumSet.of(State.STARTING, State.ACTIVE).contains(parent.getState()) && isAutostart())
				return true;
		return false;
	}
	
	boolean isReferenced(Resource resource) {
		// Everything is referenced for the root subsystem during initialization.
		if (isRoot() && EnumSet.of(State.INSTALLING, State.INSTALLED).contains(getState()))
			return true;
		DeployedContentHeader header = getDeploymentManifest().getDeployedContentHeader();
		if (header == null)
			return false;
		return header.isReferenced(resource);
	}
	
	boolean isRoot() {
		return ROOT_LOCATION.equals(getLocation());
	}
	
	boolean isScoped() {
		return isApplication() || isComposite();
	}
	
	void removedContent(Resource resource) {
		DeploymentManifest manifest = getDeploymentManifest();
		DeployedContentHeader header = manifest.getDeployedContentHeader();
		if (header == null)
			return;
		DeployedContentHeader.Clause clause = header.getClause(resource);
		if (clause == null)
			return;
		removedContent(Collections.singleton(clause));
	}
	
	synchronized void removedContent(Collection<DeployedContentHeader.Clause> content) {
		DeploymentManifest manifest = getDeploymentManifest();
		DeployedContentHeader header = manifest.getDeployedContentHeader();
		if (header == null)
			return;
		Collection<DeployedContentHeader.Clause> clauses = new ArrayList<DeployedContentHeader.Clause>(header.getClauses());
		for (Iterator<DeployedContentHeader.Clause> i = clauses.iterator(); i.hasNext();)
			if (content.contains(i.next())) {
				i.remove();
				break;
			}
		DeploymentManifest.Builder builder = new DeploymentManifest.Builder();
		for (Entry<String, Header<?>> entry : manifest.getHeaders().entrySet()) {
			if (DeployedContentHeader.NAME.equals(entry.getKey()))
				continue;
			builder.header(entry.getValue());
		}
		if (!clauses.isEmpty())
			builder.header(new DeployedContentHeader(clauses));
		try {
			setDeploymentManifest(builder.build());
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	void setAutostart(boolean value) {
		try {
			synchronized (this) {
				setDeploymentManifest(new DeploymentManifest.Builder()
						.manifest(getDeploymentManifest()).autostart(value).build());
			}
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	synchronized void setDeploymentManifest(DeploymentManifest value) throws IOException {
		deploymentManifest = value;
		Coordination coordination = Activator.getInstance().getCoordinator().peek();
		if (logger.isDebugEnabled())
			logger.debug("Setting deployment manifest for subsystem {} using coordination {}", getSymbolicName(), coordination == null ? null : coordination.getName());
		if (coordination == null) {
			saveDeploymentManifest();
		} else {
			Map<Class<?>, Object> variables = coordination.getVariables();
			synchronized (variables) {
				@SuppressWarnings("unchecked")
				Set<BasicSubsystem> dirtySubsystems = (Set<BasicSubsystem>) variables.get(SaveManifestParticipant.class);
				if (dirtySubsystems == null) {
					// currently no dirty subsystems found;
					// create a list to hold them and store it as a variable
					dirtySubsystems = new HashSet<BasicSubsystem>();
					variables.put(SaveManifestParticipant.class, dirtySubsystems);
					// add the save manifest participant
					coordination.addParticipant(new SaveManifestParticipant());
				}
				dirtySubsystems.add(this);
			}
		}
	}

	synchronized void saveDeploymentManifest() throws IOException {
		File file = new File(getDirectory(), "OSGI-INF");
		if (!file.exists())
			file.mkdirs();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(file, "DEPLOYMENT.MF")));
		try {
			if (logger.isDebugEnabled())
				logger.debug("Writing deployment manifest for subsystem {} in state {}", getSymbolicName(), getState());
			deploymentManifest.write(out);
			if (logger.isDebugEnabled())
				logger.debug("Wrote deployment manifest for subsystem {} in state {}", getSymbolicName(), getState());
		}
		finally {
			IOUtils.close(out);
		}
	}
	
	void setState(State value) {
		if (logger.isDebugEnabled())
			logger.debug("Setting state of subsystem {} to {}", getSymbolicName(), value);
		State state = getState();
		if (value.equals(state)) {
			if (logger.isDebugEnabled())
				logger.debug("Requested state {} equals current state {}", value, state);
			return;
		}
		try {
			if (logger.isDebugEnabled())
				logger.debug("Setting the deployment manifest...");
			synchronized (this) {
				setDeploymentManifest(new DeploymentManifest.Builder()
						.manifest(getDeploymentManifest()).state(value).build());
			}
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
		Activator.getInstance().getSubsystemServiceRegistrar().update(this);
		synchronized (this) {
			if (logger.isDebugEnabled())
				logger.debug("Notifying all waiting for state change of subsystem {}", getSymbolicName());
			notifyAll();
		}
	}
	
	void setRegionContextBundle(Bundle value) {
	    regionContextBundle = value; // volatile variable
	}
	
	synchronized void setSubsystemManifest(SubsystemManifest value) throws URISyntaxException, IOException {
		File file = new File(getDirectory(), "OSGI-INF");
		if (!file.exists())
			file.mkdirs();
		FileOutputStream fos = new FileOutputStream(new File(file, "SUBSYSTEM.MF"));
		try {
			value.write(fos);
			subsystemManifest = value;
		}
		finally {
			IOUtils.close(fos);
		}
	}
	
	private String getDeploymentManifestHeaderValue(String name) {
		DeploymentManifest manifest = getDeploymentManifest();
		if (manifest == null)
			return null;
		Header<?> header = manifest.getHeaders().get(name);
		if (header == null)
			return null;
		return header.getValue();
	}

	@Override
	public synchronized void addRequirements(Collection<Requirement> requirements) {
		// The root subsystem has no requirements (there is no parent to import from).
		if (isRoot())
			throw new UnsupportedOperationException("The root subsystem does not accept additional requirements");
		// Unscoped subsystems import everything already.
		if (!isScoped())
			return;
		RegionUpdater updater = new RegionUpdater(getRegion(), ((BasicSubsystem)getParents().iterator().next()).getRegion());
		try {
			updater.addRequirements(requirements);
		}
		catch (Exception e) {
			throw new SubsystemException(e);
		}
	}

	@Override
	public AriesSubsystem install(String location, IDirectory content) {
		return install(location, content, null);
	}
	
	@Override
	public AriesSubsystem install(String location, IDirectory content, InputStream deploymentManifest) {
		try {
			return AccessController.doPrivileged(new InstallAction(location, content, this, AccessController.getContext(), deploymentManifest));
		}
		finally {
			IOUtils.close(deploymentManifest);
		}
	}

	private static class SaveManifestParticipant implements Participant {
		protected SaveManifestParticipant() {}

		@Override
		public void ended(Coordination coordination) throws Exception {
			if (logger.isDebugEnabled())
				logger.debug("Saving deployment manifests because coordination {} ended", coordination.getName());
			Map<Class<?>, Object> variables = coordination.getVariables();
			Set<BasicSubsystem> dirtySubsystems;
			synchronized (variables) {
				@SuppressWarnings("unchecked")
				Set<BasicSubsystem> temp = (Set<BasicSubsystem>) variables.remove(SaveManifestParticipant.class);
				dirtySubsystems = temp == null ? Collections. <BasicSubsystem>emptySet() : temp;
			}
			for (BasicSubsystem dirtySubsystem : dirtySubsystems) {
				if (logger.isDebugEnabled())
					logger.debug("Saving deployment manifest of subsystem {} for coordination {}", dirtySubsystem.getSymbolicName(), coordination.getName());
				dirtySubsystem.saveDeploymentManifest();
			}
		}

		@Override
		public void failed(Coordination coordination) throws Exception {
			// Do no saving
		}
		
	}

	@Override
	public Map<String, String> getDeploymentHeaders() {
		SecurityManager.checkMetadataPermission(this);
		return AccessController.doPrivileged(new GetDeploymentHeadersAction(this));
	}

	@Override
	public AriesSubsystem install(String location, final InputStream content, InputStream deploymentManifest) {
		try {
			return install(location, content == null ? null : 
				AccessController.doPrivileged(new PrivilegedAction<IDirectory>() {
					@Override
					public IDirectory run() {
						return FileSystem.getFSRoot(content);
					}
				}),
				deploymentManifest);
		}
		finally {
			// This method must guarantee the content input stream was closed.
			IOUtils.close(content);
		}
	}
	
	private void setTranslations() throws IOException {
		String directoryName = getSubsystemManifest().getSubsystemLocalizationHeader().getDirectoryName();
		File file = directoryName == null ? getDirectory() : new File(getDirectory(), directoryName);
		if (!file.exists())
			file.mkdirs();
		for (TranslationFile translation : getResource().getTranslations()) {
			translation.write(file);
		}
	}
}

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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.aries.subsystem.core.archive.Archive;
import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeployedContentHeader.DeployedContent;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader.ProvisionedResource;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.VersionHeader;
import org.apache.aries.subsystem.core.obr.SubsystemEnvironment;
import org.apache.aries.subsystem.core.resource.BundleRuntimeResource;
import org.apache.aries.subsystem.core.resource.RuntimeResource;
import org.apache.aries.subsystem.core.resource.RuntimeResourceFactoryImpl;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class AriesSubsystem implements Subsystem {
	public static AriesSubsystem newInstance(String location, InputStream content, AriesSubsystem parent) throws Exception {
		AriesSubsystem subsystem = new AriesSubsystem(location, content, parent);
		Archive archive = subsystem.getArchive();
		subsystem.environment = new SubsystemEnvironment(subsystem);
		if (archive.getSubsystemManifest() == null) {
			SubsystemUri uri = new SubsystemUri(location);
			archive.setSubsystemManifest(SubsystemManifest.newInstance(uri.getSymbolicName(), uri.getVersion(), archive.getResources()));
		}
		if (archive.getDeploymentManifest() == null) {
			archive.setDeploymentManifest(DeploymentManifest.newInstance(archive.getSubsystemManifest(), subsystem.environment));
		}
		return subsystem;
	}
	
	private static final String ROOT_LOCATION = "root";
	private static final String ROOT_SYMBOLIC_NAME = "org.apache.aries.subsystem.root";
	private static final Version ROOT_VERSION = Version.parseVersion("1.0.0");
	
	private static long lastId;
	
	private static void deleteFile(File file) {
		if (file.isDirectory()) {
			deleteFiles(file.listFiles());
		}
		file.delete();
	}
	
	private static void deleteFiles(File[] files) {
		for (File file : files) {
			deleteFile(file);
		}
	}
	
	private synchronized static long getNextId() {
		if (Long.MAX_VALUE == lastId)
			throw new IllegalStateException("The next subsystem ID would exceed Long.MAX_VALUE: " + lastId);
		return ++lastId;
	}
	
	private static void postEvent(Subsystem subsystem, SubsystemConstants.EventType type) {
		postEvent(subsystem, type, null);
	}
	
	private static void postEvent(Subsystem subsystem, SubsystemConstants.EventType type, Throwable t) {
		EventAdmin eventAdmin = Activator.getEventAdmin();
		if (eventAdmin != null) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(SubsystemConstants.SUBSYSTEM_ID, subsystem.getSubsystemId());
			map.put(SubsystemConstants.SUBSYSTEM_LOCATION, subsystem.getLocation());
			map.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, subsystem.getSymbolicName());
			map.put(SubsystemConstants.SUBSYSTEM_VERSION, String.valueOf(subsystem.getVersion()));
			// TODO This needs to be defined as a constant.
			map.put("subsystem.state", String.valueOf(subsystem.getState()));
			map.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
			if (t != null) {
				map.put(EventConstants.EXCEPTION, t);
				map.put(EventConstants.EXCEPTION_CLASS, t.getClass().getName());
				map.put(EventConstants.EXCEPTION_MESSAGE, t.getMessage());
			}
			// TODO This needs to be defined as a constant.
			Event event = new Event("org/osgi/service/Subsystem/" + type, map);
			eventAdmin.postEvent(event);
		}
	}
	
	private final Archive archive;
	private final Set<Resource> constituents = Collections.synchronizedSet(new HashSet<Resource>());
	private final Set<AriesSubsystem> children = new HashSet<AriesSubsystem>();
	private volatile SubsystemEnvironment environment;
	private final long id;
	private final String location;
	private final AriesSubsystem parent;
	private final Region region;
	
	private Subsystem.State state;
	
	public AriesSubsystem() throws Exception {
		archive = null;
		id = 0;
		location = ROOT_LOCATION;
		parent = null;
		region = createRegion();
		state = State.ACTIVE;
		// TODO Haven't thought this through yet. Issues?
		environment = null;
	}
	
	private AriesSubsystem(String location, InputStream content, AriesSubsystem parent) throws Exception {
		this.location = location;
		this.parent = parent;
		if (content == null) {
			content = new URL(location).openStream();
		}
		File rootDirectory = Activator.getBundleContext().getDataFile("");
		id = getNextId();
		File subsystemDirectory = new File(rootDirectory, "subsystem" + id + System.getProperty("file.separator"));
		// TODO Leaking reference. Issues?
//		environment = new SubsystemEnvironment(this);
//		archive = new Archive(location, subsystemDirectory, content, environment);
		archive = new Archive(location, subsystemDirectory, content);
		if (isFeature()) {
			// Features do not have regions.
			region = null;
		}
		else {
			// Applications and composites have regions.
			region = createRegion();
		}
		
	}
	
	@Override
	public void cancel() throws SubsystemException {
		// TODO Auto-generated method stub
	}
	
	public Archive getArchive() {
		return archive;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<Subsystem> getChildren() {
		return (Collection<Subsystem>)(Collection)Collections.unmodifiableSet(children);
	}

	@Override
	public Collection<Resource> getConstituents() {
		return Collections.unmodifiableSet(constituents);
	}
	
	public SubsystemEnvironment getEnvironment() {
		return environment;
	}

	@Override
	public Map<String, String> getHeaders() {
		return getHeaders(null);
	}

	@Override
	public Map<String, String> getHeaders(String locale) {
		// Archive will be null for the root subsystem.
		if (archive == null)
			return Collections.emptyMap();
		ResourceBundle rb = null;
		if (locale != null && locale.length() != 0) {
			try {
				rb = ResourceBundle.getBundle(locale);
			}
			catch (MissingResourceException e) {}
		}
		Collection<Header> headers = archive.getSubsystemManifest().getHeaders();
		Map<String, String> result = new HashMap<String, String>(headers.size());
		for (Header header : headers) {
			String value = header.getValue();
			if (value.startsWith("%")) {
				value = value.substring(1);
				String translation = null;
				try {
					translation = rb.getString(value);
				}
				catch (MissingResourceException e) {}
				if (translation != null)
					value = translation;
			}
			result.put(header.getName(), value);
		}
		return result;
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public AriesSubsystem getParent() {
		return parent;
	}
	
	public Region getRegion() {
		return region;
	}

	@Override
	public Subsystem.State getState() {
		return state;
	}

	@Override
	public long getSubsystemId() {
		return id;
	}

	@Override
	public String getSymbolicName() {
		if (archive == null) {
			return ROOT_SYMBOLIC_NAME;
		}
		return ((SubsystemSymbolicNameHeader)archive.getSubsystemManifest().getSubsystemSymbolicName()).getSymbolicName();
	}

	@Override
	public Version getVersion() {
		if (archive == null) {
			return ROOT_VERSION;
		}
		return ((VersionHeader)archive.getSubsystemManifest().getSubsystemVersion()).getVersion();
	}

	@Override
	public Subsystem install(String location) throws SubsystemException {
		return install(location, null);
	}
	
	@Override
	public Subsystem install(String location, InputStream content) throws SubsystemException {
		final AriesSubsystem subsystem;
		try {
			subsystem = Activator.getSubsystemManager().newSubsystem(location, content, this);
		}
		catch (Exception e) {
			throw new SubsystemException(e);
		}
		children.add(subsystem);
		subsystem.setState(Subsystem.State.INSTALLING);
		postEvent(subsystem, SubsystemConstants.EventType.INSTALLING);
		Activator.getExecutor().execute(new Runnable() {
			public void run() {
				try {
					subsystem.installAsync();
				}
				catch (Exception e) {
					postEvent(subsystem, SubsystemConstants.EventType.FAILED, e);
					subsystem.setState(Subsystem.State.UNINSTALLED);
					postEvent(subsystem, SubsystemConstants.EventType.UNINSTALLED);
				}
			}
		});
		return subsystem;
	}
	
	public boolean isApplication() {
		// TODO Add to constants.
		return "osgi.application".equals(archive.getSubsystemManifest().getSubsystemType().getValue());
	}
	
	public boolean isComposite() {
		// TODO Add to constants.
		return "osgi.composite".equals(archive.getSubsystemManifest().getSubsystemType().getValue());
	}
	
	public boolean isFeature() {
		// TODO Add to constants.
		return "osgi.feature".equals(archive.getSubsystemManifest().getSubsystemType().getValue());
	}
	
	/* INSTALLING	Wait, Start
	 * INSTALLED	-
	 * RESOLVING	Wait, Start
	 * RESOLVED		-
	 * STARTING		Noop
	 * ACTIVE		Noop
	 * STOPPING		Wait, Start
	 * UPDATING		Wait, Start
	 * UNINSTALLING	Error
	 * UNINSTALLED	Error
	 */
	@Override
	public synchronized void start() throws SubsystemException {
		checkRoot();
		State state = getState();
		if (state == State.UNINSTALLING || state == State.UNINSTALLED) {
			throw new SubsystemException("Cannot stop from state " + state);
		}
		if (state == State.INSTALLING || state == State.RESOLVING || state == State.STOPPING || state == State.UPDATING) {
			waitForStateChange();
			start();
			return;
		}
		if (state == State.STARTING || state == State.ACTIVE) {
			return;
		}
		if (state == State.INSTALLED) {
			setState(State.RESOLVING);
			postEvent(this, SubsystemConstants.EventType.RESOLVING);
		}
		else {
			setState(State.STARTING);
			postEvent(this, SubsystemConstants.EventType.STARTING);
		}
		Activator.getExecutor().execute(new Runnable() {
			public void run() {
				startAsync();
			}
		});
	}
	
	/* INSTALLING	Noop
	 * INSTALLED	Noop
	 * RESOLVING	Noop
	 * RESOLVED		Noop
	 * STARTING		Wait, Stop
	 * ACTIVE		-
	 * STOPPING		Noop
	 * UPDATING		Noop
	 * UNINSTALLING	Error
	 * UNINSTALLED	Error
	 */
	@Override
	public synchronized void stop() throws SubsystemException {
		checkRoot();
		if (getState() == State.UNINSTALLING || getState() == State.UNINSTALLED) {
			throw new SubsystemException("Cannot stop from state " + state);
		}
		else if (getState() == State.STARTING) {
			waitForStateChange();
			stop();
		}
		else if (getState() != State.ACTIVE) {
			return;
		}
		setState(State.STOPPING);
		postEvent(this, SubsystemConstants.EventType.STOPPING);
		// TODO Need to store the task for cancellation.
		Activator.getExecutor().execute(new Runnable() {
			public void run() {
				stopAsync();
			}
		});
	}
	
	/* INSTALLING	Wait, Uninstall
	 * INSTALLED	-
	 * RESOLVING	Wait, Uninstall
	 * RESOLVED		-
	 * STARTING		Wait, Uninstall
	 * ACTIVE		Stop, Uninstall
	 * STOPPING		Wait, Uninstall
	 * UPDATING		Wait, Uninstall
	 * UNINSTALLING	Noop
	 * UNINSTALLED	Noop
	 */
	@Override
	public synchronized void uninstall() throws SubsystemException {
		checkRoot();
		State state = getState();
		if (state == State.UNINSTALLING || state == State.UNINSTALLED) {
			return;
		}
		else if (state == State.INSTALLING || state == State.RESOLVING || state == State.STARTING || state == State.STOPPING || state == State.UPDATING) {
			waitForStateChange();
			uninstall();
		}
		else if (getState() == State.ACTIVE) {
			stop();
			uninstall();
		}
		setState(State.UNINSTALLING);
		postEvent(this, SubsystemConstants.EventType.UNINSTALLING);
		Activator.getExecutor().execute(new Runnable() {
			public void run() {
				uninstallAsync();
			}
		});
	}
	
	void bundleChanged(BundleEvent event) {
		switch (event.getType()) {
			case BundleEvent.STARTING:
				if (State.STARTING.equals(getState())) {
					return;
				}
				start();
				break;
			case BundleEvent.STOPPING:
				if (State.STOPPING.equals(getState())) {
					return;
				}
				stop();
				break;
			case BundleEvent.UNINSTALLED:
				if (EnumSet.of(State.UNINSTALLING, State.UNINSTALLED).contains(getState())) {
					return;
				}
				uninstall();
				break;
		}
	}
	
	protected void addConstituent(Resource resource) {
		constituents.add(resource);
	}
	
	protected boolean contains(Resource resource) {
		return constituents.contains(resource);
	}
	
//	protected Resource findContentResource(String namespace, String symbolicName, String version) {
//		Requirement requirement = new OsgiIdentityRequirement(null, symbolicName, Version.parseVersion(version), namespace);
//		Resource result = null;
//		/* Root */
//		if (isRoot()) {
//			// For the root subsystem, simply look for a resource in the system repository.
//			result = ResourceHelper.getResource(requirement, repository);
//		}
//		/* Features */
//		else if (isFeature()) {
//			// Check for existing, accessible resources first.
//			result = parent.findContentResource(namespace, symbolicName, version);
//			// If necessary, look for the resource in the subsystem repository.
//			if (result == null) {
//				if (repository != null) {
//					// Repository will be null if the subsystem contained no resources.
//					result = ResourceHelper.getResource(requirement, repository);
//				}
//				// Finally, if necessary, look for the resource in other repositories.
//				if (result == null) {
//					result = Activator.getResourceResolver().find(symbolicName + ";version=" + version);
//				}
//			}
//			
//		}
//		/* Applications */
//		else if (isApplication()) {
//			// For applications, we never want to reuse an existing resource.
//			// Favor resources included with the subsystem definition.
//			if (repository != null) {
//				// Repository will be null if the subsystem contained no resources.
//				result = ResourceHelper.getResource(requirement, repository);
//			}
//			// If necessary, look for the resource in other repositories.
//			if (result == null) {
//				result = Activator.getResourceResolver().find(symbolicName + ";version=" + version);
//			}
//		}
//		/* Composites */
//		else {
//			// TODO Implement composite behavior.
//		}
//		return result;
//	}
	
	protected Collection<Bundle> getBundles() {
		ArrayList<Bundle> result = new ArrayList<Bundle>(constituents.size());
		for (Resource resource : constituents) {
			if (resource instanceof BundleRuntimeResource)
				result.add(((BundleRuntimeResource)resource).getBundle());
		}
		result.trimToSize();
		return result;
	}
	
	protected synchronized void setState(Subsystem.State state) {
		this.state = state;
		notifyAll();
	}
	
	protected synchronized void waitForStateChange() {
		try {
			wait();
		}
		catch (InterruptedException e) {
			throw new SubsystemException(e);
		}
	}
	
	private void checkRoot() {
		if (isRoot()) {
			throw new SubsystemException("This operation may not be performed on the root subsystem");
		}
	}
	
	private Region createRegion() throws BundleException {
		return Activator.getRegionDigraph().createRegion(getSymbolicName() + ';' + getVersion());
	}
	
	private void install(Resource resource, Coordination coordination, boolean transitive) {
//		String content = ResourceHelper.getContentAttribute(resource);
//		String location = getSubsystemId() + '@' + getSymbolicName() + '@' + content;
		try {
			final RuntimeResource runtimeResource;
//			final Bundle bundle;
			if (transitive) {
				// Transitive dependencies should be provisioned into the highest possible level.
				// Transitive dependencies do not become a constituent.
				// TODO Assumes root is always the appropriate level.
				AriesSubsystem subsystem = this;
				while (subsystem.parent != null)
					subsystem = subsystem.parent;
				runtimeResource = new RuntimeResourceFactoryImpl().create(resource, null, subsystem);
				runtimeResource.install(coordination);
				subsystem.constituents.add(runtimeResource);
//				bundle = subsystem.region.installBundle(location, new URL(content).openStream());
			}
			else if (region == null) {
				// Feature resources should be provisioned into the parent, unless the parent is also a feature.
				// TODO Assumes parent is always highest possible level.
//				AriesSubsystem subsystem = this.parent;
//				while (subsystem.region == null)
//					subsystem = subsystem.parent;
				runtimeResource = new RuntimeResourceFactoryImpl().create(resource, null, this);
				runtimeResource.install(coordination);
//				bundle = subsystem.region.installBundle(location, new URL(content).openStream());
				// Features retain their constituents.
				constituents.add(runtimeResource);
			}
			else {
				// Constituent (non-transitive) resources must be provisioned into the owning subsystem, except for features.
				// We know this isn't a feature because the region was not null.
				runtimeResource = new RuntimeResourceFactoryImpl().create(resource, null, this);
				runtimeResource.install(coordination);
//				bundle = region.installBundle(location, new URL(content).openStream());
				constituents.add(runtimeResource);
			}
//			coordination.addParticipant(new Participant() {
//				public void ended(Coordination coordination) throws Exception {
//					// noop
//				}
//	
//				public void failed(Coordination coordination) throws Exception {
//					constituents.remove(bundle.adapt(BundleRevision.class));
//					region.removeBundle(bundle);
//				}
//			});
		}
		catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	private synchronized void installAsync() throws Exception {
		List<Resource> contentResources = new ArrayList<Resource>();
		List<Resource> transitiveDependencies = new ArrayList<Resource>();
//		// Get the resources included within the archive.
//		contentResources.addAll(archive.getResources());
//		// Create a subsystem repository from the contents of the subsystem archive.
//		if (!contentResources.isEmpty()) {
//			Document document = RepositoryDescriptorGenerator.generateRepositoryDescriptor("subsystem" + getSubsystemId(), contentResources);
//			File file = new File(archive.getDirectory() + "/repository.xml");
//			FileOutputStream fout = new FileOutputStream(file);
//		    TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(fout));
//		    fout.close();
//		    repository = new RepositoryFactory().newRepository(file.toURI().toURL());
//		}
//		contentResources.clear();
		DeploymentManifest manifest = archive.getDeploymentManifest();
		DeployedContentHeader contentHeader = manifest.getDeployedContent();
		for (DeployedContent content : contentHeader.getDeployedContents()) {
			Collection<Capability> capabilities = environment.findProviders(
					new OsgiIdentityRequirement(null, content.getName(), content.getDeployedVersion(), content.getNamespace()));
			if (capabilities.isEmpty())
				throw new SubsystemException("Subsystem content resource does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
			Resource resource = capabilities.iterator().next().getResource();
			contentResources.add(resource);
			
//			// Find the most appropriate resource based on subsystem type.
//			Resource resource = findContentResource(content.getNamespace(), content.getName(), String.valueOf(content.getDeployedVersion()));
//			if (resource == null)
//				throw new SubsystemException("Subsystem content resource does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
//			contentResources.add(resource);
		}
		ProvisionResourceHeader resourceHeader = manifest.getProvisionResource();
		if (resourceHeader != null) {
			for (ProvisionedResource content : resourceHeader.getProvisionedResources()) {
				Collection<Capability> capabilities = environment.findProviders(
						new OsgiIdentityRequirement(null, content.getName(), content.getDeployedVersion(), content.getNamespace()));
				if (capabilities.isEmpty())
					throw new SubsystemException("Subsystem content resource does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
				Resource resource = capabilities.iterator().next().getResource();
				transitiveDependencies.add(resource);
				
//				// Find the most appropriate resource based on subsystem type.
//				Resource resource = findContentResource(content.getNamespace(), content.getName(), String.valueOf(content.getDeployedVersion()));
//				if (resource == null)
//					throw new SubsystemException("Subsystem transitive dependency does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
//				transitiveDependencies.add(resource);
			}
		}
		// Install content resources and transitive dependencies.
		if (!contentResources.isEmpty()) {
			Coordination coordination = Activator.getCoordinator().create(getSymbolicName() + '-' + getSubsystemId(), 0);
			try {
				// Install the content resources.
				for (Resource resource : contentResources) {
					install(resource, coordination, false);
				}
				// Discover and install transitive dependencies.
				for (Resource resource : transitiveDependencies) {
					install(resource, coordination, true);
				}
			}
			catch (Exception e) {
				coordination.fail(e);
			}
			finally {
				coordination.end();
			}
		}
		setState(Subsystem.State.INSTALLED);
		postEvent(this, SubsystemConstants.EventType.INSTALLED);
	}
	
	private boolean isRoot() {
		return ROOT_LOCATION.equals(getLocation());
	}
	
	private void setExportIsolationPolicy() {
		// Archive is null for root subsystem.
		if (archive == null)
			return;
		// TODO Implement export isolation policy for composites.
	}
	
	private void setImportIsolationPolicy() throws BundleException {
		// Archive is null for root subsystem.
		if (archive == null)
			return;
		// Feature contents are stored in the parent (or higher) region and take on the associated isolation.
		if (isFeature()) {
			return;
		}
		if (isApplication()) {
			region.connectRegion(
					parent.getRegion(), 
					region.getRegionDigraph().createRegionFilterBuilder().allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE).build());
		}
		// TODO Implement import isolation policy for composites.
	}
	
//	private void start(Resource resource, Coordination coordination) throws BundleException {
//		if (!(resource instanceof BundleRevision)) {
//			throw new SubsystemException("Unsupported resource type: " + resource);
//		}
//		final Bundle bundle = ((BundleRevision)resource).getBundle();
//		// We don't want the bundles to autostart. Starting bundles must be under the control of the
//		// subsystem in order to guarantee ordering.
//		bundle.start(Bundle.START_TRANSIENT);
//		coordination.addParticipant(new Participant() {
//			public void ended(Coordination coordination) throws Exception {
//				// noop
//			}
//
//			public void failed(Coordination coordination) throws Exception {
//				bundle.stop();
//			}
//		});
//	}
	
	private void startAsync() {
		// TODO Need to hold a lock here to guarantee that another start operation can't occur when the state goes to RESOLVED.
		// Resolve the subsystem, if necessary.
		if (getState() == State.RESOLVING) { // Otherwise, the state will be STARTING.
			try {
				setImportIsolationPolicy();
				if (!Activator.getFrameworkWiring().resolveBundles(getBundles())) {
					throw new Exception("Framework could not resolve the bundles");
				}
				setExportIsolationPolicy();
				// TODO Could avoid calling setState (and notifyAll) here and avoid the need for a lock.
				setState(State.RESOLVED);
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.RESOLVED);
				setState(State.STARTING);
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.STARTING);
			}
			catch (Exception e) {
				setState(State.INSTALLED);
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
				return;
			}
		}
		// Start the subsystem.
		Coordination coordination = Activator.getCoordinator().create(getSymbolicName() + '-' + getSubsystemId(), 0);
		try {
			for (Resource resource : constituents) {
				((RuntimeResource)resource).start(coordination);
			}
		}
		catch (Exception e) {
			coordination.fail(e);
			// TODO Need to reinstate complete isolation by disconnecting the region and transition to INSTALLED.
		}
		finally {
			try {
				coordination.end();
			}
			catch (CoordinationException e) {
				setState(State.RESOLVED);
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
			}
		}
//		try {
//			for (Resource resource : constituents) {
//				start(resource, coordination);
//			}
//		}
//		catch (Exception e) {
//			coordination.fail(e);
//			// TODO Need to reinstate complete isolation by disconnecting the region and transition to INSTALLED.
//		}
//		finally {
//			try {
//				coordination.end();
//			}
//			catch (CoordinationException e) {
//				setState(State.RESOLVED);
//				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
//			}
//		}
		setState(State.ACTIVE);
		postEvent(AriesSubsystem.this, SubsystemConstants.EventType.STARTED);
	}
	
	private void stop(Resource resource) throws Exception {
		((RuntimeResource)resource).stop(null);
//		if (!(resource instanceof BundleRevision)) {
//			throw new SubsystemException("Unsupported resource type: " + resource);
//		}
//		final Bundle bundle = ((BundleRevision)resource).getBundle();
//		bundle.stop();
	}
	
	private void stopAsync() {
		// TODO Persist stop state.
		for (Resource resource : constituents) {
			try {
				stop(resource);
			}
			catch (Exception e) {
				// TODO Should FAILED go out for each failure?
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
			}
		}
		// TODO Can we automatically assume it actually is resolved?
		setState(State.RESOLVED);
		postEvent(AriesSubsystem.this, SubsystemConstants.EventType.STOPPED);
	}
	
	private void uninstall(Resource resource) throws Exception {
		((RuntimeResource)resource).uninstall(null);
//		if (!(resource instanceof BundleRevision)) {
//			throw new SubsystemException("Unsupported resource type: " + resource);
//		}
//		final Bundle bundle = ((BundleRevision)resource).getBundle();
//		bundle.uninstall();
	}
	
	private void uninstallAsync() {
		for (Resource resource : constituents) {
			try {
				uninstall(resource);
			}
			catch (Exception e) {
				// TODO Should FAILED go out for each failure?
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
			}
		}
		parent.children.remove(this);
		deleteFile(Activator.getBundleContext().getDataFile("subsystem" + id + System.getProperty("file.separator")));
		setState(State.UNINSTALLED);
		postEvent(AriesSubsystem.this, SubsystemConstants.EventType.UNINSTALLED);
	}
}

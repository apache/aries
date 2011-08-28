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
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.aries.subsystem.core.ResourceHelper;
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
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.ResourceConstants;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AriesSubsystem implements Subsystem, Resource {
	private static final Logger LOGGER = LoggerFactory.getLogger(AriesSubsystem.class);
	private static final String ROOT_LOCATION = "root";
	private static final String ROOT_SYMBOLIC_NAME = "org.apache.aries.subsystem.root";
	private static final Version ROOT_VERSION = Version.parseVersion("1.0.0");
	
	private static final Map<String, AriesSubsystem> locationToSubsystem = new HashMap<String, AriesSubsystem>();
	private static final Map<Resource, Set<Subsystem>> resourceToSubsystems = new HashMap<Resource, Set<Subsystem>>();
	
	static synchronized Collection<AriesSubsystem> getSubsystems(Resource resource) {
		ArrayList<AriesSubsystem> result = new ArrayList<AriesSubsystem>(locationToSubsystem.size());
		for (AriesSubsystem subsystem : locationToSubsystem.values()) {
			if (subsystem.contains(resource)) {
				result.add(subsystem);
			}
		}
		result.trimToSize();
		return result;
	}
	
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
			try {
				map.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, subsystem.getSymbolicName());
			}
			catch (IllegalStateException e) {}
			try {
				map.put(SubsystemConstants.SUBSYSTEM_VERSION, String.valueOf(subsystem.getVersion()));
			}
			catch (IllegalStateException e) {}
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
	
	private final Set<Resource> constituents = Collections.synchronizedSet(new HashSet<Resource>());
	private final Set<AriesSubsystem> children = Collections.synchronizedSet(new HashSet<AriesSubsystem>());
	private volatile SubsystemEnvironment environment;
	private final long id;
	private final String location;
	private final AriesSubsystem parent;
	
	private Archive archive;
	private Region region;
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
	
	private AriesSubsystem(String location, /*InputStream content,*/ AriesSubsystem parent) /*throws Exception*/ {
		this.location = location;
		this.parent = parent;
		id = getNextId();
	}
	
	@Override
	public void cancel() throws SubsystemException {
		// TODO Auto-generated method stub
	}
	
	public synchronized Archive getArchive() {
		return archive;
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		Capability capability = new OsgiIdentityCapability(this, getSymbolicName(), getVersion(), "osgi.subsystem");
		return Arrays.asList(new Capability[]{capability});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<Subsystem> getChildren() {
		return (Collection<Subsystem>)(Collection)Collections.unmodifiableSet(children);
	}

	@Override
	public Collection<Resource> getConstituents() {
		/* 
		 * TODO The definition of constituents needs to be worked out from both a design and implementation perspective.
		 * (1) In general, any Resource type except for osgi.subsystem.
		 * (2) A transitive dependency if this is the root subsystem or provision-policy:=acceptTransitive.
		 * (3) A content Resource of this subsystem if the Resource is not also a subsystem (also, doesn't apply to 
		 *     the root subsystem since it has no content resources).
		 * (4) A bundle Resource installed into a region by means other than the subsystem API (primarily for the 
		 *     root subsystem which shares the kernel region but could apply to any subsystem).
		 * (5) Caution with 4: this does not include feature content resources installed into a non-feature subsystem region. 
		 *     These are constituents of the feature itself.
		 */
		if (isRoot()) {
			// TODO This does not take into account the possibility that resource types other than bundle became part of the root subsystem.
			Set<Long> bundleIds = region.getBundleIds();
			Collection<Resource> resources = new ArrayList<Resource>(bundleIds.size());
			for (Long bundleId : bundleIds)
				resources.add(Activator.getBundleContext().getBundle(bundleId).adapt(BundleRevision.class));
			return resources;
		}
		return Collections.unmodifiableCollection(constituents);
	}
	
	public DeploymentManifest getDeploymentManifest() throws IOException {
		if (archive.getDeploymentManifest() == null) {
			archive.setDeploymentManifest(DeploymentManifest.newInstance(archive.getSubsystemManifest(), environment));
		}
		return archive.getDeploymentManifest();
	}
	
	public synchronized SubsystemEnvironment getEnvironment() {
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
			if (rb != null && value.startsWith("%")) {
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

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
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
			// If the archive is null, this is either the root subsystem or an installing subsystem not yet initialized.
			if (State.INSTALLING.equals(getState()))
				// The root subsystem's state will never be INSTALLING, so this is an uninitialized subsystem.
				throw new IllegalStateException();
			// This is the root subsystem.
			return ROOT_SYMBOLIC_NAME;
		}
		return ((SubsystemSymbolicNameHeader)archive.getSubsystemManifest().getSubsystemSymbolicName()).getSymbolicName();
	}

	@Override
	public Version getVersion() {
		if (archive == null) {
			// If the archive is null, this is either the root subsystem or an installing subsystem not yet initialized.
			if (State.INSTALLING.equals(getState()))
				// The root subsystem's state will never be INSTALLING, so this is an uninitialized subsystem.
				throw new IllegalStateException();
			// This is the root subsystem.
			return ROOT_VERSION;
		}
		return ((VersionHeader)archive.getSubsystemManifest().getSubsystemVersion()).getVersion();
	}

	@Override
	public Subsystem install(String location) throws SubsystemException {
		return install(location, null);
	}
	
	@Override
	public synchronized Subsystem install(String location, final InputStream content) throws SubsystemException {
		final AriesSubsystem subsystem;
		if (locationToSubsystem.containsKey(location))
			return locationToSubsystem.get(location);
		subsystem = new AriesSubsystem(location, this);
		locationToSubsystem.put(location, subsystem);
		subsystem.setState(Subsystem.State.INSTALLING);
		postEvent(subsystem, SubsystemConstants.EventType.INSTALLING);
		children.add(subsystem);
		constituents.add(subsystem);
		Activator.getExecutor().execute(new Runnable() {
			public void run() {
				try {
					subsystem.initialize(content);
					subsystem.install();
					subsystem.setState(Subsystem.State.INSTALLED);
					postEvent(subsystem, SubsystemConstants.EventType.INSTALLED);
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
	
	public void removeConstituent(Resource resource) {
		constituents.remove(resource);
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
				// TODO Need to hold a lock here to guarantee that another start operation can't occur when the state goes to RESOLVED.
				// Resolve the subsystem, if necessary.
				if (getState() == State.RESOLVING) { // Otherwise, the state will be STARTING.
					try {
						setImportIsolationPolicy();
						// TODO I think this is insufficient. Do we need both pre-install and post-install environments for the Resolver?
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
					// TODO Need to make sure the consitutents are ordered by start level.
					for (Resource resource : constituents) {
						startResource(resource, coordination);
					}
					setState(State.ACTIVE);
					postEvent(AriesSubsystem.this, SubsystemConstants.EventType.STARTED);
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
						LOGGER.error("An error occurred while starting in a resource in subsystem " + this, e);
						setState(State.RESOLVED);
						postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
					}
				}
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
				// TODO Persist stop state.
				for (Resource resource : constituents) {
					try {
						stopResource(resource);
					}
					catch (Exception e) {
						LOGGER.error("An error occurred while stopping resource " + resource + " of subsystem " + this, e);
						// TODO Should FAILED go out for each failure?
						postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
					}
				}
				// TODO Can we automatically assume it actually is resolved?
				setState(State.RESOLVED);
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.STOPPED);
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
				for (Iterator<Resource> iterator = constituents.iterator(); iterator.hasNext();) {
					Resource resource = iterator.next();
					try {
						uninstallResource(resource);
					}
					catch (Exception e) {
						LOGGER.error("An error occurred while uninstalling resource " + resource + " of subsystem " + this, e);
						// TODO Should FAILED go out for each failure?
						postEvent(AriesSubsystem.this, SubsystemConstants.EventType.FAILED, e);
					}
					iterator.remove();
				}
				parent.children.remove(AriesSubsystem.this);
				locationToSubsystem.remove(location);
				deleteFile(Activator.getBundleContext().getDataFile("subsystem" + id + System.getProperty("file.separator")));
				setState(State.UNINSTALLED);
				postEvent(AriesSubsystem.this, SubsystemConstants.EventType.UNINSTALLED);
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
	
	protected boolean contains(Resource resource) {
		return constituents.contains(resource);
	}
	
	protected Collection<Bundle> getBundles() {
		ArrayList<Bundle> result = new ArrayList<Bundle>(constituents.size());
		for (Resource resource : constituents) {
			if (resource instanceof BundleRevision)
				result.add(((BundleRevision)resource).getBundle());
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
		if (isRoot())
			// The root subsystem's region should be the same one the subsystem bundle was installed into.
			return Activator.getRegionDigraph().getRegion(Activator.getBundleContext().getBundle());
		if (isFeature())
			// Feature subsystems do not have regions because they are not isolated.
			return null;
		// All other subsystems get a dedicated region for isolation.
		return Activator.getRegionDigraph().createRegion(getSymbolicName() + ';' + getVersion());
	}
	
	private AriesSubsystem getConstituentOf(Resource resource, AriesSubsystem provisionTo, boolean transitive) {
		// Application and composite resources become constituents of the application or composite.
		AriesSubsystem constituentOf;
		if (transitive) {
			// Transitive dependencies become constituents of the subsystem into which they were provisioned.
			constituentOf = provisionTo;
		} 
		else {
			// All other resources become constituents of the subsystem in which they were declared.
			constituentOf = this;
		}
		return constituentOf;
	}
	
	private AriesSubsystem getProvisionTo(Resource resource, boolean transitive) {
		// Application and composite resources are provisioned into the application or composite.
		AriesSubsystem provisionTo = this;
		if (transitive) {
			// Transitive dependencies should be provisioned into the highest possible level.
			// TODO Assumes root is always the appropriate level.
			while (provisionTo.getParent() != null)
				provisionTo = provisionTo.getParent();
		}
		else {
			if (provisionTo.isFeature())
				// Feature resources should be provisioned into the first parent that's not a feature.
				while (provisionTo.region == null)
					provisionTo = provisionTo.getParent();
		}
		return provisionTo;
	}
	
	private synchronized void initialize(InputStream content) throws BundleException, IOException, URISyntaxException {
		if (content == null)
			content = new URL(location).openStream();
		File rootDirectory = Activator.getBundleContext().getDataFile("");
		File subsystemDirectory = new File(rootDirectory, "subsystem" + id + System.getProperty("file.separator"));
		archive = new Archive(location, subsystemDirectory, content);
		region = createRegion();
		environment = new SubsystemEnvironment(this);
		if (archive.getSubsystemManifest() == null) {
			SubsystemUri uri = new SubsystemUri(location);
			archive.setSubsystemManifest(SubsystemManifest.newInstance(uri.getSymbolicName(), uri.getVersion(), archive.getResources()));
		}
	}

	private synchronized void install() throws Exception {
		List<Resource> contentResources = new ArrayList<Resource>();
		List<Resource> transitiveDependencies = new ArrayList<Resource>();
		DeploymentManifest manifest = getDeploymentManifest();
		DeployedContentHeader contentHeader = manifest.getDeployedContent();
		for (DeployedContent content : contentHeader.getDeployedContents()) {
			Collection<Capability> capabilities = environment.findProviders(
					new OsgiIdentityRequirement(content.getName(), content.getDeployedVersion(), content.getNamespace(), false));
			if (capabilities.isEmpty())
				throw new SubsystemException("Subsystem content resource does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
			Resource resource = capabilities.iterator().next().getResource();
			contentResources.add(resource);
		}
		ProvisionResourceHeader resourceHeader = manifest.getProvisionResource();
		if (resourceHeader != null) {
			for (ProvisionedResource content : resourceHeader.getProvisionedResources()) {
				Collection<Capability> capabilities = environment.findProviders(
						new OsgiIdentityRequirement(content.getName(), content.getDeployedVersion(), content.getNamespace(), true));
				if (capabilities.isEmpty())
					throw new SubsystemException("Subsystem content resource does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
				Resource resource = capabilities.iterator().next().getResource();
				transitiveDependencies.add(resource);
			}
		}
		// Install content resources and transitive dependencies.
		if (!contentResources.isEmpty()) {
			Coordination coordination = Activator.getCoordinator().create(getSymbolicName() + '-' + getSubsystemId(), 0);
			try {
				// Install the content resources.
				for (Resource resource : contentResources) {
					installResource(resource, coordination, false);
				}
				// Discover and install transitive dependencies.
				for (Resource resource : transitiveDependencies) {
					installResource(resource, coordination, true);
				}
			}
			catch (Exception e) {
				coordination.fail(e);
			}
			finally {
				coordination.end();
			}
		}
	}
	
	private void installBundleResource(Resource resource, Coordination coordination, boolean transitive) throws BundleException, IOException {
		AriesSubsystem provisionTo;
		Bundle bundle;
		synchronized (resourceToSubsystems) {
			if (resource instanceof BundleRevision) {
				resourceToSubsystems.get(resource).add(this);
				return;
			}
			provisionTo = getProvisionTo(resource, transitive);
			URL content = environment.getContent(resource);
			String location = provisionTo.getSubsystemId() + '@' + provisionTo.getSymbolicName() + '@' + content;
			bundle = provisionTo.region.installBundle(location, content.openStream());
			final BundleRevision revision = bundle.adapt(BundleRevision.class);
			Set<Subsystem> subsystems = new HashSet<Subsystem>();
			subsystems.add(this);
			resourceToSubsystems.put(revision, subsystems);
		}
		final AriesSubsystem constituentOf = getConstituentOf(resource, provisionTo, transitive);
		final BundleRevision revision = bundle.adapt(BundleRevision.class);
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				constituentOf.constituents.add(revision);
			}
	
			public void failed(Coordination coordination) throws Exception {
				revision.getBundle().uninstall();
				synchronized (resourceToSubsystems) {
					Set<Subsystem> subsystems = resourceToSubsystems.get(revision);
					subsystems.remove(AriesSubsystem.this);
					if (subsystems.isEmpty()) {
						resourceToSubsystems.remove(revision);
					}
				}
			}
		});
	}

	private void installResource(Resource resource, Coordination coordination, boolean transitive) throws IOException, BundleException {
		String type = ResourceHelper.getTypeAttribute(resource);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			installSubsystemResource(resource, coordination, transitive);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			installBundleResource(resource, coordination, transitive);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}

	private void installSubsystemResource(Resource resource, Coordination coordination, boolean transitive) throws IOException {
		final AriesSubsystem subsystem;
		synchronized (resourceToSubsystems) {
			if (resource instanceof Subsystem) {
				resourceToSubsystems.get(resource).add(this);
				return;
			}
			URL content = environment.getContent(resource);
			String location = id + '@' + getSymbolicName() + '@' + content;
			subsystem = (AriesSubsystem)install(location, content.openStream());
			Set<Subsystem> subsystems = new HashSet<Subsystem>();
			subsystems.add(this);
			resourceToSubsystems.put(subsystem, subsystems);
		}
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
			}
	
			public void failed(Coordination coordination) throws Exception {
				subsystem.uninstall();
				synchronized (resourceToSubsystems) {
					Set<Subsystem> subsystems = resourceToSubsystems.get(subsystem);
					subsystems.remove(AriesSubsystem.this);
					if (subsystems.isEmpty()) {
						resourceToSubsystems.remove(subsystem);
					}
				}
			}
		});
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
			// TODO Implement import isolation policy for applications.
			// Applications have an implicit import policy equating to "import everything that I require", which is not the same as features.
			// This must be computed from the application requirements and will be done using the Wires returned by the Resolver, when one is available.
			region.connectRegion(
					parent.region, 
					region.getRegionDigraph().createRegionFilterBuilder().allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE).build());
		}
		else if (isComposite()) {
			// TODO Implement import isolation policy for composites.
			// Composites specify an explicit import policy in their subsystem and deployment manifests.
		}
	}

	private void startBundleResource(Resource resource, Coordination coordination) throws BundleException {
		final Bundle bundle = ((BundleRevision)resource).getBundle();
		bundle.start(Bundle.START_TRANSIENT);
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

	private void startResource(Resource resource, Coordination coordination) throws BundleException {
		String type = ResourceHelper.getTypeAttribute(resource);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			startSubsystemResource(resource, coordination);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			startBundleResource(resource, coordination);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}

	private void startSubsystemResource(Resource resource, Coordination coordination) {
		final Subsystem subsystem = (Subsystem)resource;
		subsystem.start();
		if (coordination == null)
			return;
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
			}
	
			public void failed(Coordination coordination) throws Exception {
				subsystem.stop();
			}
		});
	}

	private void stopBundleResource(Resource resource) throws BundleException {
		((BundleRevision)resource).getBundle().stop();
	}

	private void stopResource(Resource resource) throws BundleException {
		String type = ResourceHelper.getTypeAttribute(resource);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			stopSubsystemResource(resource);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			stopBundleResource(resource);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}

	private void stopSubsystemResource(Resource resource) {
		((Subsystem)resource).stop();
	}

	private void uninstallBundleResource(Resource resource) throws BundleException {
		synchronized (resourceToSubsystems) {
			Set<Subsystem> subsystems = resourceToSubsystems.get(resource);
			subsystems.remove(this);
			if (!subsystems.isEmpty()) {
				return;
			}
			subsystems.remove(resource);
		}
		((BundleRevision)resource).getBundle().uninstall();
	}

	private void uninstallResource(Resource resource) throws BundleException {
		String type = ResourceHelper.getTypeAttribute(resource);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			uninstallSubsystemResource(resource);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			uninstallBundleResource(resource);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}

	private void uninstallSubsystemResource(Resource resource) {
		synchronized (resourceToSubsystems) {
			Set<Subsystem> subsystems = resourceToSubsystems.get(resource);
			subsystems.remove(this);
			if (!subsystems.isEmpty()) {
				return;
			}
			subsystems.remove(resource);
		}
		((Subsystem)resource).uninstall();
	}
}

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

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.aries.subsystem.core.ResourceHelper;
import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeployedContentHeader.DeployedContent;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader.ProvisionedResource;
import org.apache.aries.subsystem.core.archive.SubsystemArchive;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.VersionHeader;
import org.apache.aries.subsystem.core.obr.SubsystemEnvironment;
import org.apache.aries.subsystem.core.resource.SubsystemFileResource;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AriesSubsystem implements Subsystem, Resource, RepositoryContent {
	public static final String ROOT_SYMBOLIC_NAME = "org.osgi.service.subsystem.root";
	public static final Version ROOT_VERSION = Version.parseVersion("1.0.0");
	public static final String ROOT_LOCATION = "subsystem://?"
			+ SubsystemConstants.SUBSYSTEM_SYMBOLICNAME + '='
			+ ROOT_SYMBOLIC_NAME + '&' + SubsystemConstants.SUBSYSTEM_VERSION
			+ '=' + ROOT_VERSION;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AriesSubsystem.class);
	
	private static final Map<String, AriesSubsystem> locationToSubsystem = Collections.synchronizedMap(new HashMap<String, AriesSubsystem>());
	private static final Map<Resource, Set<AriesSubsystem>> resourceToSubsystems = Collections.synchronizedMap(new HashMap<Resource, Set<AriesSubsystem>>());
	
	private static long lastId;
	
	static synchronized Collection<AriesSubsystem> getSubsystems(Resource resource) {
		// TODO Does this need to be a copy? Unmodifiable?
		Collection<AriesSubsystem> result = resourceToSubsystems.get(resource);
		if (result == null)
			return Collections.emptyList();
		return result;
	}
	
	private static void copyContent(InputStream content, File destination) throws IOException {
		copyContent(
				new BufferedInputStream(content),
				new BufferedOutputStream(new FileOutputStream(destination)));
	}
	
	private static void copyContent(InputStream content, OutputStream destination) throws IOException {
		// TODO What's the optimal byte array size? Put this in a constant?
		byte[] bytes = new byte[2048];
		int read;
		try {
			while ((read = content.read(bytes)) != -1)
				destination.write(bytes, 0, read);
		}
		finally {
			try {
				destination.close();
			}
			catch (IOException e) {}
			try {
				content.close();
			}
			catch (IOException e) {}
		}
	}
	
	private static void unzipContent(File content, File destination) throws IOException {
		unzipContent(
				new BufferedInputStream(new FileInputStream(content)),
				destination);
	}
	
	private static void unzipContent(InputStream content, File destination) throws IOException {
		ZipInputStream zis = new ZipInputStream(content);
		try {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				try {
					File file = new File(destination, entry.getName());
					if (entry.isDirectory()) {
						if (!file.exists() && !file.mkdirs())
							throw new SubsystemException("Failed to create resource directory: " + file);
					}
					// TODO Let's just overwrite any existing resources for now.
//					else if (file.exists())
//						throw new SubsystemException("Resource already exists: " + file);
					else {
						OutputStream fos = new FileOutputStream(file);
						try {
							byte[] bytes = new byte[2048];
							int read;
							while ((read = zis.read(bytes)) != -1)
								fos.write(bytes, 0, read);
						}
						finally {
							try {
								fos.close();
							}
							catch (IOException e) {}
						}
					}
				}
				finally {
					try {
						zis.closeEntry();
					}
					catch (IOException e) {}
				}
			}
		}
		finally {
			try {
				zis.close();
			}
			catch (IOException e) {}
		}
	}

	private static void deleteFile(File file) {
		LOGGER.debug(LOG_ENTRY, "deleteFile", file);
		if (file.isDirectory()) {
			deleteFiles(file.listFiles());
		}
		LOGGER.debug("Deleting file {}", file);
		if (!file.delete())
			LOGGER.warn("Unable to delete file {}", file);
		LOGGER.debug(LOG_EXIT, "deleteFile");
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
	
	private final SubsystemArchive archive;
	private final Set<AriesSubsystem> children = Collections.synchronizedSet(new HashSet<AriesSubsystem>());
	private final Set<Resource> constituents = Collections.synchronizedSet(new HashSet<Resource>());
	private final File directory;
	private final SubsystemEnvironment environment;
	private final long id;
	private final String location;
	private final ArrayList<AriesSubsystem> parents = new ArrayList<AriesSubsystem>();
	private final Region region;
	
	private Subsystem.State state;
	 
	public AriesSubsystem() throws Exception {
		// Create the root subsystem.
		LOGGER.debug(LOG_ENTRY, "init");
		id = 0;
		location = ROOT_LOCATION;
		region = createRegion(null);
		setState(State.ACTIVE);
		environment = new SubsystemEnvironment(this);
		// TODO The directory field is kept separate from the archive so that it can be referenced
		// by any embedded child subsystems during archive initialization. See the constructors.
		directory = Activator.getInstance().getBundleContext().getDataFile("");
		archive = new SubsystemArchive(directory);
		// TODO The creation of the subsystem manifest is in two places. See other constructor.
		SubsystemManifest manifest = archive.getSubsystemManifest();
		if (manifest == null) {
			// This is the first time the root subsystem has been initialized in this framework or
			// a framework clean start was requested.
			SubsystemUri uri = new SubsystemUri(ROOT_LOCATION);
			manifest = SubsystemManifest.newInstance(uri.getSymbolicName(), uri.getVersion(), archive.getResources());
			archive.setSubsystemManifest(manifest);
		}
		else
			// Need to generate a new subsystem manifest in order to generated a new deployment manifest based
			// on any persisted resources.
			manifest = SubsystemManifest.newInstance(getSymbolicName(), getVersion(), archive.getResources());
		archive.setDeploymentManifest(DeploymentManifest.newInstance(manifest, environment));
		StaticDataFile sdf = archive.getStaticDataFile();
		LOGGER.debug("Data file: {}", sdf);
		if (sdf != null) {
			lastId = sdf.getLastSubsystemId();
		}
		LOGGER.debug("Last ID will start at {}", lastId);
		// TODO Begin proof of concept.
		// This is a proof of concept for initializing the relationships between the root subsystem and bundles
		// that already existed in its region. Not sure this will be the final resting place. Plus, there are issues
		// since this does not take into account the possibility of already existing bundles going away or new bundles
		// being installed out of band while this initialization is taking place. Need a bundle event hook for that.
		BundleContext context = Activator.getInstance().getBundleContext();
		for (long id : region.getBundleIds()) {
			BundleRevision br = context.getBundle(id).adapt(BundleRevision.class);
			Set<AriesSubsystem> s = resourceToSubsystems.get(br);
			if (s == null) {
				s = new HashSet<AriesSubsystem>();
				resourceToSubsystems.put(br, s);
			}
			// TODO Escaping 'this' reference.
			s.add(this);
			// TODO What the heck is going on here? Don't we need to add the bundle revision
			// as a constituent as well?
		}
		// TODO End proof of concept.
		LOGGER.debug(LOG_EXIT, "init");
	}
	
	public AriesSubsystem(String location, InputStream content, AriesSubsystem parent) throws Exception {
		// Create a non-root subsystem.
		SubsystemUri uri = null;
		if (location.startsWith("subsystem://"))
			uri = new SubsystemUri(location);
		if (content == null) {
			if (uri != null)
				content = uri.getURL().openStream();
			else
				content = new URL(location).openStream();
			
		}
		this.location = location;
		this.parents.add(parent);
		id = getNextId();
		String directoryName = "subsystem" + id;
		String fileName = directoryName + ".ssa";
		File zipFile = new File(parent.directory, fileName);
		directory = new File(parent.directory, directoryName);
		if (!directory.mkdir())
			throw new IOException("Unable to make directory for " + directory.getCanonicalPath());
		try {
			copyContent(content, zipFile);
			unzipContent(zipFile, directory);
			archive = new SubsystemArchive(directory);
		}
		catch (Exception e) {
			deleteFile(directory);
			deleteFile(zipFile);
			throw new SubsystemException(e);
		}
		environment = new SubsystemEnvironment(this);
		if (archive.getSubsystemManifest() == null) {
			// TODO Since it's optional to use the subsystem URI, it might be
			// better to create the URI in a try/catch block and throw an
			// exception with a message indicating we received a subsystem
			// with no manifest and no subsystem URI.
			archive.setSubsystemManifest(SubsystemManifest.newInstance(
					uri.getSymbolicName(), 
					uri.getVersion(), 
					archive.getResources()));
			// TODO If the subsystem manifest is not null, need to provide default headers if subsystem URI was used.
		}
		region = createRegion(getSymbolicName() + ';' + getVersion());
	}
	
	public AriesSubsystem(SubsystemArchive archive, AriesSubsystem parent) throws Exception {
		this.archive = archive;
		DataFile data = archive.getDataFile();
		if (data == null)
			throw new IllegalArgumentException("Missing data file");
		this.location = data.getLocation();
		this.parents.add(parent);
		id = data.getSubsystemId();
		String directoryName = "subsystem" + id;
//		String fileName = directoryName + ".ssa";
		directory = new File(parent.directory, directoryName);
//		if (!directory.mkdir())
//			throw new IOException("Unable to make directory for "
//					+ directory.getCanonicalPath());
//		File zipFile = new File(directory, fileName);
//		try {
//			copyContent(content, zipFile);
//			unzipContent(zipFile);
//			archive = new SubsystemArchive(directory);
//		} catch (Exception e) {
//			deleteFile(directory);
//			deleteFile(zipFile);
//			throw new SubsystemException(e);
//		}
		environment = new SubsystemEnvironment(this);
//		if (archive.getSubsystemManifest() == null) {
//			// TODO Since it's optional to use the subsystem URI, it might be
//			// better to create the URI in a try/catch block and throw an
//			// exception with a message indicating we received a subsystem
//			// with no manifest and no subsystem URI.
//			archive.setSubsystemManifest(SubsystemManifest.newInstance(
//					uri.getSymbolicName(), uri.getVersion(),
//					archive.getResources()));
//			// TODO If the subsystem manifest is not null, need to provide
//			// default headers if subsystem URI was used.
//		}
		region = createRegion(data.getRegionName());
	}
	
//	public AriesSubsystem(File content, AriesSubsystem parent) throws Exception {
//		// Create a non-root, persisted subsystem.
//		if (!content.isDirectory())
//			throw new IllegalArgumentException(content.getCanonicalPath());
//		this.parents.add(parent);
//		directory = content;
//		// TODO The following call leads to a potentially dangerous escaping 'this' reference to this subsystem
//		// (as the parent) if there is an embedded subsystem archive within this subsystem's archive.
//		// Need to investigate.
//		archive = new SubsystemArchive(directory);
//		environment = new SubsystemEnvironment(this);
//		DataFile data = archive.getDataFile();
//		id = data.getSubsystemId();
//		location = data.getLocation();
//		ServiceProvider sp = Activator.getInstance().getServiceProvider();
//		RegionDigraph digraph = sp.getService(RegionDigraph.class);
//		region = digraph.getRegion(data.getRegionName());
//	}
	
	public SubsystemArchive getArchive() {
		return archive;
	}
	
	@Override
	public BundleContext getBundleContext() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		// TODO Need to filter by namespace.
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
			BundleContext context = Activator.getInstance().getBundleContext();
			for (Long bundleId : bundleIds)
				resources.add(context.getBundle(bundleId).adapt(BundleRevision.class));
			return resources;
		}
		return Collections.unmodifiableCollection(constituents);
	}
	
	@Override
	public InputStream getContent() throws IOException {
		// TODO Assumes original archive location remains valid. Might want to
		// copy zipped archive to subsystem directory as well as extracted contents.
		return new URL(location).openStream();
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public Collection<Subsystem> getParents() {
		return Collections.unmodifiableCollection(new ArrayList<Subsystem>(parents));
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}

	@Override
	public synchronized Subsystem.State getState() {
		return state;
	}
	
	@Override
	public Map<String, String> getSubsystemHeaders(Locale locale) {
		Collection<Header> headers = archive.getSubsystemManifest().getHeaders();
		Map<String, String> result = new HashMap<String, String>(headers.size());
		for (Header header : headers) {
			String value = header.getValue();
			result.put(header.getName(), value);
		}
		return result;
	}

	@Override
	public long getSubsystemId() {
		return id;
	}

	@Override
	public String getSymbolicName() {
		if (isRoot())
			return ROOT_SYMBOLIC_NAME;
		return ((SubsystemSymbolicNameHeader)archive.getSubsystemManifest().getSubsystemSymbolicName()).getSymbolicName();
	}
	
	@Override
	public String getType() {
		return getSubsystemHeaders(null).get(SubsystemConstants.SUBSYSTEM_TYPE);
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
	public Subsystem install(String location, InputStream content) throws SubsystemException {
		try {
			AriesSubsystem subsystem = locationToSubsystem.get(location);
			if (subsystem != null)
				return subsystem;
			subsystem = new AriesSubsystem(location, content, this);
			Coordination coordination = Activator.getInstance().getServiceProvider().getService(Coordinator.class).create(getSymbolicName() + '-' + getSubsystemId(), 0);
			try {
				installSubsystemResource(subsystem, coordination, false);
				return subsystem;
			}
			catch (Exception e) {
				coordination.fail(e);
				throw e;
			}
			finally {
				coordination.end();
			}
		}
		catch (Exception e) {
			LOGGER.error("Subsystem failed to install: " + location, e);
			throw new SubsystemException(e);
		}
		finally {
			if (content != null) {
				try {
					content.close();
				}
				catch (IOException e) {}
			}
		}
	}
	
	public boolean isApplication() {
		// TODO Add to constants.
		return !isRoot() && "osgi.application".equals(archive.getSubsystemManifest().getSubsystemType().getValue());
	}
	
	public boolean isComposite() {
		// TODO Add to constants.
		return !isRoot() && "osgi.composite".equals(archive.getSubsystemManifest().getSubsystemType().getValue());
	}
	
	public boolean isFeature() {
		// TODO Add to constants.
		return !isRoot() && "osgi.feature".equals(archive.getSubsystemManifest().getSubsystemType().getValue());
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
		if (state == State.INSTALLING || state == State.RESOLVING || state == State.STOPPING) {
			waitForStateChange();
			start();
			return;
		}
		// TODO Should we wait on STARTING to see if the outcome is ACTIVE?
		if (state == State.STARTING || state == State.ACTIVE) {
			return;
		}
		// Resolve the subsystem, if necessary.
		if (state == State.INSTALLED) {
			resolve();
		}
		setState(State.STARTING);
		// TODO Need to hold a lock here to guarantee that another start
		// operation can't occur when the state goes to RESOLVED.
		// Start the subsystem.
		Coordination coordination = Activator.getInstance()
				.getServiceProvider().getService(Coordinator.class)
				.create(getSymbolicName() + '-' + getSubsystemId(), 0);
		try {
			// TODO Need to make sure the constituents are ordered by start level.
			for (Resource resource : constituents) {
				startResource(resource, coordination);
			}
			setState(State.ACTIVE);
//			persist(State.ACTIVE);
		} catch (Exception e) {
			coordination.fail(e);
			// TODO Need to reinstate complete isolation by disconnecting the
			// region and transition to INSTALLED.
		} finally {
			try {
				coordination.end();
			} catch (CoordinationException e) {
				LOGGER.error(
						"An error occurred while starting in a resource in subsystem "
								+ this, e);
				setState(State.RESOLVED);
			}
		}
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
			throw new SubsystemException("Cannot stop from state " + getState());
		}
		else if (getState() == State.STARTING) {
			waitForStateChange();
			stop();
		}
		else if (getState() != State.ACTIVE) {
			return;
		}
		setState(State.STOPPING);
		for (Resource resource : constituents) {
			try {
				stopResource(resource);
			} catch (Exception e) {
				LOGGER.error("An error occurred while stopping resource "
						+ resource + " of subsystem " + this, e);
				// TODO Should FAILED go out for each failure?
			}
		}
		// TODO Can we automatically assume it actually is resolved?
		setState(State.RESOLVED);
//		try {
//			persist(State.RESOLVED);
//		}
//		catch (IOException e) {
//			throw new SubsystemException(e);
//		}
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
		else if (state == State.INSTALLING || state == State.RESOLVING || state == State.STARTING || state == State.STOPPING) {
			waitForStateChange();
			uninstall();
		}
		else if (getState() == State.ACTIVE) {
			stop();
			uninstall();
		}
		setState(State.UNINSTALLING);
		for (Iterator<Resource> iterator = constituents.iterator(); iterator.hasNext();) {
			Resource resource = iterator.next();
			try {
				uninstallResource(resource);
			}
			catch (Exception e) {
				LOGGER.error("An error occurred while uninstalling resource " + resource + " of subsystem " + this, e);
				// TODO Should FAILED go out for each failure?
			}
			iterator.remove();
		}
		for (AriesSubsystem parent : parents)
			parent.children.remove(AriesSubsystem.this);
		locationToSubsystem.remove(location);
		deleteFile(directory);
		setState(State.UNINSTALLED);
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
	
	Region getRegion() {
		return region;
	}
	
	void install() throws Exception {
		List<Resource> contentResources = new ArrayList<Resource>();
		List<Resource> transitiveDependencies = new ArrayList<Resource>();
		DeploymentManifest manifest = getDeploymentManifest();
		DeployedContentHeader contentHeader = manifest.getDeployedContent();
		if (contentHeader != null) {
			for (DeployedContent content : contentHeader.getDeployedContents()) {
				Collection<Capability> capabilities = environment.findProviders(
						new OsgiIdentityRequirement(content.getName(), content.getDeployedVersion(), content.getNamespace(), false));
				if (capabilities.isEmpty())
					throw new SubsystemException("Subsystem content resource does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
				Resource resource = capabilities.iterator().next().getResource();
				contentResources.add(resource);
			}
		}
		ProvisionResourceHeader resourceHeader = manifest.getProvisionResource();
		if (resourceHeader != null) {
			for (ProvisionedResource content : resourceHeader.getProvisionedResources()) {
				Collection<Capability> capabilities = environment.findProviders(
						new OsgiIdentityRequirement(content.getName(), content.getDeployedVersion(), content.getNamespace(), true));
				if (capabilities.isEmpty())
					throw new SubsystemException("Subsystem provisioned resource does not exist: " + content.getName() + ";version=" + content.getDeployedVersion());
				Resource resource = capabilities.iterator().next().getResource();
				transitiveDependencies.add(resource);
			}
		}
		// Install content resources and transitive dependencies.
		if (!contentResources.isEmpty()) {
			Coordination coordination = Activator.getInstance().getServiceProvider().getService(Coordinator.class).create(getSymbolicName() + '-' + getSubsystemId(), 0);
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
				// TODO Log this exception? If not, who's responsible for logging it?
				LOGGER.error("Failed to install subsystem", e);
				coordination.fail(e);
				throw e;
			}
			finally {
				coordination.end();
			}
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
		// TODO Need to update service registration properties so that a
		// ServiceEvent goes out with the updated state.
		this.state = state;
		// The archive will be null if this is the root subsystem.
		if (archive != null) {
			// If necessary, update this subsystem's data file to honor start and stop requests.
			if (EnumSet.of(State.INSTALLED, State.RESOLVED, State.ACTIVE).contains(state)) {
				DataFile data = new DataFile(location, region.getName(), state, id);
				try {
					archive.setDataFile(data);
				}
				catch (IOException e) {
					throw new SubsystemException(e);
				}
			}
		}
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
	
	private Region createRegion(String name) throws BundleException {
		if (name == null)
			return Activator.getInstance().getServiceProvider().getService(RegionDigraph.class).getRegion(Activator.getInstance().getBundleContext().getBundle());
		Region region = Activator.getInstance().getServiceProvider().getService(RegionDigraph.class).getRegion(name);
		if (region == null)
			return Activator.getInstance().getServiceProvider().getService(RegionDigraph.class).createRegion(name);
		return region;
	}
	
	private AriesSubsystem getConstituentOf(Resource resource, AriesSubsystem provisionTo, boolean transitive) {
		// Transitive resources always become constituents of the subsystem to which they were provisioned.
		if (transitive)
			return provisionTo;
		// Non-transitive resources become constituents of the subsystem in which they were declared.
		return this;
	}
	
	private DeploymentManifest getDeploymentManifest() throws IOException {
		if (archive.getDeploymentManifest() == null) {
			archive.setDeploymentManifest(DeploymentManifest.newInstance(archive.getSubsystemManifest(), environment));
		}
		return archive.getDeploymentManifest();
	}
	
	private AriesSubsystem getProvisionTo(Resource resource, boolean transitive) {
		// Application and composite resources are provisioned into the application or composite.
		AriesSubsystem provisionTo = this;
		if (transitive) {
			// Transitive dependencies should be provisioned into the highest possible level.
			// TODO Assumes root is always the appropriate level.
			while (!provisionTo.parents.isEmpty())
				provisionTo = provisionTo.parents.get(0);
		}
		else {
			while (provisionTo.isFeature())
				// Feature resources should be provisioned into the first parent that's not a feature.
				provisionTo = provisionTo.parents.get(0);
		}
		return provisionTo;
	}

	private void installBundleResource(Resource resource, Coordination coordination, boolean transitive) throws BundleException, IOException {
		AriesSubsystem provisionTo = null;
		final BundleRevision revision;
		synchronized (resourceToSubsystems) {
			provisionTo = getProvisionTo(resource, transitive);
			if (resource instanceof BundleRevision) {
				// This means the resource is a bundle that's already been installed, but we still need to establish the resource->subsystem relationship.
				revision = (BundleRevision)resource;
			}
			else {
				InputStream content = ((RepositoryContent)resource).getContent();
				String location = provisionTo.getSubsystemId() + '@' + provisionTo.getSymbolicName() + '@' + content;
				Bundle bundle = provisionTo.region.installBundle(location, content);
				revision = bundle.adapt(BundleRevision.class);
			}
			// TODO The null check is necessary for when the bundle is in the root subsystem. Currently, the root subsystem is not initialized with
			// these relationships. Need to decide if that would be better.
			Set<AriesSubsystem> subsystems = resourceToSubsystems.get(revision);
			if (subsystems == null) {
				subsystems = new HashSet<AriesSubsystem>();
				resourceToSubsystems.put(revision, subsystems);
			}
			subsystems.add(this);
		}
		final AriesSubsystem constituentOf = getConstituentOf(resource, provisionTo, transitive);
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				constituentOf.constituents.add(revision);
			}
	
			public void failed(Coordination coordination) throws Exception {
				synchronized (resourceToSubsystems) {
					Set<AriesSubsystem> subsystems = resourceToSubsystems.get(revision);
					subsystems.remove(AriesSubsystem.this);
					if (subsystems.isEmpty()) {
						resourceToSubsystems.remove(revision);
						revision.getBundle().uninstall();
					}
				}
			}
		});
	}

	private void installResource(Resource resource, Coordination coordination, boolean transitive) throws Exception {
		String type = ResourceHelper.getTypeAttribute(resource);
		if (SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM.equals(type))
			installSubsystemResource(resource, coordination, transitive);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			installBundleResource(resource, coordination, transitive);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}

	private void installSubsystemResource(Resource resource, Coordination coordination, boolean transitive) throws Exception {
		final AriesSubsystem subsystem;
		if (resource instanceof AriesSubsystem) {
			subsystem = (AriesSubsystem)resource;
			locationToSubsystem.put(subsystem.getLocation(), subsystem);
		}
		else if (resource instanceof SubsystemFileResource) {
			SubsystemFileResource sfr = (SubsystemFileResource)resource;
			subsystem = (AriesSubsystem)install(sfr.getLocation(), sfr.getContent());
			return;
		}
		else {
			SubsystemArchive archive = (SubsystemArchive)resource;
			subsystem = new AriesSubsystem(archive, this);
			locationToSubsystem.put(subsystem.getLocation(), subsystem);
		}
		subsystem.setState(State.INSTALLING);
		Set<AriesSubsystem> subsystems = new HashSet<AriesSubsystem>();
		subsystems.add(this);
		resourceToSubsystems.put(subsystem, subsystems);
		children.add(subsystem);
		constituents.add(subsystem);
		subsystem.install();
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				subsystem.setState(State.INSTALLED);
			}
	
			public void failed(Coordination coordination) throws Exception {
				subsystem.uninstall();
				constituents.remove(subsystem);
				children.remove(subsystem);
				Set<AriesSubsystem> subsystems = resourceToSubsystems.get(subsystem);
				subsystems.remove(AriesSubsystem.this);
				if (subsystems.isEmpty())
					resourceToSubsystems.remove(subsystem);
				locationToSubsystem.remove(location);
				subsystem.setState(State.INSTALL_FAILED);
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
	
	private void resolve() {
		setState(State.RESOLVING);
		try {
			setImportIsolationPolicy();
			// TODO I think this is insufficient. Do we need both
			// pre-install and post-install environments for the Resolver?
			Collection<Bundle> bundles = getBundles();
			if (!Activator.getInstance().getBundleContext().getBundle(0)
					.adapt(FrameworkWiring.class).resolveBundles(bundles)) {
				LOGGER.error(
						"Unable to resolve bundles for subsystem/version/id {}/{}/{}: {}",
						new Object[] { getSymbolicName(), getVersion(),
								getSubsystemId(), bundles });
				// TODO SubsystemException?
				throw new SubsystemException("Framework could not resolve the bundles");
			}
			setExportIsolationPolicy();
			// TODO Could avoid calling setState (and notifyAll) here and
			// avoid the need for a lock.
			setState(State.RESOLVED);
		} catch (Exception e) {
			setState(State.INSTALLED);
			throw new SubsystemException(e);
		}
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
					parents.get(0).region, 
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

	private void startResource(Resource resource, Coordination coordination) throws BundleException, IOException {
		String type = ResourceHelper.getTypeAttribute(resource);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			startSubsystemResource(resource, coordination);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			startBundleResource(resource, coordination);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}

	private void startSubsystemResource(Resource resource, Coordination coordination) throws IOException {
		final AriesSubsystem subsystem = (AriesSubsystem)resource;
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

	private void stopResource(Resource resource) throws BundleException, IOException {
		String type = ResourceHelper.getTypeAttribute(resource);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			stopSubsystemResource(resource);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			stopBundleResource(resource);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}

	private void stopSubsystemResource(Resource resource) throws IOException {
		((AriesSubsystem)resource).stop();
	}

	private void uninstallBundleResource(Resource resource) throws BundleException {
		LOGGER.debug(LOG_ENTRY, "uninstallBundleResource", resource);
		Bundle bundle = null;
		synchronized (resourceToSubsystems) {
			Set<AriesSubsystem> subsystems = resourceToSubsystems.get(resource);
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Subsystems that currently have {} as a constituent: {}", new Object[]{resource, subsystems});
			subsystems.remove(this);
			if (subsystems.isEmpty()) {
				resourceToSubsystems.remove(resource);
				bundle = ((BundleRevision)resource).getBundle();
			}
		}
		if (bundle != null) {
			LOGGER.debug("Uninstalling bundle {}", bundle);
			bundle.uninstall();
		}
		LOGGER.debug(LOG_EXIT, "uninstallBundleResource");
	}

	private void uninstallResource(Resource resource) throws BundleException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(LOG_ENTRY, "uninstallResource", resource);
			LOGGER.debug("Subsystem {} is uninstalling resource {};{};{}", new Object[]{
					getSymbolicName(),
					ResourceHelper.getSymbolicNameAttribute(resource),
					ResourceHelper.getVersionAttribute(resource),
					ResourceHelper.getTypeAttribute(resource)
			});
		}
		String type = ResourceHelper.getTypeAttribute(resource);
		// TODO Add to constants.
		if ("osgi.subsystem".equals(type))
			uninstallSubsystemResource(resource);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			uninstallBundleResource(resource);
		else
			throw new SubsystemException("Unsupported resource type: " + type);
		LOGGER.debug(LOG_EXIT, "uninstallResource");
	}

	private void uninstallSubsystemResource(Resource resource) {
		synchronized (resourceToSubsystems) {
			Set<AriesSubsystem> subsystems = resourceToSubsystems.get(resource);
			subsystems.remove(this);
			if (!subsystems.isEmpty()) {
				return;
			}
			subsystems.remove(resource);
		}
		((AriesSubsystem)resource).uninstall();
	}
}

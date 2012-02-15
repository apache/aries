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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
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
import org.apache.aries.subsystem.core.obr.SubsystemEnvironment;
import org.apache.aries.subsystem.core.resource.SubsystemDirectoryResource;
import org.apache.aries.subsystem.core.resource.SubsystemFileResource;
import org.apache.aries.subsystem.core.resource.SubsystemStreamResource;
import org.apache.aries.util.io.IOUtils;
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

public class AriesSubsystem implements Subsystem, Resource {
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
	private final Set<AriesSubsystem> parents = Collections.synchronizedSet(new HashSet<AriesSubsystem>());
	private final Region region;
	
	private boolean autostart;
	private Subsystem.State state = State.INSTALLING;
	 
	public AriesSubsystem() throws Exception {
		// Create the root subsystem.
		LOGGER.debug(LOG_ENTRY, "init");
		// TODO The directory field is kept separate from the archive so that it can be referenced
		// by any embedded child subsystems during archive initialization. See the constructors.
		directory = Activator.getInstance().getBundleContext().getDataFile("");
		archive = new SubsystemArchive(directory);
		DeploymentManifest deploymentManifest = archive.getDeploymentManifest();
		if (deploymentManifest != null) {
			autostart = Boolean.parseBoolean(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_AUTOSTART).getValue());
			id = Long.parseLong(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_ID).getValue());
			lastId = Long.parseLong(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_LASTID).getValue());
			location = deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_LOCATION).getValue();
		}
		else {
			autostart = true;
			id = 0;
			location = ROOT_LOCATION;
		}
		region = createRegion(null);
		// TODO The creation of the subsystem manifest is in two places. See other constructor.
		SubsystemManifest subsystemManifest = archive.getSubsystemManifest();
		if (subsystemManifest == null) {
			// This is the first time the root subsystem has been initialized in this framework or
			// a framework clean start was requested.
			SubsystemUri uri = new SubsystemUri(ROOT_LOCATION);
			subsystemManifest = new SubsystemManifest(uri.getSymbolicName(), uri.getVersion(), archive.getResources());
			archive.setSubsystemManifest(subsystemManifest);
		}
		else
			// Need to generate a new subsystem manifest in order to generated a new deployment manifest based
			// on any persisted resources.
			subsystemManifest = new SubsystemManifest(getSymbolicName(), getVersion(), archive.getResources());
		environment = new SubsystemEnvironment(this);
		archive.setDeploymentManifest(new DeploymentManifest(
				deploymentManifest, 
				subsystemManifest, 
				environment,
				autostart,
				id,
				lastId,
				location,
				true));
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
			constituents.add(br);
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
			environment = new SubsystemEnvironment(this);
			// Make sure the relevant headers are derived, if absent.
			archive.setSubsystemManifest(new SubsystemManifest(
					archive.getSubsystemManifest(),
					uri == null ? null : uri.getSymbolicName(), 
					uri == null ? null : uri.getVersion(), 
					archive.getResources()));
			// Unscoped subsystems don't get their own region. They share the region with their scoped parent.
			if (isFeature())
				region = parent.region;
			else
				region = createRegion(getSymbolicName() + ';' + getVersion() + ';' + getType() + ';' + getSubsystemId());
		}
		catch (Exception e) {
			deleteFile(directory);
			deleteFile(zipFile);
			throw new SubsystemException(e);
		}
	}
	
	public AriesSubsystem(SubsystemArchive archive, AriesSubsystem parent) throws Exception {
		this.archive = archive;
		DeploymentManifest manifest = archive.getDeploymentManifest();
		if (manifest == null)
			throw new IllegalStateException("Missing deployment manifest");
		autostart = Boolean.parseBoolean(manifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_AUTOSTART).getValue());
		id = Long.parseLong(manifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_ID).getValue());
		location = manifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_LOCATION).getValue();
		String directoryName = "subsystem" + id;
		directory = new File(parent.directory, directoryName);
		environment = new SubsystemEnvironment(this);
		parents.add(parent);
		// Unscoped subsystems don't get their own region. They share the region with their scoped parent.
		if (isFeature())
			region = parent.region;
		else
			region = createRegion(getSymbolicName() + ';' + getVersion() + ';' + getType() + ';' + getSubsystemId());
	}
	
	public SubsystemArchive getArchive() {
		return archive;
	}
	
	@Override
	public BundleContext getBundleContext() {
		if (EnumSet.of(State.INSTALL_FAILED, State.UNINSTALLED).contains(getState()))
			return null;
		Region region = this.region;
		Subsystem subsystem = this;
		// Features, and unscoped subsystems in general, do not have their own region context
		// bundle but rather share with the scoped subsystem in the same region.
		if (isFeature()) {
			for (Subsystem parent : getParents()) {
				if (!((AriesSubsystem)parent).isFeature()) {
					region = ((AriesSubsystem)parent).getRegion();
					subsystem = parent;
				}
			}
		}
		return region.getBundle(RegionContextBundleHelper.SYMBOLICNAME_PREFIX + subsystem.getSubsystemId(), RegionContextBundleHelper.VERSION).getBundleContext();
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null || namespace.equals(ResourceConstants.IDENTITY_NAMESPACE)) {
			Capability capability = new OsgiIdentityCapability(this, getSymbolicName(), getVersion(), SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM, getType());
			return Arrays.asList(new Capability[]{capability});
		}
		return Collections.emptyList();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<Subsystem> getChildren() {
		return (Collection<Subsystem>)(Collection)Collections.unmodifiableCollection(new ArrayList<Subsystem>(children));
	}

	@Override
	public synchronized Collection<Resource> getConstituents() {
		return Collections.unmodifiableCollection(new ArrayList<Resource>(constituents));
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
		Map<String, Header<?>> headers = archive.getSubsystemManifest().getHeaders();
		Map<String, String> result = new HashMap<String, String>(headers.size());
		for (Entry<String, Header<?>> entry: headers.entrySet()) {
			Header<?> value = entry.getValue();
			result.put(entry.getKey(), value.getValue());
		}
		return result;
	}

	@Override
	public long getSubsystemId() {
		return id;
	}

	@Override
	public String getSymbolicName() {
		return archive.getSubsystemManifest().getSubsystemSymbolicNameHeader().getSymbolicName();
	}
	
	@Override
	public String getType() {
		return archive.getSubsystemManifest().getSubsystemTypeHeader().getValue();
	}

	@Override
	public Version getVersion() {
		return archive.getSubsystemManifest().getSubsystemVersionHeader().getVersion();
	}

	@Override
	public Subsystem install(String location) throws SubsystemException {
		return install(location, null);
	}
	
	@Override
	public Subsystem install(String location, InputStream content) throws SubsystemException {
		SubsystemStreamResource ssr = null;
		try {
			TargetRegion region = new TargetRegion(this);
			ssr = new SubsystemStreamResource(location, content);
			AriesSubsystem subsystem = locationToSubsystem.get(location);
			if (subsystem != null) {
				if (!region.contains(subsystem))
					throw new SubsystemException("Location already exists but existing subsystem is not part of target region: " + location);
				if (!(subsystem.getSymbolicName().equals(ssr.getSubsystemSymbolicName())
						&& subsystem.getVersion().equals(ssr.getSubsystemVersion())
						&& subsystem.getType().equals(ssr.getSubsystemType())))
					throw new SubsystemException("Location already exists but symbolic name, version, and type are not the same: " + location);
				children.add(subsystem);
				constituents.add(subsystem);
				return subsystem;
			}
			subsystem = (AriesSubsystem)region.find(ssr.getSubsystemSymbolicName(), ssr.getSubsystemVersion());
			if (subsystem != null) {
				if (!subsystem.getType().equals(ssr.getSubsystemType()))
					throw new SubsystemException("Subsystem already exists in target region but has a different type: " + location);
				children.add(subsystem);
				constituents.add(subsystem);
				return subsystem;
			}
			subsystem = new AriesSubsystem(location, ssr.getContent(), this);
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
			if (ssr != null)
				ssr.close();
			IOUtils.close(content);
		}
	}
	
	public boolean isApplication() {
		return !isRoot()
				&& SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(archive
						.getSubsystemManifest().getHeaders()
						.get(SubsystemManifest.SUBSYSTEM_TYPE).getValue());
	}
	
	public boolean isComposite() {
		return !isRoot() 
				&& SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(archive
						.getSubsystemManifest().getHeaders()
						.get(SubsystemManifest.SUBSYSTEM_TYPE).getValue());
	}
	
	public boolean isFeature() {
		return !isRoot() 
				&& SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(archive
						.getSubsystemManifest().getHeaders()
						.get(SubsystemManifest.SUBSYSTEM_TYPE).getValue());
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
		autostart = true;
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
						"An error occurred while starting a resource in subsystem "
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
		// The root subsystem may not be stopped.
		checkRoot();
		autostart = false;
		stop0();
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
		// The root subsystem may not be uninstalled.
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
		// Uninstall child subsystems first.
		for (Subsystem subsystem : getChildren()) {
			try {
				uninstallSubsystemResource((AriesSubsystem)subsystem);
			}
			catch (Exception e) {
				LOGGER.error("An error occurred while uninstalling resource " + subsystem + " of subsystem " + this, e);
				// TODO Should FAILED go out for each failure?
			}
		}
		// Uninstall any remaining constituents.
		for (Resource resource : getConstituents()) {
			// Don't uninstall the region context bundle here.
			if (ResourceHelper.getSymbolicNameAttribute(resource).startsWith(RegionContextBundleHelper.SYMBOLICNAME_PREFIX))
				continue;
			try {
				uninstallResource(resource);
			}
			catch (Exception e) {
				LOGGER.error("An error occurred while uninstalling resource " + resource + " of subsystem " + this, e);
				// TODO Should FAILED go out for each failure?
			}
		}
		for (AriesSubsystem parent : parents) {
			parent.children.remove(this);
			parent.constituents.remove(this);
		}
		locationToSubsystem.remove(location);
		deleteFile(directory);
		setState(State.UNINSTALLED);
		Activator.getInstance().getSubsystemServiceRegistrar().unregister(this);
		if (!isFeature())
			constituents.remove(RegionContextBundleHelper.uninstallRegionContextBundle(this));
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
	
	synchronized void install() throws Exception {
		if (!isFeature())
			constituents.add(RegionContextBundleHelper.installRegionContextBundle(this));
		Activator.getInstance().getSubsystemServiceRegistrar().register(this);
		Set<Resource> contentResources = new TreeSet<Resource>(
				new Comparator<Resource>() {
					@Override
					public int compare(Resource o1, Resource o2) {
						if (o1.equals(o2))
							// Consistent with equals.
							return 0;
						String t1 = ResourceHelper.getTypeAttribute(o1);
						String t2 = ResourceHelper.getTypeAttribute(o2);
						boolean b1 = ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(t1)
								|| ResourceConstants.IDENTITY_TYPE_FRAGMENT.equals(t1);
						boolean b2 = ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(t2)
								|| ResourceConstants.IDENTITY_TYPE_FRAGMENT.equals(t2);
						if (b1 && !b2)
							// o1 is a bundle or fragment but o2 is not.
							return -1;
						if (!b1 && b2)
							// o1 is not a bundle or fragment but o2 is.
							return 1;
						// Either both or neither are bundles or fragments. In this case we don't care about the order.
						return -1;
					}
				});
		List<Resource> transitiveDependencies = new ArrayList<Resource>();
		DeploymentManifest manifest = getDeploymentManifest();
		DeployedContentHeader contentHeader = manifest.getDeployedContentHeader();
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
		ProvisionResourceHeader resourceHeader = manifest.getProvisionResourceHeader();
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
		setState(State.INSTALLED);
		if (autostart)
			start();
	}
	
	void stop0() {
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
		// Stop child subsystems first.
		for (AriesSubsystem subsystem : children) {
			try {
				stopSubsystemResource(subsystem);
			}
			catch (Exception e) {
				LOGGER.error("An error occurred while stopping resource "
						+ subsystem + " of subsystem " + this, e);
			}
		}
		// For non-root subsystems, stop any remaining constituents.
		if (!isRoot()){
			for (Resource resource : constituents) {
				// Don't stop the region context bundle.
				if (ResourceHelper.getSymbolicNameAttribute(resource).startsWith(RegionContextBundleHelper.SYMBOLICNAME_PREFIX))
					continue;
				try {
					stopResource(resource);
				} catch (Exception e) {
					LOGGER.error("An error occurred while stopping resource "
							+ resource + " of subsystem " + this, e);
					// TODO Should FAILED go out for each failure?
				}
			}
		}
		// TODO Can we automatically assume it actually is resolved?
		setState(State.RESOLVED);
		DeploymentManifest manifest = new DeploymentManifest(
				archive.getDeploymentManifest(),
				null,
				null,
				autostart,
				id,
				lastId,
				location,
				false);
		try {
			archive.setDeploymentManifest(manifest);
		}
		catch (IOException e) {
			throw new SubsystemException(e);
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
		Activator.getInstance().getSubsystemServiceRegistrar().update(this);
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
		Activator activator = Activator.getInstance();
		RegionDigraph digraph = activator.getServiceProvider().getService(RegionDigraph.class);
		if (name == null)
			return digraph.getRegion(activator.getBundleContext().getBundle());
		Region region = digraph.getRegion(name);
		if (region == null)
			return digraph.createRegion(name);
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
			archive.setDeploymentManifest(new DeploymentManifest(
					null,
					archive.getSubsystemManifest(), 
					environment,
					autostart,
					id,
					lastId,
					location,
					true));
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
				provisionTo = provisionTo.parents.iterator().next();
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
				String location = provisionTo.getSubsystemId() + "@" + provisionTo.getSymbolicName() + "@" + ResourceHelper.getSymbolicNameAttribute(resource);
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
		constituentOf.constituents.add(revision);
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
			}
	
			public void failed(Coordination coordination) throws Exception {
				synchronized (resourceToSubsystems) {
					constituentOf.constituents.remove(revision);
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
		else if (ResourceConstants.IDENTITY_TYPE_FRAGMENT.equals(type))
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
		else if (resource instanceof SubsystemDirectoryResource) {
			SubsystemDirectoryResource sdr = (SubsystemDirectoryResource)resource;
			subsystem = new AriesSubsystem(sdr.getArchive(), this);
			locationToSubsystem.put(subsystem.getLocation(), subsystem);
		}
		else if (resource instanceof RepositoryContent) {
			String location = getSubsystemId() + "@" + getSymbolicName() + "@" + ResourceHelper.getSymbolicNameAttribute(resource);
			subsystem = (AriesSubsystem)install(location, ((RepositoryContent)resource).getContent());
			return;
		}
		else {
			throw new IllegalArgumentException("Unrecognized subsystem resource: " + resource);
		}
		Set<AriesSubsystem> subsystems = new HashSet<AriesSubsystem>();
		subsystems.add(this);
		resourceToSubsystems.put(subsystem, subsystems);
		children.add(subsystem);
		constituents.add(subsystem);
		subsystem.install();
		coordination.addParticipant(new Participant() {
			public void ended(Coordination coordination) throws Exception {
				// noop
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
					parents.iterator().next().region, 
					region.getRegionDigraph().createRegionFilterBuilder().allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE).build());
		}
		else if (isComposite()) {
			// TODO Implement import isolation policy for composites.
			// Composites specify an explicit import policy in their subsystem and deployment manifests.
		}
	}

	private void startBundleResource(Resource resource, Coordination coordination) throws BundleException {
		final Bundle bundle = ((BundleRevision)resource).getBundle();
		if ((bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0)
			return;
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
		if (SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM.equals(type))
			startSubsystemResource(resource, coordination);
		else if (ResourceConstants.IDENTITY_TYPE_BUNDLE.equals(type))
			startBundleResource(resource, coordination);
		else if (ResourceConstants.IDENTITY_TYPE_FRAGMENT.equals(type)) {
			// Fragments are not started.
		}
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
		if (SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM.equals(type))
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
		if (SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM.equals(type))
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

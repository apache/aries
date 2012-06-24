package org.apache.aries.subsystem.core.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.aries.subsystem.core.archive.DeployedContentHeader;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class AriesSubsystem implements Resource, Subsystem {
	public static final String ROOT_REGION = "org.eclipse.equinox.region.kernel";
	public static final String ROOT_SYMBOLIC_NAME = "org.osgi.service.subsystem.root";
	public static final Version ROOT_VERSION = Version.parseVersion("1.0.0");
	public static final String ROOT_LOCATION = "subsystem://?"
			+ SubsystemConstants.SUBSYSTEM_SYMBOLICNAME + '='
			+ ROOT_SYMBOLIC_NAME + '&' + SubsystemConstants.SUBSYSTEM_VERSION
			+ '=' + ROOT_VERSION;
	
	private DeploymentManifest deploymentManifest;
	private SubsystemResource resource;
	private SubsystemManifest subsystemManifest;
	
	private final IDirectory directory;
	
	public AriesSubsystem(SubsystemResource resource) throws URISyntaxException, IOException, BundleException, InvalidSyntaxException {
		this.resource = resource;
//		long id;
//		if (resource.getParents().isEmpty())
//			id = 0;
//		else
//			id = SubsystemIdentifier.getNextId();
		File file = new File(Activator.getInstance().getBundleContext().getDataFile(""), Long.toString(resource.getId()));
		file.mkdirs();
		directory = FileSystem.getFSRoot(file);
		setSubsystemManifest(resource.getSubsystemManifest());
		SubsystemManifestValidator.validate(this, getSubsystemManifest());
		setDeploymentManifest(new DeploymentManifest.Builder()
				.manifest(resource.getSubsystemManifest())
				.manifest(resource.getDeploymentManifest())
				.location(resource.getLocation())
				.autostart(false)
				.id(resource.getId())
				.lastId(SubsystemIdentifier.getLastId())
				.region(resource.getRegion().getName())
				.state(State.INSTALLING)
				.build());
	}
	
	public AriesSubsystem(File file) throws IOException, URISyntaxException, ResolutionException {
		this(FileSystem.getFSRoot(file));
	}
	
	public AriesSubsystem(IDirectory directory) throws IOException, URISyntaxException, ResolutionException {
		this.directory = directory;
		setDeploymentManifest(new DeploymentManifest.Builder().manifest(getDeploymentManifest()).state(State.INSTALLING).build());
	}
	
	/* BEGIN Resource interface methods. */
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace)) {
			Capability capability = new OsgiIdentityCapability(this, getSymbolicName(), getVersion(), getType());
			return Collections.singletonList(capability);
		}
		SubsystemManifest manifest = getSubsystemManifest();
		if (namespace == null) {
			Capability capability = new OsgiIdentityCapability(this, getSymbolicName(), getVersion(), getType());
			List<Capability> result = manifest.toCapabilities(this);
			result.add(capability);
			return result;
		}
		List<Capability> result = manifest.toCapabilities(this);
		for (Iterator<Capability> i = result.iterator(); i.hasNext();)
			if (!i.next().getNamespace().equals(namespace))
				i.remove();
		return result;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		SubsystemManifest manifest = getSubsystemManifest();
		if (namespace == null)
			return manifest.toRequirements(this);
		List<Requirement> result = manifest.toRequirements(this);
		for (Iterator<Requirement> i = result.iterator(); i.hasNext();)
			if (!i.next().getNamespace().equals(namespace))
				i.remove();
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
		return AccessController.doPrivileged(new GetSubsystemHeadersAction(this));
	}

	@Override
	public String getLocation() {
		return getDeploymentManifestHeaderValue(DeploymentManifest.ARIESSUBSYSTEM_LOCATION);
	}

	@Override
	public Collection<Subsystem> getParents() {
		Header<?> header = deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_PARENTS);
		if (header == null)
			return Collections.emptyList();
		String[] parents = header.getValue().split(",");
		Collection<Subsystem> result = new ArrayList<Subsystem>(parents.length);
		for (String parent : parents)
			result.add(Activator.getInstance().getSubsystems().getSubsystemById(Long.valueOf(parent)));
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
	public Subsystem install(String location) {
		return install(location, null);
	}

	@Override
	public Subsystem install(String location, InputStream content) {
		try {
			return AccessController.doPrivileged(new InstallAction(location, content, this, AccessController.getContext()));
		}
		finally {
			// This method must guarantee the content input stream was closed.
			IOUtils.close(content);
		}
	}

	@Override
	public void start() {
		SecurityManager.checkExecutePermission(this);
		AccessController.doPrivileged(new StartAction(this));
	}

	@Override
	public void stop() {
		SecurityManager.checkExecutePermission(this);
		AccessController.doPrivileged(new StopAction(this));
	}

	@Override
	public void uninstall() {
		SecurityManager.checkLifecyclePermission(this);
		AccessController.doPrivileged(new UninstallAction(this));
	}
	
	/* END Subsystem interface methods. */
	
	void addedContent(Resource resource) {
		try {
			setDeploymentManifest(new DeploymentManifest.Builder()
					.manifest(getDeploymentManifest()).content(resource).build());
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	void addedParent(AriesSubsystem subsystem) {
		try {
			setDeploymentManifest(new DeploymentManifest.Builder()
					.manifest(getDeploymentManifest()).parent(subsystem.getSubsystemId()).build());
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
	
	org.eclipse.equinox.region.Region getRegion() {
		DeploymentManifest manifest = getDeploymentManifest();
		Header<?> header = manifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_REGION);
		return Activator.getInstance().getRegionDigraph().getRegion(header.getValue());
	}
	
	synchronized SubsystemResource getResource() {
		if (resource == null) {
			try {
				resource = new SubsystemResource(directory);
			}
			catch (Exception e) {
				throw new SubsystemException(e);
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
		Collection<DeployedContentHeader.Clause> clauses = new ArrayList<DeployedContentHeader.Clause>(header.getClauses());
		for (Iterator<DeployedContentHeader.Clause> i = clauses.iterator(); i.hasNext();)
			if (clause.equals(i.next())) {
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
			setDeploymentManifest(new DeploymentManifest.Builder()
					.manifest(getDeploymentManifest()).autostart(value).build());
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
	
	synchronized void setDeploymentManifest(DeploymentManifest value) throws IOException, URISyntaxException {
		File file = new File(getDirectory(), "OSGI-INF");
		if (!file.exists())
			file.mkdirs();
		FileOutputStream fos = new FileOutputStream(new File(file, "DEPLOYMENT.MF"));
		try {
			value.write(fos);
			deploymentManifest = value;
		}
		finally {
			IOUtils.close(fos);
		}
	}
	
	void setState(State value) {
		if (value.equals(getState()))
			return;
		try {
			setDeploymentManifest(new DeploymentManifest.Builder()
					.manifest(getDeploymentManifest()).state(value).build());
		} catch (Exception e) {
			throw new SubsystemException(e);
		}
		Activator.getInstance().getSubsystemServiceRegistrar().update(this);
		synchronized (this) {
			notifyAll();
		}
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
	
//	private org.eclipse.equinox.region.Region createRegion(SubsystemResource resource, long id) throws BundleException {
//		if (!isScoped())
//			return resource.getParents().iterator().next().getRegion();
//		Activator activator = Activator.getInstance();
//		RegionDigraph digraph = activator.getRegionDigraph();
//		if (resource.getParents().isEmpty())
//			return digraph.getRegion(ROOT_REGION);
//		String name = resource.getSubsystemManifest()
//				.getSubsystemSymbolicNameHeader().getSymbolicName()
//				+ ';'
//				+ resource.getSubsystemManifest().getSubsystemVersionHeader()
//						.getVersion()
//				+ ';'
//				+ resource.getSubsystemManifest().getSubsystemTypeHeader()
//						.getType() + ';' + Long.toString(id);
//		org.eclipse.equinox.region.Region region = digraph.getRegion(name);
//		// TODO New regions need to be cleaned up if this subsystem fails to
//		// install, but there's no access to the coordination here.
//		if (region == null)
//			return digraph.createRegion(name);
//		return region;
//	}
	
	private String getDeploymentManifestHeaderValue(String name) {
		DeploymentManifest manifest = getDeploymentManifest();
		if (manifest == null)
			return null;
		Header<?> header = manifest.getHeaders().get(name);
		if (header == null)
			return null;
		return header.getValue();
	}
	
//	private Manifest getManifest(String name) {
//		try {
//			return ManifestProcessor.obtainManifestFromAppDir(directory, name);
//		}
//		catch (IOException e) {
//			throw new SubsystemException(e);
//		}
//	}
}

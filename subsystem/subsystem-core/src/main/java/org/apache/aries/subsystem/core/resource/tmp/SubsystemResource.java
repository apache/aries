package org.apache.aries.subsystem.core.resource.tmp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.internal.Activator;
import org.apache.aries.subsystem.core.internal.SubsystemUri;
import org.apache.aries.subsystem.core.resource.BundleResource;
import org.apache.aries.subsystem.core.resource.CompositeRepository;
import org.apache.aries.subsystem.core.resource.DependencyCalculator;
import org.apache.aries.subsystem.core.resource.LocalRepository;
import org.apache.aries.subsystem.core.resource.RepositoryServiceRepository;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemException;

public abstract class SubsystemResource implements Resource {
	protected static class Location {
		private final String symbolicName;
		private final String value;
		private final Version version;
		
		public Location(String location) throws MalformedURLException, URISyntaxException {
			value = location;
			SubsystemUri uri = null;
			if (location.startsWith("subsystem://"))
				uri = new SubsystemUri(location);
			symbolicName = uri == null ? null : uri.getSymbolicName();
			version = uri == null ? null : uri.getVersion();
		}
		
		public String getSymbolicName() {
			return symbolicName;
		}
		
		public String getValue() {
			return value;
		}
		
		public Version getVersion() {
			return version;
		}
	}
	
	protected static final Pattern PATTERN = Pattern.compile("([^@]+)(?:@(.+))?.esa");
	
	public static SubsystemResource newInstance(String location, InputStream content) throws IOException, URISyntaxException {
		if (content == null)
			content = new URL(location).openStream();
		IDirectory directory = FileSystem.getFSRoot(content);
		SubsystemManifest manifest = computeSubsystemManifest(directory);
		String type = manifest.getSubsystemTypeHeader().getType();
		// TODO Make an enum out of the types?
		if (SubsystemTypeHeader.TYPE_APPLICATION.equals(type))
			return new ApplicationResource(new Location(location), directory, manifest);
		if (SubsystemTypeHeader.TYPE_COMPOSITE.equals(type))
			return new CompositeResource(new Location(location), directory, manifest);
		if (SubsystemTypeHeader.TYPE_FEATURE.equals(type))
			return new FeatureResource(new Location(location), directory, manifest);
		throw new SubsystemException("Unsupported subsystem type: " + type);
	}
	
	protected static SubsystemManifest computeExistingSubsystemManifest(IDirectory directory) throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/SUBSYSTEM.MF");
		if (manifest == null)
			return null;
		return new SubsystemManifest(manifest);
	}
	
	protected static SubsystemManifest computeNewSubsystemManifest() {
		return new SubsystemManifest.Builder().build();
	}
	
	protected static SubsystemManifest computeSubsystemManifest(IDirectory directory) throws IOException {
		SubsystemManifest result = computeExistingSubsystemManifest(directory);
		if (result == null)
			result = computeNewSubsystemManifest();
		return result;
	}
	
	protected static String convertFileToLocation(IFile file) throws MalformedURLException {
		String result = convertFileNameToLocation(file.getName());
		if (result == null)
			result = file.toURL().toString();
		return result;
	}
	
	protected static String convertFileNameToLocation(String fileName) {
		Matcher matcher = PATTERN.matcher(fileName);
		if (!matcher.matches())
			return null;
		String version = matcher.group(2);
		return new SubsystemUri(matcher.group(1), version == null ? null
				: Version.parseVersion(version), null).toString();
	}
	
	protected final List<Capability> capabilities;
	protected final DeploymentManifest deploymentManifest;
	protected final IDirectory directory;
	protected final Location location;
	protected final List<Requirement> requirements;
	protected final Collection<Resource> resources;
	protected final SubsystemManifest subsystemManifest;
	
	public SubsystemResource(Location location, IDirectory directory, SubsystemManifest manifest) throws IOException, URISyntaxException {
		this.location = location;
		this.directory = directory;
		resources = computeResources();
		manifest = computeSubsystemManifestBeforeRequirements(manifest);
		requirements = computeRequirements(manifest);
		subsystemManifest = computeSubsystemManifestAfterRequirements(manifest);
		capabilities = computeCapabilities();
		deploymentManifest = computeDeploymentManifest();
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(capabilities);
		ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (Capability capability : capabilities)
			if (namespace.equals(capability.getNamespace()))
				result.add(capability);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}
	
	public DeploymentManifest getDeploymentManifest() {
		return deploymentManifest;
	}
	
	public String getLocation() {
		return location.getValue();
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(requirements);
		ArrayList<Requirement> result = new ArrayList<Requirement>(requirements.size());
		for (Requirement requirement : requirements)
			if (namespace.equals(requirement.getNamespace()))
				result.add(requirement);
		result.trimToSize();
		return Collections.unmodifiableList(result);
	}
	
	public Collection<Resource> getResources() {
		return resources;
	}
	
	public SubsystemManifest getSubsystemManifest() {
		return subsystemManifest;
	}
	
	protected void addHeader(SubsystemManifest.Builder builder, Header<?> header) {
		if (header == null)
			return;
		builder.header(header);
	}
	
	protected void addImportPackageHeader(SubsystemManifest.Builder builder) {
		addHeader(builder, computeImportPackageHeader());
	}
	
	protected void addRequireBundleHeader(SubsystemManifest.Builder builder) {
		addHeader(builder, computeRequireBundleHeader());
	}
	
	protected void addRequireCapabilityHeader(SubsystemManifest.Builder builder) {
		addHeader(builder, computeRequireCapabilityHeader());
	}
	
	protected void addSubsystemContentHeader(SubsystemManifest.Builder builder, SubsystemManifest manifest) {
		SubsystemContentHeader header = computeSubsystemContentHeader(manifest);
		if (header == null)
			return;
		addHeader(builder, header);
	}
	
	protected void addSubsystemSymbolicNameHeader(SubsystemManifest.Builder builder, SubsystemManifest manifest) {
		addHeader(builder, computeSubsystemSymbolicNameHeader(manifest));
	}
	
	protected List<Capability> computeCapabilities() {
		return subsystemManifest.toCapabilities(this);
	}
	
	protected DeploymentManifest computeExistingDeploymentManifest() throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/DEPLOYMENT.MF");
		if (manifest == null)
			return null;
		return new DeploymentManifest(manifest);
	}
	
	protected DeploymentManifest computeDeploymentManifest() throws IOException {
		return computeExistingDeploymentManifest();
	}
	
	protected ImportPackageHeader computeImportPackageHeader() {
		if (requirements.isEmpty())
			return null;
		ArrayList<ImportPackageHeader.Clause> clauses = new ArrayList<ImportPackageHeader.Clause>(requirements.size());
		for (Requirement requirement : requirements) {
			if (!PackageNamespace.PACKAGE_NAMESPACE.equals(requirement.getNamespace()))
				continue;
			clauses.add(new ImportPackageHeader.Clause(requirement));
		}
		if (clauses.isEmpty())
			return null;
		clauses.trimToSize();
		return new ImportPackageHeader(clauses);
	}
	
	protected RequireBundleHeader computeRequireBundleHeader() {
		if (requirements.isEmpty())
			return null;
		ArrayList<RequireBundleHeader.Clause> clauses = new ArrayList<RequireBundleHeader.Clause>(requirements.size());
		for (Requirement requirement : requirements) {
			if (!BundleNamespace.BUNDLE_NAMESPACE.equals(requirement.getNamespace()))
				continue;
			clauses.add(new RequireBundleHeader.Clause(requirement));
		}
		if (clauses.isEmpty())
			return null;
		clauses.trimToSize();
		return new RequireBundleHeader(clauses);
	}
	
	protected RequireCapabilityHeader computeRequireCapabilityHeader() {
		if (requirements.isEmpty())
			return null;
		ArrayList<RequireCapabilityHeader.Clause> clauses = new ArrayList<RequireCapabilityHeader.Clause>();
		for (Requirement requirement : requirements) {
			if (requirement.getNamespace().startsWith("osgi."))
				continue;
			clauses.add(new RequireCapabilityHeader.Clause(requirement));
		}
		if (clauses.isEmpty())
			return null;
		clauses.trimToSize();
		return new RequireCapabilityHeader(clauses);
	}
	
	protected List<Requirement> computeRequirements(SubsystemManifest manifest) {
		SubsystemContentHeader header = manifest.getSubsystemContentHeader();
		if (header == null)
			return Collections.emptyList();
		// TODO Need the system repository in here. Preferred provider as well?
		LocalRepository localRepo = new LocalRepository(resources);
		RepositoryServiceRepository serviceRepo = new RepositoryServiceRepository(Activator.getInstance().getBundleContext());
		CompositeRepository compositeRepo = new CompositeRepository(localRepo, serviceRepo);
		List<Requirement> requirements = header.toRequirements();
		List<Resource> resources = new ArrayList<Resource>(requirements.size());
		for (Requirement requirement : requirements) {
			Collection<Capability> capabilities = compositeRepo.findProviders(requirement);
			if (capabilities.isEmpty())
				continue;
			resources.add(capabilities.iterator().next().getResource());
		}
		return new DependencyCalculator(resources).calculateDependencies();
	}
	
	protected Collection<Resource> computeResources() throws IOException, URISyntaxException {
		List<IFile> files = directory.listFiles();
		if (files.isEmpty())
			return Collections.emptyList();
		ArrayList<Resource> result = new ArrayList<Resource>(files.size());
		for (IFile file : directory.listFiles()) {
			String name = file.getName();
			if (name.endsWith(".jar"))
				result.add(BundleResource.newInstance(file.toURL()));
			else if (name.endsWith(".esa"))
				result.add(SubsystemResource.newInstance(convertFileToLocation(file), file.open()));
		}
		result.trimToSize();
		return result;
	}
	
	protected SubsystemContentHeader computeSubsystemContentHeader(SubsystemManifest manifest) {
		Header<?> header = manifest.getSubsystemContentHeader();
		if (header == null && !resources.isEmpty())
			header = new SubsystemContentHeader(resources);
		return (SubsystemContentHeader)header;
	}
	
	protected SubsystemManifest computeSubsystemManifestAfterRequirements(SubsystemManifest manifest) {
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder().manifest(manifest);
		addImportPackageHeader(builder);
		addRequireBundleHeader(builder);
		addRequireCapabilityHeader(builder);
		return builder.build();
	}
	
	protected SubsystemManifest computeSubsystemManifestBeforeRequirements(SubsystemManifest manifest) {
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder().manifest(manifest);
		addSubsystemSymbolicNameHeader(builder, manifest);
		addSubsystemContentHeader(builder, manifest);
		return builder.build();
	}
	
	protected SubsystemSymbolicNameHeader computeSubsystemSymbolicNameHeader(SubsystemManifest manifest) {
		Header<?> header = manifest.getSubsystemSymbolicNameHeader();
		if (header == null)
			header = new SubsystemSymbolicNameHeader(location.getSymbolicName());
		return (SubsystemSymbolicNameHeader)header;
	}
}

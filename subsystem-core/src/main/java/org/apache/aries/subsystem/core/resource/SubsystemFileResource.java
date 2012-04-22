package org.apache.aries.subsystem.core.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.archive.SubsystemVersionHeader;
import org.apache.aries.subsystem.core.internal.Activator;
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
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemFileResource implements Resource, RepositoryContent {
	private static final String REGEX = "([^@]+)(?:@(.+))?.esa"; // TODO Add .esa to constants.
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	private final List<Capability> capabilities;
	private final DeploymentManifest deploymentManifest;
	private final IDirectory directory;
	private final File file;
	private final IFile iFile;
	private final String location;
	private final List<Requirement> requirements;
	private final Collection<Resource> resources;
	private final SubsystemManifest subsystemManifest;
	
	public SubsystemFileResource(File content) throws IOException, ResolutionException {
		this(FileSystem.getFSRoot(content), content, null);
	}
	
	private SubsystemFileResource(IDirectory directory, File file, IFile iFile) throws IOException, ResolutionException {
		this.directory = directory;
		this.file = file;
		this.iFile = iFile;
		resources = computeResources();
		deploymentManifest = generateDeploymentManifest();
		subsystemManifest = generateSubsystemManifest();
		capabilities = subsystemManifest.toCapabilities(this);
		requirements = computeDependencies();
		location = computeLocation();
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null)
			return capabilities;
		ArrayList<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (Capability capability : capabilities)
			if (namespace.equals(capability.getNamespace()))
				result.add(capability);
		result.trimToSize();
		return result;
	}

	@Override
	public InputStream getContent() {
		try {
			return file == null ? iFile.open() : new FileInputStream(file);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getLocation() {
		return location;
	}
	
	public DeploymentManifest getDeploymentManifest() {
		return deploymentManifest;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		if (namespace == null)
			return requirements;
		ArrayList<Requirement> result = new ArrayList<Requirement>(requirements.size());
		for (Requirement requirement : requirements)
			if (namespace.equals(requirement.getNamespace()))
				result.add(requirement);
		result.trimToSize();
		return result;
	}
	
	public Collection<Resource> getResources() {
		return resources;
	}
	
	public SubsystemManifest getSubsystemManifest() {
		return subsystemManifest;
	}
	
	private List<Requirement> computeDependencies() throws IOException, ResolutionException {
		SubsystemTypeHeader type = subsystemManifest.getSubsystemTypeHeader();
		if (SubsystemTypeHeader.TYPE_APPLICATION.equals(type.getType()))
			return computeDependenciesForApplication();
		return subsystemManifest.toRequirements(this);
	}
	
	private List<Requirement> computeDependenciesForApplication() throws IOException, ResolutionException {
		SubsystemContentHeader header = subsystemManifest.getSubsystemContentHeader();
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
	
	private ImportPackageHeader computeImportPackageHeaderForApplication() {
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
	
	private String computeLocation() {
		return "subsystem://?" + SubsystemConstants.SUBSYSTEM_SYMBOLICNAME
				+ '=' 
				+ subsystemManifest.getSubsystemSymbolicNameHeader().getSymbolicName() 
				+ '&' + SubsystemConstants.SUBSYSTEM_VERSION + '='
				+ subsystemManifest.getSubsystemVersionHeader().getVersion();
	}
	
	private RequireBundleHeader computeRequireBundleHeaderForApplication() {
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
	
	private RequireCapabilityHeader computeRequireCapabilityHeaderForApplication() {
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
	
	private Collection<Resource> computeResources() throws IOException, ResolutionException {
		List<IFile> files = directory.listFiles();
		if (files.isEmpty())
			return Collections.emptyList();
		ArrayList<Resource> result = new ArrayList<Resource>(files.size());
		for (IFile file : directory.listFiles()) {
			String name = file.getName();
			if (name.endsWith(".jar"))
				result.add(BundleResource.newInstance(file.toURL()));
			else if (name.endsWith(".esa"))
				result.add(new SubsystemFileResource(file.convertNested(), null, file));
		}
		result.trimToSize();
		return result;
	}
	
	private void fillInSubsystemManifestContent(SubsystemManifest.Builder builder) {
		builder.content(resources);
	}
	
	private SubsystemManifest fillInSubsystemManifestDefaults(SubsystemManifest manifest) {
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder().manifest(manifest);
		fillInSubsystemManifestSymbolicNameAndVersionFromFileName(manifest, builder);
		if (manifest.getSubsystemContentHeader() == null)
			fillInSubsystemManifestContent(builder);
		if (manifest.getSubsystemTypeHeader().getType().equals(SubsystemTypeHeader.TYPE_APPLICATION))
			fillInSubsystemManifestForApplication(builder);
		return builder.build();
	}
	
	private void fillInSubsystemManifestForApplication(SubsystemManifest.Builder builder) {
		builder
		.header(computeImportPackageHeaderForApplication())
		.header(computeRequireBundleHeaderForApplication())
		.header(computeRequireCapabilityHeaderForApplication());
	}
	
	private void fillInSubsystemManifestSymbolicNameAndVersionFromFileName(
			SubsystemManifest manifest, 
			SubsystemManifest.Builder builder) {
		String fileName = file == null ? iFile.getName() : file.getName();
		Matcher matcher = PATTERN.matcher(fileName);
		if (!matcher.matches())
			return;
		fillInSubsystemManifestSymbolicName(builder, manifest.getSubsystemSymbolicNameHeader(), matcher.group(1));
		fillInSubsystemManifestVersion(builder, manifest.getSubsystemVersionHeader(), matcher.group(2));
	}
	
	private void fillInSubsystemManifestSymbolicName(
			SubsystemManifest.Builder builder,
			SubsystemSymbolicNameHeader header,
			String symbolicName) {
		if (header == null)
			builder.symbolicName(symbolicName);
	}
	
	private void fillInSubsystemManifestVersion(
			SubsystemManifest.Builder builder,
			SubsystemVersionHeader header,
			String version) {
		if (version == null)
			return;
		fillInSubsystemManifestVersion(builder, header, Version.parseVersion(version));
	}
	
	private void fillInSubsystemManifestVersion(
			SubsystemManifest.Builder builder,
			SubsystemVersionHeader header,
			Version version) {
		if (header.getVersion().equals(Version.emptyVersion))
			builder.version(version);
	}
	
	private SubsystemManifest generateDefaultSubsystemManifest() {
		return new SubsystemManifest(generateDefaultSymbolicName(), null, resources);
	}
	
	private String generateDefaultSymbolicName() {
		String fileName = file == null ? iFile.getName() : file.getName();
		Matcher matcher = PATTERN.matcher(fileName);
		if (!matcher.matches())
			throw new IllegalArgumentException("No symbolic name");
		return matcher.group(1);
	}
	
	private DeploymentManifest generateDeploymentManifest() throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/DEPLOYMENT.MF");
		if (manifest == null)
			return null;
		return new DeploymentManifest(manifest);
	}
	
	private SubsystemManifest generateExistingSubsystemManifest() throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/SUBSYSTEM.MF");
		if (manifest == null)
			return null;
		return new SubsystemManifest(manifest);
	}
	
	private SubsystemManifest generateSubsystemManifest() throws IOException {
		SubsystemManifest result = generateExistingSubsystemManifest();
		if (result == null)
			result = generateDefaultSubsystemManifest();
		result = fillInSubsystemManifestDefaults(result);
		if (result.getSubsystemSymbolicNameHeader() == null)
			throw new SubsystemException("Missing symbolic name");
		return result;
	}
}

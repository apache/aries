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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemImportServiceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.archive.SubsystemVersionHeader;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;

public class RawSubsystemResource implements Resource {
	private static final Pattern PATTERN = Pattern.compile("([^@/\\\\]+)(?:@(.+))?.esa");
	private static final String APPLICATION_IMPORT_SERVICE_HEADER = "Application-ImportService";
	
	private static SubsystemManifest computeExistingSubsystemManifest(IDirectory directory) throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/SUBSYSTEM.MF");
		if (manifest == null)
			return null;
		return new SubsystemManifest(manifest);
	}
	
	private static SubsystemManifest computeNewSubsystemManifest() {
		return new SubsystemManifest.Builder().build();
	}
	
	private static SubsystemManifest computeSubsystemManifest(IDirectory directory) throws IOException {
		SubsystemManifest result = computeExistingSubsystemManifest(directory);
		if (result == null)
			result = computeNewSubsystemManifest();
		return result;
	}
	
	private static String convertFileToLocation(IFile file) throws MalformedURLException {
		String result = convertFileNameToLocation(file.getName());
		if (result == null)
			result = file.toURL().toString();
		return result;
	}
	
	private static String convertFileNameToLocation(String fileName) {
		Matcher matcher = PATTERN.matcher(fileName);
		if (!matcher.matches())
			return null;
		String version = matcher.group(2);
		return new SubsystemUri(matcher.group(1), version == null ? null
				: Version.parseVersion(version), null).toString();
	}
	
	private final List<Capability> capabilities;
	private final DeploymentManifest deploymentManifest;
	private final long id;
	private final Repository localRepository;
	private final Location location;
	private final List<Requirement> requirements;
	private final Collection<Resource> resources;
	private final Resource fakeImportServiceResource;
	private final SubsystemManifest subsystemManifest;
	
	public RawSubsystemResource(String location, IDirectory content) throws URISyntaxException, IOException, ResolutionException, ModellerException {
		id = SubsystemIdentifier.getNextId();
		this.location = new Location(location);
		if (content == null)
			content = this.location.open();
		try {
			resources = computeResources(content);
			SubsystemManifest manifest = computeSubsystemManifest(content);
			fakeImportServiceResource = createFakeResource(manifest);
			localRepository = computeLocalRepository();
			manifest = computeSubsystemManifestBeforeRequirements(manifest);
			requirements = computeRequirements(manifest);
			subsystemManifest = computeSubsystemManifestAfterRequirements(manifest);
			capabilities = computeCapabilities();
			deploymentManifest = computeDeploymentManifest(content);
		}
		finally {
			IOUtils.close(content.toCloseable());
		}
	}
	
	public RawSubsystemResource(File file) throws IOException, URISyntaxException, ResolutionException {
		this(FileSystem.getFSRoot(file));
	}
	
	public RawSubsystemResource(IDirectory idir) throws IOException, URISyntaxException, ResolutionException {
		resources = Collections.emptyList();
		fakeImportServiceResource = null; // not needed for persistent subsystems
		localRepository = computeLocalRepository();
		subsystemManifest = initializeSubsystemManifest(idir);
		requirements = subsystemManifest.toRequirements(this);
		capabilities = subsystemManifest.toCapabilities(this);
		deploymentManifest = initializeDeploymentManifest(idir);
		id = Long.parseLong(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_ID).getValue());
		location = new Location(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_LOCATION).getValue());
	}

	private static Resource createFakeResource(SubsystemManifest manifest) {
		Header<?> importServiceHeader = manifest.getHeaders().get(APPLICATION_IMPORT_SERVICE_HEADER);
		if (importServiceHeader == null) {
			return null;
		}
		List<Capability> modifiableCaps = new ArrayList<Capability>();
		final List<Capability> fakeCapabilities = Collections.unmodifiableList(modifiableCaps);
		Resource fakeResource = new Resource() {

			@Override
			public List<Capability> getCapabilities(String namespace) {
				if (namespace == null) {
					return fakeCapabilities;
				}
				List<Capability> results = new ArrayList<Capability>();
				for (Capability capability : fakeCapabilities) {
					if (namespace.equals(capability.getNamespace())) {
						results.add(capability);
					}
				}
				return results;
			}

			@Override
			public List<Requirement> getRequirements(String namespace) {
				return Collections.emptyList();
			}
		};

		modifiableCaps.add(new OsgiIdentityCapability(fakeResource, Constants.ResourceTypeSynthesized, new Version(1,0,0), Constants.ResourceTypeSynthesized));
    	Map<String, Map<String, String>> serviceImports = ManifestHeaderProcessor.parseImportString(importServiceHeader.getValue());
    	for (Entry<String, Map<String, String>> serviceImport : serviceImports.entrySet()) {
			Collection<String> objectClasses = new ArrayList<String>(Arrays.asList(serviceImport.getKey()));
			String filter = serviceImport.getValue().get(IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE);
			BasicCapability.Builder capBuilder = new BasicCapability.Builder();
			capBuilder.namespace(ServiceNamespace.SERVICE_NAMESPACE);
			capBuilder.attribute(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, objectClasses);
			if (filter != null)
				capBuilder.attributes(new HashMap<String, Object>(ManifestHeaderProcessor.parseFilter(filter)));
			capBuilder.attribute("service.imported", "");
			capBuilder.resource(fakeResource);
			modifiableCaps.add(capBuilder.build());
		}

    	return fakeResource;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RawSubsystemResource))
			return false;
		RawSubsystemResource that = (RawSubsystemResource)o;
		return getLocation().equals(that.getLocation());
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
	
	public long getId() {
		return id;
	}
	
	public Repository getLocalRepository() {
		return localRepository;
	}
	
	public Location getLocation() {
		return location;
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
		return Collections.unmodifiableCollection(resources);
	}
	
	public SubsystemManifest getSubsystemManifest() {
		return subsystemManifest;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + getLocation().hashCode();
		return result;
	}
	
	private void addHeader(SubsystemManifest.Builder builder, Header<?> header) {
		if (header == null)
			return;
		builder.header(header);
	}
	
	private void addImportPackageHeader(SubsystemManifest.Builder builder) {
		addHeader(builder, computeImportPackageHeader());
	}
	
	private void addRequireBundleHeader(SubsystemManifest.Builder builder) {
		addHeader(builder, computeRequireBundleHeader());
	}
	
	private void addRequireCapabilityHeader(SubsystemManifest.Builder builder) {
		addHeader(builder, computeRequireCapabilityHeader());
	}
	
	private void addSubsystemContentHeader(SubsystemManifest.Builder builder, SubsystemManifest manifest) {
		addHeader(builder, computeSubsystemContentHeader(manifest));
	}
	
	private void addSubsystemImportServiceHeader(SubsystemManifest.Builder builder) {
		addHeader(builder, computeSubsystemImportServiceHeader());
	}
	
	private void addSubsystemSymbolicNameHeader(SubsystemManifest.Builder builder, SubsystemManifest manifest) {
		addHeader(builder, computeSubsystemSymbolicNameHeader(manifest));
	}
	
	private void addSubsystemVersionHeader(SubsystemManifest.Builder builder, SubsystemManifest manifest) {
		addHeader(builder, computeSubsystemVersionHeader(manifest));
	}
	
	private List<Capability> computeCapabilities() {
		return subsystemManifest.toCapabilities(this);
	}
	
	private DeploymentManifest computeDeploymentManifest(IDirectory directory) throws IOException {
		return computeExistingDeploymentManifest(directory);
	}
	
	private DeploymentManifest computeExistingDeploymentManifest(IDirectory directory) throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/DEPLOYMENT.MF");
		if (manifest == null)
			return null;
		return new DeploymentManifest(manifest);
	}
	
	private ImportPackageHeader computeImportPackageHeader() {
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
	
	private Repository computeLocalRepository() {
		if (fakeImportServiceResource != null) {
			Collection<Resource> temp = new ArrayList<Resource>(resources);
			temp.add(fakeImportServiceResource);
			return new LocalRepository(temp);
		}
		return new LocalRepository(resources);
	}
	
	private RequireBundleHeader computeRequireBundleHeader() {
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
	
	private RequireCapabilityHeader computeRequireCapabilityHeader() {
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
	
	private List<Requirement> computeRequirements(SubsystemManifest manifest) throws ResolutionException {
		if (isComposite(manifest))
			return manifest.toRequirements(this);
		SubsystemContentHeader header = manifest.getSubsystemContentHeader();
		if (header == null)
			return Collections.emptyList();
		// TODO Need the system repository in here. Preferred provider as well?
		LocalRepository localRepo = new LocalRepository(resources);
		RepositoryServiceRepository serviceRepo = new RepositoryServiceRepository(Activator.getInstance().getBundleContext());
		CompositeRepository compositeRepo = new CompositeRepository(localRepo, serviceRepo);
		List<Requirement> requirements = header.toRequirements(this);
		List<Resource> resources = new ArrayList<Resource>(requirements.size());
		for (Requirement requirement : requirements) {
			Collection<Capability> capabilities = compositeRepo.findProviders(requirement);
			if (capabilities.isEmpty())
				continue;
			resources.add(capabilities.iterator().next().getResource());
		}
		return new DependencyCalculator(resources).calculateDependencies();
	}
	
	private Collection<Resource> computeResources(IDirectory directory) throws IOException, URISyntaxException, ResolutionException, ModellerException {
		List<IFile> files = directory.listFiles();
		if (files.isEmpty())
			return Collections.emptyList();
		ArrayList<Resource> result = new ArrayList<Resource>(files.size());
		for (IFile file : directory.listFiles()) {
			String name = file.getName();
			if (file.isFile()) {
				// Subsystem resources must end with ".esa".
				if (name.endsWith(".esa"))
					result.add(new RawSubsystemResource(convertFileToLocation(file), file.convertNested()));
				else {
					// Assume all other resources are bundles.
					try {
						result.add(new BundleResource(file));
					}
					catch (Exception e) {
						// Ignore if the resource is an invalid bundle or not a bundle at all.
					}
				}
			}
			else {
				if (name.endsWith(".esa"))
					result.add(new RawSubsystemResource(convertFileToLocation(file), file.convert()));
				else {
					try {
						result.add(new BundleResource(file));
					}
					catch (Exception e) {
						// Ignore
					}
				}
			}
		}
		result.trimToSize();
		return result;
	}
	
	private SubsystemContentHeader computeSubsystemContentHeader(SubsystemManifest manifest) {
		SubsystemContentHeader header = manifest.getSubsystemContentHeader();
		if (header == null && !resources.isEmpty())
			header = SubsystemContentHeader.newInstance(resources);
		return header;
	}
	
	private SubsystemImportServiceHeader computeSubsystemImportServiceHeader() {
		if (requirements.isEmpty())
			return null;
		ArrayList<SubsystemImportServiceHeader.Clause> clauses = new ArrayList<SubsystemImportServiceHeader.Clause>(requirements.size());
		for (Requirement requirement : requirements) {
			if (!ServiceNamespace.SERVICE_NAMESPACE.equals(requirement.getNamespace()))
				continue;
			clauses.add(new SubsystemImportServiceHeader.Clause(requirement));
		}
		if (clauses.isEmpty())
			return null;
		clauses.trimToSize();
		return new SubsystemImportServiceHeader(clauses);
	}
	
	private SubsystemManifest computeSubsystemManifestAfterRequirements(SubsystemManifest manifest) {
		if (isComposite(manifest))
			return manifest;
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder().manifest(manifest);
		addImportPackageHeader(builder);
		addRequireBundleHeader(builder);
		addRequireCapabilityHeader(builder);
		addSubsystemImportServiceHeader(builder);
		return builder.build();
	}
	
	private SubsystemManifest computeSubsystemManifestBeforeRequirements(SubsystemManifest manifest) {
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder().manifest(manifest);
		addSubsystemSymbolicNameHeader(builder, manifest);
		addSubsystemVersionHeader(builder, manifest);
		addSubsystemContentHeader(builder, manifest);
		return builder.build();
	}
	
	private SubsystemSymbolicNameHeader computeSubsystemSymbolicNameHeader(SubsystemManifest manifest) {
		SubsystemSymbolicNameHeader header = manifest.getSubsystemSymbolicNameHeader();
		if (header != null)
			return header;
		String symbolicName = location.getSymbolicName();
		if (symbolicName == null)
			symbolicName = "org.apache.aries.subsystem." + id;
		return new SubsystemSymbolicNameHeader(symbolicName);
	}
	
	private SubsystemVersionHeader computeSubsystemVersionHeader(SubsystemManifest manifest) {
		SubsystemVersionHeader header = manifest.getSubsystemVersionHeader();
		if (header.getVersion().equals(Version.emptyVersion) && location.getVersion() != null)
			header = new SubsystemVersionHeader(location.getVersion());
		return header;
	}
	
	private DeploymentManifest initializeDeploymentManifest(IDirectory idir)
			throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(idir,
				"OSGI-INF/DEPLOYMENT.MF");
		if (manifest != null)
			return new DeploymentManifest(manifest);
		else
			return new DeploymentManifest.Builder()
					.manifest(getSubsystemManifest())
					.location(BasicSubsystem.ROOT_LOCATION).autostart(true).id(0)
					.lastId(SubsystemIdentifier.getLastId())
					.state(State.INSTALLING)
					.build();
	}
	
	private SubsystemManifest initializeSubsystemManifest(IDirectory idir)
			throws IOException {
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(idir,
				"OSGI-INF/SUBSYSTEM.MF");
		if (manifest != null)
			return new SubsystemManifest(manifest);
		else
			return new SubsystemManifest.Builder()
					.symbolicName(BasicSubsystem.ROOT_SYMBOLIC_NAME)
					.version(BasicSubsystem.ROOT_VERSION)
					.type(SubsystemTypeHeader.TYPE_APPLICATION
							+ ';'
							+ SubsystemTypeHeader.DIRECTIVE_PROVISION_POLICY
							+ ":="
							+ SubsystemTypeHeader.PROVISION_POLICY_ACCEPT_DEPENDENCIES)
					.build();
	}
	
	private boolean isComposite(SubsystemManifest manifest) {
		return SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(manifest.getSubsystemTypeHeader().getType());
	}
}

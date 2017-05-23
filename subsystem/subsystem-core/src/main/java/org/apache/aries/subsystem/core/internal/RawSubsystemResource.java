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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.ContentHandler;
import org.apache.aries.subsystem.core.archive.AriesProvisionDependenciesDirective;
import org.apache.aries.subsystem.core.archive.Attribute;
import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.GenericHeader;
import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.ImportPackageHeader;
import org.apache.aries.subsystem.core.archive.RequireBundleHeader;
import org.apache.aries.subsystem.core.archive.RequireCapabilityHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader.Clause;
import org.apache.aries.subsystem.core.archive.SubsystemImportServiceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemLocalizationHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.archive.SubsystemVersionHeader;
import org.apache.aries.subsystem.core.capabilityset.SimpleFilter;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawSubsystemResource implements Resource {
	private static final Logger logger = LoggerFactory.getLogger(RawSubsystemResource.class);

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
	private final org.apache.aries.subsystem.core.repository.Repository localRepository;
	private final Location location;
    private final BasicSubsystem parentSubsystem;
	private final List<Requirement> requirements;
	private final Collection<Resource> resources;
	private final Resource fakeImportServiceResource;
	private final SubsystemManifest subsystemManifest;
	private final Collection<TranslationFile> translations;

	public RawSubsystemResource(String location, IDirectory content, BasicSubsystem parent) throws URISyntaxException, IOException, ResolutionException {
		id = SubsystemIdentifier.getNextId();
		this.location = new Location(location);
		this.parentSubsystem = parent;
		if (content == null)
			content = this.location.open();
		try {
            SubsystemManifest manifest = computeSubsystemManifest(content);
            resources = computeResources(content, manifest);
			fakeImportServiceResource = createFakeResource(manifest);
			localRepository = computeLocalRepository();
			manifest = computeSubsystemManifestBeforeRequirements(content, manifest);
			requirements = computeRequirements(manifest);
			subsystemManifest = computeSubsystemManifestAfterRequirements(manifest);
			capabilities = computeCapabilities();
			deploymentManifest = computeDeploymentManifest(content);
			translations = computeTranslations(content);
		}
		finally {
			IOUtils.close(content.toCloseable());
		}
	}

	public RawSubsystemResource(File file, BasicSubsystem parent) throws IOException, URISyntaxException, ResolutionException {
		this(FileSystem.getFSRoot(file), parent);
	}

	public RawSubsystemResource(IDirectory idir, BasicSubsystem parent) throws IOException, URISyntaxException, ResolutionException {
		subsystemManifest = initializeSubsystemManifest(idir);
		requirements = subsystemManifest.toRequirements(this);
		capabilities = subsystemManifest.toCapabilities(this);
		deploymentManifest = initializeDeploymentManifest(idir);
		id = Long.parseLong(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_ID).getValue());
		location = new Location(deploymentManifest.getHeaders().get(DeploymentManifest.ARIESSUBSYSTEM_LOCATION).getValue());
		parentSubsystem = parent;
		translations = Collections.emptyList();
		Map<String, Header<?>> headers = deploymentManifest.getHeaders();
		if (State.INSTALLING.equals(
				State.valueOf(
						headers.get(
								DeploymentManifest.ARIESSUBSYSTEM_STATE).getValue()))
								&& subsystemManifest.getSubsystemTypeHeader().getAriesProvisionDependenciesDirective().isResolve()) {
			URL url = new URL(headers.get(Constants.AriesSubsystemOriginalContent).getValue());
			Collection<Resource> resources;
			try {
				resources = computeResources(FileSystem.getFSRoot(new File(url.toURI())), subsystemManifest);
			}
			catch (IllegalArgumentException e) {
				// Thrown by File if the URI is not hierarchical. For example,
				// when handling a JAR URL.
				resources = computeResources(FileSystem.getFSRoot(url.openStream()), subsystemManifest);
			}
			this.resources = resources;
			fakeImportServiceResource = createFakeResource(subsystemManifest);
		}
		else {
			resources = Collections.emptyList();
			fakeImportServiceResource = null;
		}
		localRepository = computeLocalRepository();
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
				capBuilder.attributes(new HashMap<String, Object>(SimpleFilter.attributes(filter)));
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

	public org.apache.aries.subsystem.core.repository.Repository getLocalRepository() {
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

	public SubsystemManifest getSubsystemManifest() {
		return subsystemManifest;
	}

	public Collection<TranslationFile> getTranslations() {
		return translations;
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
	
	private void addSubsystemTypeHeader(SubsystemManifest.Builder builder, SubsystemManifest manifest) {
		addHeader(builder, computeSubsystemTypeHeader(manifest));
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
			clauses.add(ImportPackageHeader.Clause.valueOf(requirement));
		}
		if (clauses.isEmpty())
			return null;
		clauses.trimToSize();
		return new ImportPackageHeader(clauses);
	}

	private org.apache.aries.subsystem.core.repository.Repository computeLocalRepository() {
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
			clauses.add(RequireBundleHeader.Clause.valueOf(requirement));
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
			String namespace = requirement.getNamespace();
			if (namespace.startsWith("osgi.") && !(
					// Don't filter out the osgi.ee namespace...
					namespace.equals(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE) ||
					// ...or the osgi.service namespace.
					namespace.equals(ServiceNamespace.SERVICE_NAMESPACE)))
				continue;
			clauses.add(RequireCapabilityHeader.Clause.valueOf(requirement));
		}
		if (clauses.isEmpty())
			return null;
		clauses.trimToSize();
		return new RequireCapabilityHeader(clauses);
	}

	private List<Requirement> computeRequirements(SubsystemManifest manifest) throws ResolutionException {
		if (isComposite(manifest)) {
			// Composites determine their own requirements.
			return manifest.toRequirements(this);
		}
		// Gather up all of the content resources for the subsystem.
		SubsystemContentHeader header = manifest.getSubsystemContentHeader();
		if (header == null) {
			// Empty subsystems (i.e. subsystems with no content) are allowed.
			return Collections.emptyList();
		}
		List<Requirement> requirements = header.toRequirements(this);
		List<Resource> resources = new ArrayList<Resource>(requirements.size());
		// TODO Do we need the system repository in here (e.g., for features)?
		// What about the preferred provider repository?
		// Search the local repository and service repositories for content.
		RepositoryServiceRepository serviceRepo = new RepositoryServiceRepository();
		// TODO Should we search the service repositories first, the assumption
		// being they will contain more current content than the subsystem
		// archive?
		CompositeRepository compositeRepo = new CompositeRepository(localRepository, serviceRepo);
		for (Requirement requirement : requirements) {
			Collection<Capability> capabilities = compositeRepo.findProviders(requirement);
			if (!capabilities.isEmpty()) {
				resources.add(capabilities.iterator().next().getResource());
			}
		}
		if (fakeImportServiceResource != null) {
			// Add the fake resource so the dependency calculator knows not to
			// return service requirements that are included in 
			// Application-ImportService.
			resources.add(fakeImportServiceResource);
		}
		// Now compute the dependencies of the content resources. These are
		// dependencies not satisfied by the content resources themselves.
		return new DependencyCalculator(resources).calculateDependencies();
	}

	private Collection<Resource> computeResources(IDirectory directory, SubsystemManifest manifest) throws IOException, URISyntaxException, ResolutionException {
		List<IFile> files = directory.listFiles();
		if (files.isEmpty())
			return Collections.emptyList();
		ArrayList<Resource> result = new ArrayList<Resource>(files.size());
		for (IFile file : directory.listFiles()) {
            if (file.isFile()) {
                addResource(file, file.convertNested(), manifest, result);
            } else if (!file.getName().endsWith("OSGI-INF")) {
                addResource(file, file.convert(), manifest, result);
            }
		}
		result.trimToSize();
		return result;
	}

    private void addResource(IFile file, IDirectory content, SubsystemManifest manifest, ArrayList<Resource> result) throws URISyntaxException,
            IOException, ResolutionException, MalformedURLException {
        String name = file.getName();
        if (name.endsWith(".esa")) {
        	result.add(new RawSubsystemResource(convertFileToLocation(file), content, parentSubsystem));
        } else if (name.endsWith(".jar")) {
            result.add(new BundleResource(file));
        } else {
            // This is a different type of file. Add a file resource for it if there is a custom content handler for it.
            FileResource fr = new FileResource(file);
            fr.setCapabilities(computeFileCapabilities(fr, file, manifest));
            List<Capability> idcaps = fr.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
            if (idcaps.size() > 0) {
                Capability idcap = idcaps.get(0);
                Object type = idcap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
                if (type instanceof String && parentSubsystem != null) {
                    if (CustomResources.getCustomContentHandler(parentSubsystem, (String) type) != null) {
                        // Yes, there is a custom content handler, add it.
                        result.add(fr);
                        return;
                    }
                }
            }

            // There is no custom handler for this resource, let's check if it turns out to be a bundle
            try {
                result.add(new BundleResource(file));
            } catch (Exception e) {
                // Ignore if the resource is an invalid bundle or not a bundle at all.
                if (logger.isDebugEnabled()) {
                    logger.debug("File \"" + file.getName() + "\" in subsystem with location \"" + location + "\" will be ignored because it is not recognized as a supported resource", e);
                }
            }
        }
    }

    private List<Capability> computeFileCapabilities(FileResource resource, IFile file, SubsystemManifest manifest) {
        SubsystemContentHeader ssch = manifest.getSubsystemContentHeader();
        if (ssch == null)
            return Collections.emptyList();

        for (Clause c : ssch.getClauses()) {
            Attribute er = c.getAttribute(ContentHandler.EMBEDDED_RESOURCE_ATTRIBUTE);
            if (er != null) {
                if (file.getName().equals(er.getValue())) {
                    Map<String, Object> attrs = new HashMap<String, Object>();
                    attrs.put(ContentHandler.EMBEDDED_RESOURCE_ATTRIBUTE, er.getValue());
                    return Collections.<Capability> singletonList(
                            new OsgiIdentityCapability(resource, c.getSymbolicName(), c.getVersionRange().getLeft(), c.getType(), attrs));
                }
            }
        }
        return Collections.emptyList();
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
			clauses.add(SubsystemImportServiceHeader.Clause.valueOf(requirement));
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

	private SubsystemManifest computeSubsystemManifestBeforeRequirements(IDirectory content, SubsystemManifest manifest) throws MalformedURLException {
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder().manifest(manifest);
		addSubsystemSymbolicNameHeader(builder, manifest);
		addSubsystemVersionHeader(builder, manifest);
		addSubsystemTypeHeader(builder, manifest);
		addSubsystemContentHeader(builder, manifest);
		builder.header(new GenericHeader(Constants.AriesSubsystemOriginalContent, String.valueOf(content.toURL())));
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
	
	private SubsystemTypeHeader computeSubsystemTypeHeader(SubsystemManifest manifest) {
		SubsystemTypeHeader header = manifest.getSubsystemTypeHeader();
		AriesProvisionDependenciesDirective directive = header.getAriesProvisionDependenciesDirective();
		if (directive != null) {
			// Nothing to do because the directive was specified in the original 
			// manifest. Validation of the value occurs later.
			return header;
		}
		// The directive was not specified in the original manifest. The value 
		// of the parent directive becomes the default.
		SubsystemManifest parentManifest = ((BasicSubsystem)parentSubsystem).getSubsystemManifest();
		SubsystemTypeHeader parentHeader = parentManifest.getSubsystemTypeHeader();
		directive = parentHeader.getAriesProvisionDependenciesDirective();
		header = new SubsystemTypeHeader(header.getValue() + ';' + directive);
		return header;
	}

	private SubsystemVersionHeader computeSubsystemVersionHeader(SubsystemManifest manifest) {
		SubsystemVersionHeader header = manifest.getSubsystemVersionHeader();
		if (header.getVersion().equals(Version.emptyVersion) && location.getVersion() != null)
			header = new SubsystemVersionHeader(location.getVersion());
		return header;
	}

	private Collection<TranslationFile> computeTranslations(IDirectory directory) throws IOException {
		SubsystemManifest manifest = getSubsystemManifest();
		SubsystemLocalizationHeader header = manifest.getSubsystemLocalizationHeader();
		String directoryName = header.getDirectoryName();
		// TODO Assumes the ZIP file includes directory entries. Issues?
		IFile file = directoryName == null ? directory : directory.getFile(directoryName);
		if (file == null || !file.isDirectory())
			return Collections.emptyList();
		List<IFile> files = file.convert().listFiles();
		if (files == null || files.isEmpty())
			return Collections.emptyList();
		ArrayList<TranslationFile> result = new ArrayList<TranslationFile>(files.size());
		for (IFile f : files) {
			Properties properties = new Properties();
			InputStream is = f.open();
			try {
				properties.load(is);
				result.add(new TranslationFile(f.getName(), properties));
			}
			finally {
				is.close();
			}
		}
		result.trimToSize();
		return result;
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
							+ SubsystemTypeHeader.PROVISION_POLICY_ACCEPT_DEPENDENCIES
							+ ';'
							+ AriesProvisionDependenciesDirective.INSTALL.toString())
					.build();
	}

	private boolean isComposite(SubsystemManifest manifest) {
		return SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(manifest.getSubsystemTypeHeader().getType());
	}
	
	Collection<Resource> getResources() {
		return resources;
	}
}

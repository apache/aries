package org.apache.aries.subsystem.core.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.internal.Activator;
import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.apache.aries.subsystem.core.internal.OsgiIdentityRequirement;
import org.apache.aries.subsystem.core.internal.SubsystemEnvironment;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class DeploymentManifest {
	public static class Builder {
		private Map<String, Header<?>> headers = new HashMap<String, Header<?>>();
		
		public DeploymentManifest build() {
			return new DeploymentManifest(headers);
		}
		
		public Builder header(Header<?> value) {
			if (value != null)
				headers.put(value.getName(), value);
			return this;
		}
		
		public Builder manifest(SubsystemManifest value) {
			for (Entry<String, Header<?>> entry : value.getHeaders().entrySet())
				header(entry.getValue());
			return this;
		}
	}
	
	public static final String DEPLOYED_CONTENT = SubsystemConstants.DEPLOYED_CONTENT;
	public static final String DEPLOYMENT_MANIFESTVERSION = SubsystemConstants.DEPLOYMENT_MANIFESTVERSION;
	public static final String EXPORT_PACKAGE = Constants.EXPORT_PACKAGE;
	public static final String IMPORT_PACKAGE = Constants.IMPORT_PACKAGE;
	public static final String PROVIDE_CAPABILITY = Constants.PROVIDE_CAPABILITY;
	public static final String PROVISION_RESOURCE = SubsystemConstants.PROVISION_RESOURCE;
	public static final String REQUIRE_BUNDLE = Constants.REQUIRE_BUNDLE;
	public static final String REQUIRE_CAPABILITY = Constants.REQUIRE_CAPABILITY;
	public static final String SUBSYSTEM_EXPORTSERVICE = SubsystemConstants.SUBSYSTEM_EXPORTSERVICE;
	public static final String SUBSYSTEM_IMPORTSERVICE = SubsystemConstants.SUBSYSTEM_IMPORTSERVICE;
	public static final String SUBSYSTEM_SYMBOLICNAME = SubsystemConstants.SUBSYSTEM_SYMBOLICNAME;
	public static final String SUBSYSTEM_VERSION = SubsystemConstants.SUBSYSTEM_VERSION;
	
	public static final String ARIESSUBSYSTEM_AUTOSTART = "AriesSubsystem-Autostart";
	public static final String ARIESSUBSYSTEM_ID = "AriesSubsystem-Id";
	public static final String ARIESSUBSYSTEM_LASTID = "AriesSubsystem-LastId";
	public static final String ARIESSUBSYSTEM_LOCATION = "AriesSubsystem-Location";
	
	private final Map<String, Header<?>> headers;
	
	public DeploymentManifest(java.util.jar.Manifest manifest) {
		headers = new HashMap<String, Header<?>>();
		for (Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
			String key = String.valueOf(entry.getKey());
			if (key.equals(SubsystemManifest.SUBSYSTEM_SYMBOLICNAME))
				continue;
			headers.put(key, HeaderFactory.createHeader(key, String.valueOf(entry.getValue())));
		}
	}
	
	public DeploymentManifest(File file) throws FileNotFoundException, IOException {
		Manifest manifest = ManifestProcessor.parseManifest(new FileInputStream(file));
		Attributes attributes = manifest.getMainAttributes();
		Map<String, Header<?>> headers = new HashMap<String, Header<?>>(attributes.size() + 4); // Plus the # of potentially derived headers.
		for (Entry<Object, Object> entry : attributes.entrySet()) {
			String key = String.valueOf(entry.getKey());
			headers.put(key, HeaderFactory.createHeader(key, String.valueOf(entry.getValue())));
		}
		this.headers = Collections.unmodifiableMap(headers);
	}
	
	public DeploymentManifest(
			DeploymentManifest deploymentManifest, 
			SubsystemManifest subsystemManifest, 
			SubsystemEnvironment environment,
			boolean autostart, 
			long id, 
			long lastId, 
			String location,
			boolean overwrite,
			boolean acceptDependencies) {
		Map<String, Header<?>> headers;
		if (deploymentManifest == null // We're generating a new deployment manifest.
				|| (deploymentManifest != null && overwrite)) { // A deployment manifest already exists but overwriting it with subsystem manifest content is desired.
			headers = new HashMap<String, Header<?>>();
			Collection<Resource> resources = new HashSet<Resource>();
			SubsystemContentHeader contentHeader = subsystemManifest.getSubsystemContentHeader();
			Map<Resource, List<Wire>> resolution = null;
			Collection<Resource> deployedContent = new HashSet<Resource>();
			if (contentHeader != null) {
				for (SubsystemContentHeader.Content content : contentHeader.getContents()) {
					OsgiIdentityRequirement requirement = new OsgiIdentityRequirement(content.getName(), content.getVersionRange(), content.getType(), false);
					Resource resource = environment.findResource(requirement);
					// If the resource is null, can't continue.
					if (resource == null) {
						if (content.isMandatory())
							throw new SubsystemException("Resource does not exist: " + requirement);
						continue;
					}
					resources.add(resource);
				}
				// TODO This does not validate that all content bundles were found.
				resolution = Activator.getInstance().getResolver().resolve(environment, new ArrayList<Resource>(resources), Collections.EMPTY_LIST);
				Collection<Resource> provisionResource = new HashSet<Resource>();
				for (Resource resource : resolution.keySet()) {
					if (contentHeader.contains(resource))
						deployedContent.add(resource);
					else
						provisionResource.add(resource);
				}
				// Make sure any already resolved content resources are added back in.
				deployedContent.addAll(resources);
				headers.put(DEPLOYED_CONTENT, DeployedContentHeader.newInstance(deployedContent));
				if (!provisionResource.isEmpty())
					headers.put(PROVISION_RESOURCE, ProvisionResourceHeader.newInstance(provisionResource));
			}
			headers.put(SUBSYSTEM_SYMBOLICNAME, subsystemManifest.getSubsystemSymbolicNameHeader());
			headers.put(SUBSYSTEM_VERSION, subsystemManifest.getSubsystemVersionHeader());
			SubsystemTypeHeader typeHeader = subsystemManifest.getSubsystemTypeHeader();
			if (typeHeader.isApplication()) {
				if (resolution != null) {
					Header<?> header = computeImportPackageHeader(resolution, deployedContent, acceptDependencies);
					if (header != null)
						headers.put(IMPORT_PACKAGE, header);
					header = computeRequireCapabilityHeader(resolution, deployedContent, acceptDependencies);
					if (header != null)
						headers.put(REQUIRE_CAPABILITY, header);
					header = computeRequireBundleHeader(resolution, deployedContent, acceptDependencies);
					if (header != null)
						headers.put(REQUIRE_BUNDLE, header);
				}
				// TODO Compute additional headers for an application.
			}
			else if (typeHeader.isComposite()) {
				Header<?> header = subsystemManifest.getImportPackageHeader();
				if (header != null)
					headers.put(IMPORT_PACKAGE, header);
				header = subsystemManifest.getRequireCapabilityHeader();
				if (header != null)
					headers.put(REQUIRE_CAPABILITY, header);
				header = subsystemManifest.getSubsystemImportServiceHeader();
				if (header != null)
					headers.put(SUBSYSTEM_IMPORTSERVICE, header);
				header = subsystemManifest.getRequireBundleHeader();
				if (header != null)
					headers.put(REQUIRE_BUNDLE, header);
				header = subsystemManifest.getExportPackageHeader();
				if (header != null)
					headers.put(EXPORT_PACKAGE, header);
				header = subsystemManifest.getProvideCapabilityHeader();
				if (header != null)
					headers.put(PROVIDE_CAPABILITY, header);
				header = subsystemManifest.getSubsystemExportServiceHeader();
				if (header != null)
					headers.put(SUBSYSTEM_EXPORTSERVICE, header);
				// TODO Compute additional headers for a composite. 
			}
			// Features require no additional headers.
		}
		else {
			headers = new HashMap<String, Header<?>>(deploymentManifest.getHeaders());
		}
		// TODO DEPLOYMENT_MANIFESTVERSION
		headers.put(ARIESSUBSYSTEM_AUTOSTART, new GenericHeader(ARIESSUBSYSTEM_AUTOSTART, Boolean.toString(autostart)));
		headers.put(ARIESSUBSYSTEM_ID, new GenericHeader(ARIESSUBSYSTEM_ID, Long.toString(id)));
		headers.put(ARIESSUBSYSTEM_LOCATION, new GenericHeader(ARIESSUBSYSTEM_LOCATION, location));
		headers.put(ARIESSUBSYSTEM_LASTID, new GenericHeader(ARIESSUBSYSTEM_LASTID, Long.toString(lastId)));
		this.headers = Collections.unmodifiableMap(headers);
	}
	
	private DeploymentManifest(Map<String, Header<?>> headers) {
		Map<String, Header<?>> map = new HashMap<String, Header<?>>(headers);
		this.headers = Collections.unmodifiableMap(map);
	}
	
	public DeployedContentHeader getDeployedContentHeader() {
		return (DeployedContentHeader)getHeaders().get(DEPLOYED_CONTENT);
	}
	
	public ExportPackageHeader getExportPackageHeader() {
		return (ExportPackageHeader)getHeaders().get(EXPORT_PACKAGE);
	}
	
	public Map<String, Header<?>> getHeaders() {
		return headers;
	}
	
	public ImportPackageHeader getImportPackageHeader() {
		return (ImportPackageHeader)getHeaders().get(IMPORT_PACKAGE);
	}
	
	public ProvideCapabilityHeader getProvideCapabilityHeader() {
		return (ProvideCapabilityHeader)getHeaders().get(PROVIDE_CAPABILITY);
	}
	
	public ProvisionResourceHeader getProvisionResourceHeader() {
		return (ProvisionResourceHeader)getHeaders().get(PROVISION_RESOURCE);
	}
	
	public RequireBundleHeader getRequireBundleHeader() {
		return (RequireBundleHeader)getHeaders().get(REQUIRE_BUNDLE);
	}
	
	public RequireCapabilityHeader getRequireCapabilityHeader() {
		return (RequireCapabilityHeader)getHeaders().get(REQUIRE_CAPABILITY);
	}
	
	public SubsystemExportServiceHeader getSubsystemExportServiceHeader() {
		return (SubsystemExportServiceHeader)getHeaders().get(SUBSYSTEM_EXPORTSERVICE);
	}
	
	public SubsystemImportServiceHeader getSubsystemImportServiceHeader() {
		return (SubsystemImportServiceHeader)getHeaders().get(SUBSYSTEM_IMPORTSERVICE);
	}
	
	public void write(OutputStream out) throws IOException {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		// The manifest won't write anything unless the following header is present.
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		for (Entry<String, Header<?>> entry : headers.entrySet()) {
			attributes.putValue(entry.getKey(), entry.getValue().getValue());
		}
		manifest.write(out);
	}
	
	private static ImportPackageHeader computeImportPackageHeader(
			Map<Resource, List<Wire>> resolution, 
			Collection<Resource> content,
			boolean acceptDependencies) {
		Collection<ImportPackageHeader.Clause> clauses = new ArrayList<ImportPackageHeader.Clause>();
		for (Entry<Resource, List<Wire>> entry : resolution.entrySet()) {
			for (Wire wire : entry.getValue()) {
				Resource provider = wire.getProvider();
				if (content.contains(provider))
					// If the provider is a content resource, we don't need an import.
					continue;
				// The provider is a dependency that is already provisioned or needs provisioning.
				if (acceptDependencies && !((provider instanceof BundleRevision) || (provider instanceof AriesSubsystem)))
					// If the application accepts dependencies and the provider is a dependency that needs provisioning,
					// we don't need an import.
					continue;
				// For all other cases, we need an import.
				Requirement requirement = wire.getRequirement();
				if (PackageNamespace.PACKAGE_NAMESPACE.equals(requirement.getNamespace())) {
					clauses.add(new ImportPackageHeader.Clause(requirement));
				}
			}
		}
		if (clauses.isEmpty())
			return null;
		return new ImportPackageHeader(clauses);
	}
	
	private static RequireBundleHeader computeRequireBundleHeader(
			Map<Resource, List<Wire>> resolution, 
			Collection<Resource> content,
			boolean acceptDependencies) {
		Collection<RequireBundleHeader.Clause> clauses = new ArrayList<RequireBundleHeader.Clause>();
		for (Entry<Resource, List<Wire>> entry : resolution.entrySet()) {
			for (Wire wire : entry.getValue()) {
				Resource provider = wire.getProvider();
				if (content.contains(provider))
					// If the provider is a content resource, we don't need an import.
					continue;
				// The provider is a dependency that is already provisioned or needs provisioning.
				if (acceptDependencies && !((provider instanceof BundleRevision) || (provider instanceof AriesSubsystem)))
					// If the application accepts dependencies and the provider is a dependency that needs provisioning,
					// we don't need an import.
					continue;
				// For all other cases, we need an import.
				Requirement requirement = wire.getRequirement();
				if (BundleNamespace.BUNDLE_NAMESPACE.equals(requirement.getNamespace())) {
					clauses.add(new RequireBundleHeader.Clause(requirement));
				}
			}
		}
		if (clauses.isEmpty())
			return null;
		return new RequireBundleHeader(clauses);
	}
	
	private static RequireCapabilityHeader computeRequireCapabilityHeader(
			Map<Resource, List<Wire>> resolution, 
			Collection<Resource> content,
			boolean acceptDependencies) {
		Collection<RequireCapabilityHeader.Clause> clauses = new ArrayList<RequireCapabilityHeader.Clause>();
		for (Entry<Resource, List<Wire>> entry : resolution.entrySet()) {
			for (Wire wire : entry.getValue()) {
				Resource provider = wire.getProvider();
				if (content.contains(provider))
					// If the provider is a content resource, we don't need an imported capability.
					continue;
				// The provider is a dependency that is already provisioned or needs provisioning.
				if (acceptDependencies && !((provider instanceof BundleRevision) || (provider instanceof AriesSubsystem)))
					// If the application accepts dependencies and the provider is a dependency that needs provisioning,
					// we don't need an import.
					continue;
				// For all other cases, we need an import.
				Requirement requirement = wire.getRequirement();
				// TODO Not sure if the startsWith check will be sufficient.
				if (!requirement.getNamespace().startsWith("osgi.")) {
					clauses.add(new RequireCapabilityHeader.Clause(requirement));
				}
			}
		}
		if (clauses.isEmpty())
			return null;
		return new RequireCapabilityHeader(clauses);
	}
}

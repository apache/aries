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
import org.apache.aries.subsystem.core.obr.SubsystemEnvironment;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class DeploymentManifest {
	public static final String DEPLOYED_CONTENT = SubsystemConstants.DEPLOYED_CONTENT;
	public static final String DEPLOYMENT_MANIFESTVERSION = "Deployment-ManifestVersion"; // TODO Needs constant on SubsystemConstants.
	public static final String EXPORT_PACKAGE = Constants.EXPORT_PACKAGE;
	public static final String IMPORT_PACKAGE = Constants.IMPORT_PACKAGE;
	public static final String PROVISION_RESOURCE = SubsystemConstants.PROVISION_RESOURCE;
	public static final String REQUIRE_BUNDLE = Constants.REQUIRE_BUNDLE;
	public static final String REQUIRE_CAPABILITY = Constants.REQUIRE_CAPABILITY;
	public static final String SUBSYSTEM_IMPORTSERVICE = SubsystemConstants.SUBSYSTEM_IMPORTSERVICE;
	public static final String SUBSYSTEM_SYMBOLICNAME = SubsystemConstants.SUBSYSTEM_SYMBOLICNAME;
	public static final String SUBSYSTEM_VERSION = SubsystemConstants.SUBSYSTEM_VERSION;
	
	public static final String ARIESSUBSYSTEM_AUTOSTART = "AriesSubsystem-Autostart";
	public static final String ARIESSUBSYSTEM_ID = "AriesSubsystem-Id";
	public static final String ARIESSUBSYSTEM_LASTID = "AriesSubsystem-LastId";
	public static final String ARIESSUBSYSTEM_LOCATION = "AriesSubsystem-Location";
	
	private final Map<String, Header<?>> headers;
	
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
				// TODO Once we have a resolver that actually returns lists of wires, we can use them to compute other manifest headers such as Import-Package.
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
			if (SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(typeHeader.getValue())) {
				if (resolution != null) {
					Header<?> header = computeImportPackageHeader(resolution, deployedContent, acceptDependencies);
					if (header != null)
						headers.put(IMPORT_PACKAGE, header);
				}
				// TODO Compute additional headers for an application.
			}
			else if (SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(typeHeader.getValue())) {
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
	
	public ProvisionResourceHeader getProvisionResourceHeader() {
		return (ProvisionResourceHeader)getHeaders().get(PROVISION_RESOURCE);
	}
	
	public RequireBundleHeader getRequireBundleHeader() {
		return (RequireBundleHeader)getHeaders().get(REQUIRE_BUNDLE);
	}
	
	public RequireCapabilityHeader getRequireCapabilityHeader() {
		return (RequireCapabilityHeader)getHeaders().get(REQUIRE_CAPABILITY);
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
}

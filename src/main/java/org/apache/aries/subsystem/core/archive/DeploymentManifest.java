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
package org.apache.aries.subsystem.core.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.internal.BasicSubsystem;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Constants;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class DeploymentManifest {
	public static class Builder {
		private Map<String, Header<?>> headers = new HashMap<String, Header<?>>();
		
		public DeploymentManifest build() {
			return new DeploymentManifest(headers);
		}
		
		public Builder autostart(boolean value) {
			header(new GenericHeader(ARIESSUBSYSTEM_AUTOSTART, Boolean.toString(value)));
			return this;
		}
		
//		public Builder content(Resource resource) {
//			return content(resource, true);
//		}
		
		public Builder content(Resource resource, boolean referenced) {
			DeployedContentHeader header = (DeployedContentHeader)headers.get(DeploymentManifest.DEPLOYED_CONTENT);
			if (header == null) {
				DeployedContentHeader.Clause clause = new DeployedContentHeader.Clause(resource, referenced);
				header(new DeployedContentHeader(Collections.singleton(clause)));
				return this;
			}
			DeployedContentHeader.Clause clause = header.getClause(resource);
			if (clause == null) {
				clause = new DeployedContentHeader.Clause(resource, referenced);
				List<DeployedContentHeader.Clause> clauses = new ArrayList<DeployedContentHeader.Clause>(header.getClauses().size() + 1);
				clauses.addAll(header.getClauses());
				clauses.add(clause);
				header(new DeployedContentHeader(clauses));
				return this;
			}
			
			Collection<DeployedContentHeader.Clause> clauses = new ArrayList<DeployedContentHeader.Clause>(header.getClauses());
			for (Iterator<DeployedContentHeader.Clause> i = clauses.iterator(); i.hasNext();)
				if (clause.equals(i.next())) {
					i.remove();
					break;
				}
			clauses.add(new DeployedContentHeader.Clause(resource, referenced));
			header(new DeployedContentHeader(clauses));
			return this;
		}
		
		public Builder header(Header<?> value) {
			if (value != null)
				headers.put(value.getName(), value);
			return this;
		}
		
		public Builder id(long value) {
			header(new GenericHeader(ARIESSUBSYSTEM_ID, Long.toString(value)));
			return this;
		}
		
		public Builder lastId(long value) {
			header(new GenericHeader(ARIESSUBSYSTEM_LASTID, Long.toString(value)));
			return this;
		}
		
		public Builder location(String value) {
			if (value != null)
				header(new GenericHeader(ARIESSUBSYSTEM_LOCATION, value));
			return this;
		}
		
		public Builder manifest(DeploymentManifest value) {
			if (value != null)
				for (Entry<String, Header<?>> entry : value.getHeaders().entrySet())
					header(entry.getValue());
			return this;
		}
		
		public Builder manifest(SubsystemManifest value) {
			if (value != null)
				for (Entry<String, Header<?>> entry : value.getHeaders().entrySet())
					header(entry.getValue());
			return this;
		}
		
		public Builder parent(BasicSubsystem value, boolean referenceCount) {
			AriesSubsystemParentsHeader.Clause clause = new AriesSubsystemParentsHeader.Clause(value, referenceCount);
			AriesSubsystemParentsHeader header = (AriesSubsystemParentsHeader)headers.get(ARIESSUBSYSTEM_PARENTS);
			if (header == null)
				header(new AriesSubsystemParentsHeader(Collections.singleton(clause)));
			else {
				Collection<AriesSubsystemParentsHeader.Clause> clauses = new ArrayList<AriesSubsystemParentsHeader.Clause>(header.getClauses().size() + 1);
				clauses.addAll(header.getClauses());
				clauses.add(clause);
				header(new AriesSubsystemParentsHeader(clauses));
			}
			return this;
		}
		
		public Builder region(String value) {
			if (value != null)
				header(new GenericHeader(ARIESSUBSYSTEM_REGION, value));
			return this;
		}
		
		public Builder region(org.eclipse.equinox.region.Region value) {
			if (value != null)
				region(value.getName());
			return this;
		}
		
		public Builder state(Subsystem.State value) {
			if (value != null)
				header(new GenericHeader(ARIESSUBSYSTEM_STATE, value.toString()));
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
	public static final String ARIESSUBSYSTEM_PARENTS = "AriesSubsystem-Parents";
	public static final String ARIESSUBSYSTEM_REGION = "AriesSubsystem-Region";
	public static final String ARIESSUBSYSTEM_STATE = "AriesSubsystem-State";
	
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
	
	public DeploymentManifest(File file) throws IOException {
		this(new FileInputStream(file));
	}
	
	public DeploymentManifest(InputStream in) throws IOException {
		Manifest manifest = ManifestProcessor.parseManifest(in);
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
			boolean autostart, 
			long id, 
			long lastId, 
			String location,
			boolean overwrite,
			boolean acceptDependencies) throws ResolutionException, IOException, URISyntaxException {
		Map<String, Header<?>> headers;
		if (deploymentManifest == null // We're generating a new deployment manifest.
				|| (deploymentManifest != null && overwrite)) { // A deployment manifest already exists but overwriting it with subsystem manifest content is desired.
			headers = computeHeaders(subsystemManifest);
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
	
	public AriesSubsystemParentsHeader getAriesSubsystemParentsHeader() {
		return (AriesSubsystemParentsHeader)getHeaders().get(ARIESSUBSYSTEM_PARENTS);
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
	
	private Map<String, Header<?>> computeHeaders(SubsystemManifest manifest) {
		return new HashMap<String, Header<?>>(manifest.getHeaders());
	}

    @Override
    public int hashCode() {
    	return 31 * 17 + headers.hashCode();
    }

    @Override
    public boolean equals(Object o) {
    	if (o == this) {
    		return true;
    	}
    	if (!(o instanceof SubsystemManifest)) {
    		return false;
    	}
    	DeploymentManifest that = (DeploymentManifest)o;
    	return that.headers.equals(this.headers);
    }
}

package org.apache.aries.subsystem.core.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemManifest {
	public static final String EXPORT_PACKAGE = Constants.EXPORT_PACKAGE;
	public static final String IMPORT_PACKAGE = Constants.IMPORT_PACKAGE;
	public static final String PREFERRED_PROVIDER = SubsystemConstants.PREFERRED_PROVIDER;
	public static final String PROVIDE_CAPABILITY = Constants.PROVIDE_CAPABILITY;
	public static final String REQUIRE_BUNDLE = Constants.REQUIRE_BUNDLE;
	public static final String REQUIRE_CAPABILITY = Constants.REQUIRE_CAPABILITY;
	public static final String SUBSYSTEM_CONTENT = SubsystemConstants.SUBSYSTEM_CONTENT;
	public static final String SUBSYSTEM_DESCRIPTION = SubsystemConstants.SUBSYSTEM_DESCRIPTION;
	public static final String SUBSYSTEM_EXPORTSERVICE = SubsystemConstants.SUBSYSTEM_EXPORTSERVICE;
	public static final String SUBSYSTEM_IMPORTSERVICE = SubsystemConstants.SUBSYSTEM_IMPORTSERVICE;
	public static final String SUBSYSTEM_MANIFESTVERSION = SubsystemConstants.SUBSYSTEM_MANIFESTVERSION;
	public static final String SUBSYSTEM_NAME = SubsystemConstants.SUBSYSTEM_NAME;
	public static final String SUBSYSTEM_SYMBOLICNAME = SubsystemConstants.SUBSYSTEM_SYMBOLICNAME;
	public static final String SUBSYSTEM_TYPE = SubsystemConstants.SUBSYSTEM_TYPE;
	public static final String SUBSYSTEM_VERSION = SubsystemConstants.SUBSYSTEM_VERSION;
	
	private final Map<String, Header<?>> headers;
	
	public SubsystemManifest(File file) throws FileNotFoundException, IOException {
		Manifest manifest = ManifestProcessor.parseManifest(new FileInputStream(file));
		Attributes attributes = manifest.getMainAttributes();
		Map<String, Header<?>> headers = new HashMap<String, Header<?>>(attributes.size() + 4); // Plus the # of potentially derived headers.
		for (Entry<Object, Object> entry : attributes.entrySet()) {
			String key = String.valueOf(entry.getKey());
			headers.put(key, HeaderFactory.createHeader(key, String.valueOf(entry.getValue())));
		}
		Header<?> header = headers.get(SUBSYSTEM_VERSION);
		if (header == null) {
			headers.put(SUBSYSTEM_VERSION, SubsystemVersionHeader.DEFAULT);
		}
		header = headers.get(SUBSYSTEM_TYPE);
		if (header == null)
			headers.put(SUBSYSTEM_TYPE, SubsystemTypeHeader.DEFAULT);
		this.headers = Collections.unmodifiableMap(headers);
	}
	
	public SubsystemManifest(String symbolicName, Version version, Collection<Resource> content) {
		this(null, symbolicName, version, content);
	}
	
	public SubsystemManifest(SubsystemManifest manifest, String symbolicName, Version version, Collection<Resource> content) {
		Map<String, Header<?>> headers;
		if (manifest == null) {
			headers = new HashMap<String, Header<?>>(4);
		}
		else {
			headers = new HashMap<String, Header<?>>(manifest.headers);
		}
		Header<?> header = headers.get(SUBSYSTEM_SYMBOLICNAME);
		if (header == null)
			headers.put(SUBSYSTEM_SYMBOLICNAME, new SubsystemSymbolicNameHeader(symbolicName));
		header = headers.get(SUBSYSTEM_VERSION);
		if (header == null) {
			if (version == null)
				headers.put(SUBSYSTEM_VERSION, SubsystemVersionHeader.DEFAULT);
			else
				headers.put(SUBSYSTEM_VERSION, new SubsystemVersionHeader(version));
		}
		header = headers.get(SUBSYSTEM_CONTENT);
		if (header == null && content != null && !content.isEmpty()) {
			// TODO Better way than using StringBuilder? Would require a more robust SubsystemContentHeader in order to fulfill the Header contract.
			StringBuilder sb = new StringBuilder();
			for (Resource resource : content) {
				Capability c = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
				Map<String, Object> a = c.getAttributes();
				String s = (String)a.get(IdentityNamespace.IDENTITY_NAMESPACE);
				Version v = (Version)a.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				String t = (String)a.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
				sb.append(s).append(';')
					.append(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE).append('=').append(v).append(';')
					.append(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE).append('=').append(t).append(',');
			}
			if (sb.length() != 0) {
				// Remove the trailing comma.
				sb.deleteCharAt(sb.length() - 1);
				headers.put(SubsystemContentHeader.NAME, new SubsystemContentHeader(sb.toString()));
			}
		}
		header = headers.get(SUBSYSTEM_TYPE);
		if (header == null)
			headers.put(SUBSYSTEM_TYPE, SubsystemTypeHeader.DEFAULT);
		this.headers = Collections.unmodifiableMap(headers);
	}
	
	public Map<String, Header<?>> getHeaders() {
		return headers;
	}
	
	public ImportPackageHeader getImportPackageHeader() {
		return (ImportPackageHeader)getHeaders().get(IMPORT_PACKAGE);
	}
	
	public RequireBundleHeader getRequireBundleHeader() {
		return (RequireBundleHeader)getHeaders().get(REQUIRE_BUNDLE);
	}
	
	public RequireCapabilityHeader getRequireCapabilityHeader() {
		return (RequireCapabilityHeader)getHeaders().get(REQUIRE_CAPABILITY);
	}
	
	public SubsystemContentHeader getSubsystemContentHeader() {
		return (SubsystemContentHeader)getHeaders().get(SUBSYSTEM_CONTENT);
	}
	
	public SubsystemImportServiceHeader getSubsystemImportServiceHeader() {
		return (SubsystemImportServiceHeader)getHeaders().get(SUBSYSTEM_IMPORTSERVICE);
	}
	
	public SubsystemSymbolicNameHeader getSubsystemSymbolicNameHeader() {
		return (SubsystemSymbolicNameHeader)getHeaders().get(SUBSYSTEM_SYMBOLICNAME);
	}
	
	public SubsystemTypeHeader getSubsystemTypeHeader() {
		return (SubsystemTypeHeader)getHeaders().get(SUBSYSTEM_TYPE);
	}
	
	public SubsystemVersionHeader getSubsystemVersionHeader() {
		return (SubsystemVersionHeader)getHeaders().get(SUBSYSTEM_VERSION);
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
}

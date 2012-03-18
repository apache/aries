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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemManifest {
	public static class Builder {
		private Map<String, Header<?>> headers = new HashMap<String, Header<?>>();
		
		public Builder(String symbolicName) {
			headers.put(SUBSYSTEM_SYMBOLICNAME, new SubsystemSymbolicNameHeader(symbolicName));
		}
		
		public SubsystemManifest build() {
			return new SubsystemManifest(headers);
		}
		
		public Builder content(String value) {
			return value == null ? this : content(new SubsystemContentHeader(value));
		}
		
		public Builder content(Collection<Resource> value) {
			return value == null || value.isEmpty() ? this : content(new SubsystemContentHeader(value));
		}
		
		public Builder content(SubsystemContentHeader value) {
			return header(value);
		}
		
		public Builder header(Header<?> value) {
			if (value != null)
				headers.put(value.getName(), value);
			return this;
		}
		
		public Builder type(String value) {
			return value == null ? this : type(new SubsystemTypeHeader(value));
		}
		
		public Builder type(SubsystemTypeHeader value) {
			return header(value);
		}
		
		public Builder version(String value) {
			return value == null ? this : version(Version.parseVersion(value));
		}
		
		public Builder version(Version value) {
			return value == null ? this : version(new SubsystemVersionHeader(value));
		}
		
		public Builder version(SubsystemVersionHeader value) {
			return header(value);
		}
	}
	
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
	
	private static void fillInDefaults(Map<String, Header<?>> headers) {
		Header<?> header = headers.get(SUBSYSTEM_VERSION);
		if (header == null) {
			headers.put(SUBSYSTEM_VERSION, SubsystemVersionHeader.DEFAULT);
		}
		header = headers.get(SUBSYSTEM_TYPE);
		if (header == null)
			headers.put(SUBSYSTEM_TYPE, SubsystemTypeHeader.DEFAULT);
	}
	
	private final Map<String, Header<?>> headers;
	
	private SubsystemManifest(Map<String, Header<?>> headers) {
		Map<String, Header<?>> map = new HashMap<String, Header<?>>(headers);
		fillInDefaults(map);
		this.headers = Collections.unmodifiableMap(map);
	}
	
	public SubsystemManifest(File file) throws FileNotFoundException, IOException {
		Manifest manifest = ManifestProcessor.parseManifest(new FileInputStream(file));
		Attributes attributes = manifest.getMainAttributes();
		Map<String, Header<?>> headers = new HashMap<String, Header<?>>(attributes.size() + 4); // Plus the # of potentially derived headers.
		for (Entry<Object, Object> entry : attributes.entrySet()) {
			String key = String.valueOf(entry.getKey());
			headers.put(key, HeaderFactory.createHeader(key, String.valueOf(entry.getValue())));
		}
		fillInDefaults(headers);
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
		if (header == null && !(version == null)) {
			headers.put(SUBSYSTEM_VERSION, new SubsystemVersionHeader(version));
		}
		header = headers.get(SUBSYSTEM_CONTENT);
		if (header == null && content != null && !content.isEmpty()) {
			headers.put(SubsystemContentHeader.NAME, new SubsystemContentHeader(content));
		}
		fillInDefaults(headers);
		this.headers = Collections.unmodifiableMap(headers);
	}
	
	public Map<String, Header<?>> getHeaders() {
		return headers;
	}
	
	public ExportPackageHeader getExportPackageHeader() {
		return (ExportPackageHeader)getHeaders().get(EXPORT_PACKAGE);
	}
	
	public ImportPackageHeader getImportPackageHeader() {
		return (ImportPackageHeader)getHeaders().get(IMPORT_PACKAGE);
	}
	
	public ProvideCapabilityHeader getProvideCapabilityHeader() {
		return (ProvideCapabilityHeader)getHeaders().get(PROVIDE_CAPABILITY);
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
	
	public SubsystemExportServiceHeader getSubsystemExportServiceHeader() {
		return (SubsystemExportServiceHeader)getHeaders().get(SUBSYSTEM_EXPORTSERVICE);
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
	
	public List<Capability> toCapabilities(Resource resource) {
		ArrayList<Capability> requirements = new ArrayList<Capability>();
		for (Header<?> header : headers.values())
			if (header instanceof CapabilityHeader)
				requirements.addAll(((CapabilityHeader<?>)header).toCapabilities(resource));
		requirements.trimToSize();
		return requirements;
	}
	
	public List<Requirement> toRequirements(Resource resource) {
		ArrayList<Requirement> requirements = new ArrayList<Requirement>();
		for (Header<?> header : headers.values())
			if (header instanceof RequirementHeader)
				requirements.addAll(((RequirementHeader<?>)header).toRequirements(resource));
		requirements.trimToSize();
		return requirements;
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

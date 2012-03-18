package org.apache.aries.subsystem.core.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.HeaderFactory;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.archive.SubsystemVersionHeader;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.subsystem.core.internal.SubsystemUri;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemStreamResource implements Resource, RepositoryContent {
	private final byte[] content;
	private final List<Capability> capabilities;
	private final ICloseableDirectory directory;
	private final List<Requirement> requirements;
	
	public SubsystemStreamResource(String location, InputStream content) throws IOException, URISyntaxException {
		SubsystemUri uri = null;
		try {
			if (location.startsWith("subsystem://"))
				uri = new SubsystemUri(location);
			if (content == null) {
				if (uri != null)
					content = uri.getURL().openStream();
				else
					content = new URL(location).openStream();
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] bytes = new byte[2048];
			int read;
			while ((read = content.read(bytes)) != -1) {
				baos.write(bytes, 0, read);
			}
			this.content = baos.toByteArray();
			directory = FileSystem.getFSRoot(new ByteArrayInputStream(baos.toByteArray()));
			if (directory == null)
				throw new IOException("Unable to parse content of " + location);
		}
		finally {
			IOUtils.close(content);
		}
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/SUBSYSTEM.MF");
		Attributes attributes = manifest.getMainAttributes();
		String symbolicName = attributes.getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
		if (symbolicName == null) {
			if (uri == null)
				throw new IllegalArgumentException("No symbolic name");
			symbolicName = uri.getSymbolicName();
		}
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder(symbolicName);
		for (Entry<Object, Object> entry : attributes.entrySet()) {
			String key = String.valueOf(entry.getKey());
			if (key.equals(SubsystemManifest.SUBSYSTEM_SYMBOLICNAME))
				continue;
			builder.header(HeaderFactory.createHeader(key, String.valueOf(entry.getValue())));
		}
		SubsystemManifest subsystemManifest = builder.build();
		SubsystemVersionHeader version = SubsystemVersionHeader.DEFAULT;
		SubsystemTypeHeader type = SubsystemTypeHeader.DEFAULT;
		Header<?> value = subsystemManifest.getSubsystemVersionHeader();
		if (value != null)
			version = (SubsystemVersionHeader)value;
		value = subsystemManifest.getSubsystemTypeHeader();
		if (value != null)
			type = (SubsystemTypeHeader)value;
		if (version.equals(SubsystemVersionHeader.DEFAULT) && uri != null)
			version = new SubsystemVersionHeader(uri.getVersion());
		List<Capability> capabilities;
		List<Requirement> requirements;
		capabilities = subsystemManifest.toCapabilities(this);
		requirements = subsystemManifest.toRequirements(this);
		capabilities.add(new OsgiIdentityCapability(this, symbolicName, version.getVersion(), type.getType()));
		this.capabilities = Collections.unmodifiableList(capabilities);
		this.requirements = Collections.unmodifiableList(requirements);
	}
	
	public void close() {
		IOUtils.close(directory);
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
			return new ByteArrayInputStream(content);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
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
	
	public String getSubsystemSymbolicName() {
		Capability identity = capabilities.get(0);
		return (String)identity.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
	}
	
	public String getSubsystemType() {
		Capability identity = capabilities.get(0);
		return (String)identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
	}
	
	public Version getSubsystemVersion() {
		Capability identity = capabilities.get(0);
		return (Version)identity.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
	}
}

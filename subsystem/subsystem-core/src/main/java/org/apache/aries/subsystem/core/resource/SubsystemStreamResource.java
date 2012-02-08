package org.apache.aries.subsystem.core.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.subsystem.core.internal.SubsystemUri;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.ICloseableDirectory;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemStreamResource implements Resource, RepositoryContent {
	private final List<Capability> capabilities;
	private final ICloseableDirectory directory;
	
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
			directory = FileSystem.getFSRoot(content);
			if (directory == null)
				throw new IOException("Unable to parse content of " + location);
		}
		finally {
			IOUtils.close(content);
		}
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/DEPLOYMENT.MF");
		if (manifest == null)
			manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/SUBSYSTEM.MF");
		String symbolicName = null;
		Version version = Version.emptyVersion;
		if (manifest != null) {
			String value = manifest.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
			if (value != null)
				symbolicName = new SubsystemSymbolicNameHeader(value).getSymbolicName();
			value = manifest.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_VERSION);
			if (value != null)
				version = Version.parseVersion(value);
		}
		if (symbolicName == null) {
			if (uri == null)
				throw new IllegalArgumentException("No symbolic name");
			symbolicName = uri.getSymbolicName();
		}
		if (version == Version.emptyVersion && uri != null)
			version = uri.getVersion();
		List<Capability> capabilities = new ArrayList<Capability>(1);
		capabilities.add(new OsgiIdentityCapability(this, symbolicName, version, SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM));
		this.capabilities = Collections.unmodifiableList(capabilities);
	}
	
	public void close() {
		IOUtils.close(directory);
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null || ResourceConstants.IDENTITY_NAMESPACE.equals(namespace))
			return capabilities;
		return Collections.emptyList();
	}

	@Override
	public InputStream getContent() throws IOException {
		return directory.open();
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
}

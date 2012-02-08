package org.apache.aries.subsystem.core.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.ResourceConstants;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemDirectoryResource implements Resource, RepositoryContent {
	private final List<Capability> capabilities;
	private final IDirectory directory;
	
	public SubsystemDirectoryResource(File content) throws IOException {
		this(FileSystem.getFSRoot(content));
	}
	
	public SubsystemDirectoryResource(IDirectory content) throws IOException {
		if (!content.isDirectory())
			throw new IllegalArgumentException("The content must represent a directory: " + content);
		this.directory = content;
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(content, "OSGI-INF/DEPLOYMENT.MF");
		if (manifest == null)
			manifest = ManifestProcessor.obtainManifestFromAppDir(content, "OSGI-INF/SUBSYSTEM.MF");
		String symbolicName = new SubsystemSymbolicNameHeader(manifest
				.getMainAttributes().getValue(
						SubsystemConstants.SUBSYSTEM_SYMBOLICNAME))
				.getSymbolicName();
		Version version = Version.parseVersion(manifest.getMainAttributes()
				.getValue(SubsystemConstants.SUBSYSTEM_VERSION));
		List<Capability> capabilities = new ArrayList<Capability>(1);
		capabilities.add(new OsgiIdentityCapability(this, symbolicName, version, SubsystemConstants.IDENTITY_TYPE_SUBSYSTEM));
		this.capabilities = Collections.unmodifiableList(capabilities);
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

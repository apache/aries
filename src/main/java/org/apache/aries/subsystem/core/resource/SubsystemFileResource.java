package org.apache.aries.subsystem.core.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemFileResource implements Resource, RepositoryContent {
	private static final String REGEX = "([^@]+)(?:@(.+))?.esa"; // TODO Add .esa to constants.
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	private final List<Capability> capabilities;
	private final IDirectory directory;
	private final File file;
	private final String location;
	
	public SubsystemFileResource(File content) throws IOException {
		file = content;
		directory = FileSystem.getFSRoot(content);
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/DEPLOYMENT.MF");
		if (manifest == null)
			manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/SUBSYSTEM.MF");
		String symbolicName = null;
		Version version = Version.emptyVersion;
		String type = SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION;
		if (manifest != null) {
			String value = manifest.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
			if (value != null)
				symbolicName = new SubsystemSymbolicNameHeader(value).getSymbolicName();
			value = manifest.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_VERSION);
			if (value != null)
				version = Version.parseVersion(value);
			value = manifest.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_TYPE);
			if (value != null)
				type = value;
		}
		Matcher matcher = PATTERN.matcher(content.getName());
		if (symbolicName == null) {
			if (!matcher.matches())
				throw new IllegalArgumentException("No symbolic name");
			symbolicName = new SubsystemSymbolicNameHeader(matcher.group(1)).getSymbolicName();
		}
		if (version == Version.emptyVersion && matcher.matches()) {
			String group = matcher.group(2);
			if (group != null)
				version = Version.parseVersion(group);
		}
		List<Capability> capabilities = new ArrayList<Capability>(1);
		capabilities.add(new OsgiIdentityCapability(this, symbolicName, version, type));
		this.capabilities = Collections.unmodifiableList(capabilities);
		location = "subsystem://?" + SubsystemConstants.SUBSYSTEM_SYMBOLICNAME + '=' + symbolicName + '&' + SubsystemConstants.SUBSYSTEM_VERSION + '=' + version;
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null || IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace))
			return capabilities;
		return Collections.emptyList();
	}

	@Override
	public InputStream getContent() {
		try {
			return new FileInputStream(file);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getLocation() {
		return location;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
}

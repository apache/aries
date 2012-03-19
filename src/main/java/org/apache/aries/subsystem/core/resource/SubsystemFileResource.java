package org.apache.aries.subsystem.core.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.Header;
import org.apache.aries.subsystem.core.archive.HeaderFactory;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.apache.aries.subsystem.core.archive.SubsystemVersionHeader;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.manifest.ManifestProcessor;
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
	private final List<Requirement> requirements;
	
	public SubsystemFileResource(File content) throws IOException {
		file = content;
		directory = FileSystem.getFSRoot(content);
		Manifest manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/DEPLOYMENT.MF");
		if (manifest == null)
			manifest = ManifestProcessor.obtainManifestFromAppDir(directory, "OSGI-INF/SUBSYSTEM.MF");
		
		String symbolicName = manifest == null ? null : manifest.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
		Matcher matcher = PATTERN.matcher(content.getName());
		if (symbolicName == null) {
			if (!matcher.matches())
				throw new IllegalArgumentException("No symbolic name");
			symbolicName = new SubsystemSymbolicNameHeader(matcher.group(1)).getSymbolicName();
		}
		SubsystemManifest.Builder builder = new SubsystemManifest.Builder(symbolicName);
		if (manifest != null)
			for (Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
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
		if (version == SubsystemVersionHeader.DEFAULT && matcher.matches()) {
			String group = matcher.group(2);
			if (group != null)
				version = new SubsystemVersionHeader(group);
		}
		List<Capability> capabilities;
		List<Requirement> requirements;
		capabilities = subsystemManifest.toCapabilities(this);
		requirements = subsystemManifest.toRequirements(this);
		capabilities.add(new OsgiIdentityCapability(this, symbolicName, version.getVersion(), type.getType()));
		this.capabilities = Collections.unmodifiableList(capabilities);
		this.requirements = Collections.unmodifiableList(requirements);
		location = "subsystem://?" + SubsystemConstants.SUBSYSTEM_SYMBOLICNAME + '=' + symbolicName + '&' + SubsystemConstants.SUBSYSTEM_VERSION + '=' + version.getVersion();
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
		if (namespace == null)
			return requirements;
		ArrayList<Requirement> result = new ArrayList<Requirement>(requirements.size());
		for (Requirement requirement : requirements)
			if (namespace.equals(requirement.getNamespace()))
				result.add(requirement);
		result.trimToSize();
		return result;
	}
}

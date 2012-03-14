package org.apache.aries.subsystem.core.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.aries.subsystem.core.archive.SubsystemArchive;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.internal.OsgiIdentityCapability;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class SubsystemDirectoryResource implements Resource {
	private final SubsystemArchive archive;
	private final List<Capability> capabilities;
	
	public SubsystemDirectoryResource(File content) throws Exception {
		archive = new SubsystemArchive(content);
		SubsystemManifest manifest = archive.getSubsystemManifest();
		List<Capability> capabilities = new ArrayList<Capability>(1);
		capabilities.add(new OsgiIdentityCapability(
				this, 
				manifest.getSubsystemSymbolicNameHeader().getSymbolicName(), 
				manifest.getSubsystemVersionHeader().getVersion(), 
				manifest.getSubsystemTypeHeader().getType()));
		this.capabilities = Collections.unmodifiableList(capabilities);
	}
	
	public SubsystemArchive getArchive() {
		return archive;
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null || IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace))
			return capabilities;
		return Collections.emptyList();
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}
}

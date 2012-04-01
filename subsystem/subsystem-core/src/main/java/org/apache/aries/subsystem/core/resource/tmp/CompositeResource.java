package org.apache.aries.subsystem.core.resource.tmp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.util.filesystem.IDirectory;
import org.osgi.resource.Requirement;

public class CompositeResource extends SubsystemResource {
	public CompositeResource(Location location, IDirectory directory,
			SubsystemManifest manifest) throws IOException, URISyntaxException {
		super(location, directory, manifest);
	}
	
	@Override
	protected List<Requirement> computeRequirements(SubsystemManifest manifest) {
		return manifest.toRequirements(this);
	}
	
	@Override
	protected SubsystemManifest computeSubsystemManifestAfterRequirements(SubsystemManifest manifest) {
		return manifest;
	}
}

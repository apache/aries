package org.apache.aries.subsystem.core.resource.tmp;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.util.filesystem.IDirectory;

public class FeatureResource extends SubsystemResource {
	public FeatureResource(Location location, IDirectory directory,
			SubsystemManifest manifest) throws IOException, URISyntaxException {
		super(location, directory, manifest);
	}
}

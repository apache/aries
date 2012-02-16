package org.apache.aries.subsystem.core.internal;

import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemManifestValidator {
	public static void validate(AriesSubsystem subsystem, SubsystemManifest manifest) {
		if (subsystem.isComposite()) {
			SubsystemContentHeader header = manifest.getSubsystemContentHeader();
			if (header == null)
				return;
			for (SubsystemContentHeader.Content content : header.getContents()) {
				if (!content.getVersionRange().isExactVersion())
					throw new SubsystemException("Composite subsystem using version range for content: " + content);
			}
		}
	}
}

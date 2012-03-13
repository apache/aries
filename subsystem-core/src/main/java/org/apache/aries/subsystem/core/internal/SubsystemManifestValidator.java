package org.apache.aries.subsystem.core.internal;

import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.osgi.framework.VersionRange;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemManifestValidator {
	public static void validate(AriesSubsystem subsystem, SubsystemManifest manifest) {
		if (subsystem.isComposite()) {
			SubsystemContentHeader header = manifest.getSubsystemContentHeader();
			if (header == null)
				return;
			for (SubsystemContentHeader.Content content : header.getContents()) {
				// TODO Need to update this to use the new VersionRange.isExact() method, which is more robust.
				if (!isExactVersion(content.getVersionRange()))
					throw new SubsystemException("Composite subsystem using version range for content: " + content);
			}
		}
	}
	
	private static boolean isExactVersion(VersionRange range) {
		if (range.getLeftType() == VersionRange.LEFT_CLOSED
		          && range.getLeft().equals(range.getRight())
		          && range.getRightType() == VersionRange.RIGHT_CLOSED) {
		     return true;
		}
		return false;
	}
}

package org.apache.aries.subsystem.core.internal;

import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.osgi.framework.VersionRange;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemManifestValidator {
	public static void validate(AriesSubsystem subsystem, SubsystemManifest manifest) {
		validatePreferredProviderHeader(manifest.getPreferredProviderHeader());
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
		else if (subsystem.isFeature()) {
			if (manifest.getSubsystemTypeHeader().getProvisionPolicyDirective().isAcceptDependencies())
				throw new SubsystemException("Feature subsystems may not declare a provision-policy of acceptDependencies");
			if (manifest.getHeaders().get(SubsystemConstants.PREFERRED_PROVIDER) != null)
				throw new SubsystemException("Feature subsystems may not declare a " + SubsystemConstants.PREFERRED_PROVIDER + " header");
		}
	}
	
	// TODO Replace this with the new isExact() method on OSGi VersionRange.
	private static boolean isExactVersion(VersionRange range) {
		if (range.getLeftType() == VersionRange.LEFT_CLOSED
		          && range.getLeft().equals(range.getRight())
		          && range.getRightType() == VersionRange.RIGHT_CLOSED) {
		     return true;
		}
		return false;
	}
	
	private static void validatePreferredProviderHeader(PreferredProviderHeader header) {
		if (header == null)
			return;
		for (PreferredProviderHeader.Clause clause : header.getClauses()) {
			String type = (String)clause.getAttribute(PreferredProviderHeader.Clause.ATTRIBUTE_TYPE).getValue();
			if (!(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type) ||
					SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type) ||
					Constants.ResourceTypeBundle.equals(type)))
				throw new SubsystemException("Unsupported " + PreferredProviderHeader.NAME + " type: " + type);
		}
	}
}

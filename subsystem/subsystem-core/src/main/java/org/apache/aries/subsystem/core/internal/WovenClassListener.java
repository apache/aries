package org.apache.aries.subsystem.core.internal;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.apache.aries.subsystem.core.archive.DynamicImportPackageHeader;
import org.apache.aries.subsystem.core.archive.DynamicImportPackageRequirement;
import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class WovenClassListener implements org.osgi.framework.hooks.weaving.WovenClassListener {
	private final BundleContext context;
	private final Subsystems subsystems;
	
	public WovenClassListener(BundleContext context, Subsystems subsystems) {
		this.context = context;
		this.subsystems = subsystems;
	}
	
	/*
	 * This does not update sharing policies up the chain any further than the
	 * parent. Does not account for providers in child subsystems.
	 */
	@Override
	public void modified(WovenClass wovenClass) {
		if (wovenClass.getState() != WovenClass.TRANSFORMED) {
			// Dynamic package imports must be added when the woven class is in
			// the transformed state in order to ensure the class will load once
			// the defined state is reached.
			return;
		}
		List<String> dynamicImports = wovenClass.getDynamicImports();
		if (dynamicImports.isEmpty()) {
			// Nothing to do if there are no dynamic imports.
			return;
		}
		// Add the dynamic imports to the sharing policy of the scoped subsystem 
		// that contains the bundle whose class was woven as a constituent.
		Bundle wovenBundle = wovenClass.getBundleWiring().getBundle();
		BundleRevision wovenRevision = wovenBundle.adapt(BundleRevision.class);
		BasicSubsystem subsystem = subsystems.getSubsystemsByConstituent(new BundleConstituent(null, wovenRevision)).iterator().next();
		if (subsystem.getSubsystemId() == 0) {
			// The root subsystem needs no sharing policy.
			return;
		}
		if (EnumSet.of(Subsystem.State.INSTALLING, Subsystem.State.INSTALLED).contains(subsystem.getState())) {
			// The subsystem must be resolved before adding dynamic package
			// imports to the sharing policy in order to minimize unpredictable
			// wirings.
			AccessController.doPrivileged(new StartAction(subsystem, subsystem, subsystem, true));
		}
		// Determine the requirements that must be added to the sharing policy.
		Collection<Requirement> requirements = new ArrayList<Requirement>();
		for (String dynamicImport : dynamicImports) {
			DynamicImportPackageHeader header = new DynamicImportPackageHeader(dynamicImport);
			for (DynamicImportPackageRequirement requirement : header.toRequirements(wovenRevision)) {
				String pkg = requirement.getPackageName();
				if (pkg.endsWith(".*")) {
					// Dynamic imports with wildcards must always be added.
					requirements.add(requirement);
				}
				else {
					// Absolute dynamic imports are added to the sharing policy
					// only if they are not satisfied within the subsystem.
					FrameworkWiring fw = context.getBundle(org.osgi.framework.Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
					Collection<BundleCapability> providers = fw.findProviders(requirement);
					boolean found = false;
					for (BundleCapability provider : providers) {
						BundleRevision br = provider.getResource();
						if (subsystem.getConstituents().contains(new BundleConstituent(null, br))) {
							// We found a provider that's a constituent of the subsystem.
							found = true;
							break;
						}
					}
					if (!found) {
						requirements.add(requirement);
					}
				}
			}
		}
		if (requirements.isEmpty()) {
			// No wildcards and all dynamic imports were satisfied within the
			// subsystem.
			return;
		}
		// Now update the sharing policy with the necessary requirements.
		Region from = subsystem.getRegion();
		Region to = ((BasicSubsystem)subsystem.getParents().iterator().next()).getRegion();
		RegionUpdater updater = new RegionUpdater(from, to);
		try {
			updater.addRequirements(requirements);
		}
		catch (Exception e) {
			throw new SubsystemException(e);
		}
	}
}

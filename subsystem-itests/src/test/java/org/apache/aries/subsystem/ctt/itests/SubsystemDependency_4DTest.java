package org.apache.aries.subsystem.ctt.itests;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 *  D) Test that Local Repository takes priority over registered repositories
     - Register repository R2
     - Using the Root subsystem, install a scoped subsystem with the following content bundles
       - Bundle C
       - Bundle E
       and the following resources in the Local Repository
       - Bundle F
       - Bundle G
     - Verify that bundles F and G got installed into the Root Subsystem
     - Verify the wiring of C and E wire to F->x and G->y respectively
     - Verify that bundles A and B did not get installed into the Root Subsystem
 */

public class SubsystemDependency_4DTest extends SubsystemDependencyTestBase 
{
	private static final String SUBSYSTEM_4D = "sdt_application4d.esa";
	private Subsystem subsystem;
	
	@Override
	public void createApplications() throws Exception {
		super.createApplications();
		createApplication4d();
		registerRepositoryR2();
	}
	
	//  - Verify that bundles F and G got installed into the Root Subsystem
	@Test
	public void verifyBundesFandGinstalledIntoRootSubsystem() throws Exception
	{
		startSubsystem();
		verifyBundlesInstalled (bundleContext, "Root", BUNDLE_F, BUNDLE_G);
		stopSubsystem();
	}
	

	
	// - Verify the wiring of C and E wire to F->x and G->y respectively
	@Test
	public void verifyBundleCWiredToPackageXFromBundleF() throws Exception
	{
		startSubsystem();
		verifySinglePackageWiring (subsystem, BUNDLE_C, "x", BUNDLE_F);
		stopSubsystem();
	}
	
	@Test
	public void verifyBundleEWiredToCapability_yFromBundleG() throws Exception
	{
		startSubsystem();
		verifyCapabilityWiring (subsystem, BUNDLE_E, "y", BUNDLE_G);
		stopSubsystem();
	}
	
	// - Verify that bundles A and B did not get installed into the Root Subsystem
	@Test
	public void verifyBundlesAandBNotInstalledInRootSubsystem() throws Exception
	{
		startSubsystem();
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle b: bundles) {
			assertTrue ("Bundle A should not have been provisioned!", !b.getSymbolicName().equals(BUNDLE_A));
			assertTrue ("Bundle B should not have been provisioned!", !b.getSymbolicName().equals(BUNDLE_B));
		}
		stopSubsystem();
	}
	
	// doing this within @Before doesn't work :(
	private void startSubsystem() throws Exception
	{ 
		subsystem = installSubsystemFromFile(SUBSYSTEM_4D);
		startSubsystem(subsystem);
	}
	
	private void stopSubsystem() throws Exception
	{
		stopSubsystem(subsystem);
		uninstallSubsystem(subsystem);
	}
	
	private static void createApplication4d() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4D);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		String appContent = BUNDLE_C + "," + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_4D + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4D, BUNDLE_F, BUNDLE_G);
	}
}

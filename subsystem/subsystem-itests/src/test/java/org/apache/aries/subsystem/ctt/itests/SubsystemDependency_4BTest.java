package org.apache.aries.subsystem.ctt.itests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * B) Test with no pre-installed transitive resources
     - Register repository R2
     - Using the Root subsystem, install a scoped subsystem with the following content bundles and no local repository
       - Bundle C
       - Bundle D
       - Bundle E
     - Verify that bundles A and B got installed into the Root Subsystem
     - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
 */

public class SubsystemDependency_4BTest extends SubsystemDependencyTestBase 
{

	protected static String APPLICATION_B="sdt_application.b.esa";
	
	@Override
	public void createApplications() throws Exception {
		super.createApplications();
		createTestApplicationB();
	}
	
	@Before
	public void registerRepo() throws Exception {
		registerRepositoryR2();
	}
	
	// - Verify that bundles A and B got installed into the Root Subsystem
	@Test
	public void verifyBundlesAandBinstalledIntoRootRegion() throws Exception
	{
		System.out.println ("Into verifyBundlesAandBinstalledIntoRootRegion");
		
		Subsystem s = installSubsystemFromFile(APPLICATION_B);
		startSubsystem(s);
		Bundle[] bundles = bundleContext.getBundles();
		Collection<String> bundleNames = new ArrayList<String>();
		for (Bundle b : bundles) { 
			bundleNames.add(b.getSymbolicName());
		}
		assertTrue ("Bundle A should have been provisioned to the root region", bundleNames.contains(BUNDLE_A));
		assertTrue ("Bundle B should have been provisioned to the root region", bundleNames.contains(BUNDLE_B));
		stopSubsystem(s);
	}
	
	@Test
	public void verifyBundleCWiredToPackage_xFromBundleA() throws Exception
	{
		Subsystem s = installSubsystemFromFile(APPLICATION_B);
		startSubsystem(s);
		verifySinglePackageWiring (s, BUNDLE_C, "x", BUNDLE_A);
		stopSubsystem(s);
	}
	
	@Test
	public void verifyBundleDWiredToBundleA() throws Exception
	{ 
		Subsystem s = installSubsystemFromFile(APPLICATION_B);
		startSubsystem(s);
		verifyRequireBundleWiring (s, BUNDLE_D, BUNDLE_A);
		stopSubsystem(s);
	}

	@Test
	public void verifyBundleEWiredToCapability_yFromBundleB() throws Exception
	{
		Subsystem s = installSubsystemFromFile(APPLICATION_B);
		startSubsystem(s);
		verifyCapabilityWiring (s, BUNDLE_E, "y", BUNDLE_B);
		stopSubsystem(s);
	}
	
	private static void createTestApplicationB() throws Exception
	{ 
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		String appContent = BUNDLE_C + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_D + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_E + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y");
		createManifest(APPLICATION_B + ".mf", attributes);
		createSubsystem(APPLICATION_B);
	}
}

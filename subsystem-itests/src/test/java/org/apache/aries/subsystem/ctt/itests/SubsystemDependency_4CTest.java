package org.apache.aries.subsystem.ctt.itests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
C) Test with pre-installed transitive resources
- Register repository R1
- Using the Root subsystem, install a composite subsystem S1 with the following content bundles (with no import/export policy)
  - Bundle A
  - Bundle B
- Using the subsystem S1, install a composite S2 that imports package x, requires bundle A and required capability y
- Verify the wiring of C, D and E wire to A->x, A, B->y respectively 
- Verify no new bundles are installed into the Root or S1 subsystems
*/

public class SubsystemDependency_4CTest extends SubsystemDependencyTestBase 
{
	private static final String SUBSYSTEM_S1 = "sdt_composite.s1.esa";
	private static final String SUBSYSTEM_S2 = "sdt_composite.s2.esa";
	private static boolean _testSubsystemCreated = false;
	private Subsystem s1;
	private Subsystem s2;
	
	@Before
	public void setUp() throws Exception
	{ 
		super.setUp();
		if (!_testSubsystemCreated) { 
			createSubsystemS1();
			createSubsystemS2();
			_testSubsystemCreated = true;
		}
		registerRepositoryR1();
		
	}
	
	// doing this within @Before doesn't work :(
	private void startSubsystems() throws Exception
	{ 
		s1 = installSubsystemFromFile(SUBSYSTEM_S1);
		startSubsystem(s1);
		s2 = installSubsystemFromFile(s1, SUBSYSTEM_S2);
		startSubsystem(s2);
	}
	
	private void stopSubsystems() throws Exception
	{
		stopSubsystem(s2);
		stopSubsystem(s1);
		uninstallSubsystem(s2);
		uninstallSubsystem(s1);
	}
	
	// Using the subsystem S1, install a composite S2 that 
	//   imports package x, 
	//   requires bundle A 
	//   and required capability y
    // - Verify the wiring of C, D and E wire to A->x, A, B->y respectively 

	@Test
	public void verifyCinS1WiresToAxInS2() throws Exception
	{
		startSubsystems();
		verifySinglePackageWiring (s2, BUNDLE_C, "x", BUNDLE_A);
		stopSubsystems();
	}
	
	@Test
	public void verifyBundleDWiredToBundleA() throws Exception
	{
		startSubsystems();
		verifyRequireBundleWiring (s2, BUNDLE_D, BUNDLE_A);
		stopSubsystems();
	}
	
	@Test
	public void verifyBundleEWiredToCapability_yFromBundleB() throws Exception
	{
		startSubsystems();
		verifyCapabilityWiring (s2, BUNDLE_E, "y", BUNDLE_B);
		stopSubsystems();
	}
	
	/*
	 *  Verify no new bundles are installed into the Root or S1 subsystems 
	 */
	
	private static final Collection<String> _expectedRootRegionBundles = Arrays.asList(new String[]{ 
		"org.eclipse.osgi", "org.ops4j.pax.exam", "org.ops4j.pax.exam.junit.extender", 
		"org.ops4j.pax.exam.junit.extender.impl", "org.ops4j.pax.logging.pax-logging-api", 
		"org.ops4j.pax.logging.pax-logging-service", "org.ops4j.pax.url.mvn", 
		"org.eclipse.osgi.services", "org.eclipse.equinox.region", 
		"org.apache.aries.testsupport.unit", "org.apache.aries.application.api", 
		"org.apache.aries.util", "org.apache.aries.application.utils", 
		"org.apache.felix.bundlerepository", "org.apache.felix.resolver", 
		"org.eclipse.equinox.coordinator", "org.eclipse.equinox.event", 
		"org.apache.aries.subsystem.api", "org.apache.aries.subsystem.core", 
		"com.springsource.org.junit", "org.ops4j.pax.exam.rbc", 
		"org.osgi.service.subsystem.region.context.0", "pax-exam-probe",
		"wrap_mvn_org.ops4j.pax.exam_pax-exam-junit_1.2.3", "org.apache.aries.subsystem.itest.interfaces"});
	
	private static final Collection<String> _expectedS1RegionBundles = Arrays.asList(new String[] { 
			BUNDLE_A, BUNDLE_B, "org.osgi.service.subsystem.region.context.1"});

	@Test
	public void verifyNoUnexpectedBundlesProvisioned() throws Exception 
	{ 
		startSubsystems();
		checkSubsystemContents ("Root", bundleContext,_expectedRootRegionBundles);
		BundleContext s1Context = s1.getBundleContext();
		checkSubsystemContents ("S1", s1Context, _expectedS1RegionBundles);
		stopSubsystems();
	}
	
	private void checkSubsystemContents (String subsystemName, BundleContext subsystemContext, Collection<String> expectedBundleNames ) 
	{ 
		Bundle[] regionBundles = subsystemContext.getBundles();
		if (expectedBundleNames.size() != regionBundles.length) { 
			fail ("Wrong number of bundles in the " + subsystemName + " subsystem." 
				+ " Expected " + expectedBundleNames.size() + " bundles: " + expectedBundleNames
				+ " Found " + regionBundles.length + " bundles: " + Arrays.toString(regionBundles));
		}
		for (Bundle b: regionBundles) {
			String bsn = b.getSymbolicName();
			assertTrue ("Unexpected bundle found in " + subsystemName + " subsystem: " + bsn, expectedBundleNames.contains(bsn));
		}
	}
	
	/*
	 * a composite subsystem S1 with the following content bundles (with no import/export policy)
       - Bundle A
       - Bundle B
	 */
	private static void createSubsystemS1() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_S1);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		String appContent = BUNDLE_A + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_B + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_S1 + ".mf", attributes);
		createSubsystem(SUBSYSTEM_S1);
	}
	
	/*
	 * a composite S2 that 
	 *  imports package x, 
	 *  requires bundle A 
	 *  and required capability y
	 *  
	 * Although the test plan is silent as to the content of S2, I think we have to assume
	 * that it contains bundles C, D and E
	 */
	private static void createSubsystemS2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_S2);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		String appContent = BUNDLE_C + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_D + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_E + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y"); 
		
		createManifest(SUBSYSTEM_S2 + ".mf", attributes);
		createSubsystem(SUBSYSTEM_S2);
	}

}

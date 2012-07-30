package org.apache.aries.subsystem.ctt.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * First block: section 4A
 * 
A) Test a transitively closed subsystem deploys no transitive resources
 - Register repository R1
 - Using the Root subsystem, install a scoped subsystem with the following content bundles and no local repository
   - Bundle A
   - Bundle B
   - Bundle C
   - Bundle D
   - Bundle E
 - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
 - Verify no new bundles are installed into the Root subsystem (particularly bundles F and G)
 
*/

public class SubsystemDependency_4ATest extends SubsystemDependencyTestBase 
{
	protected static String APPLICATION_A="sdt_application.a.esa";
	private static boolean _testAppCreated = false;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		if (!_testAppCreated) { 
			createTestApplicationA();
			_testAppCreated = true;
		}
		registerRepositoryR1();
	}
	
	@Test
	public void verifyBundleCWiredToPackageXFromBundleA() throws Exception
	{ 
		Subsystem s = installSubsystemFromFile(APPLICATION_A);
		startSubsystem(s);
		
		verifySinglePackageWiring (s, BUNDLE_C, "x", BUNDLE_A);
 
		stopSubsystem(s);
	}
	
	@Test
	public void verifyBundleDWiredToBundleA() throws Exception
	{ 
		Subsystem s = installSubsystemFromFile(APPLICATION_A);
		startSubsystem(s);
		verifyRequireBundleWiring (s, BUNDLE_D, BUNDLE_A);
		stopSubsystem(s);
	}
	
	@Test
	public void verifyBundleEWiredToCapability_yFromBundleB() throws Exception
	{
		Subsystem s = installSubsystemFromFile (APPLICATION_A);
		startSubsystem(s);
		verifyCapabilityWiring (s, BUNDLE_E, "y", BUNDLE_B);
		stopSubsystem(s);
	}
	
	/*
	 * Verify no new bundles are installed into the Root subsystem 
	 * (particularly bundles F and G)
 	 * 
 	 * As of the time of writing, the Root subsystem should contain 23 bundles: 
 	 * org.eclipse.osgi
     * org.ops4j.pax.exam
     * org.ops4j.pax.exam.junit.extender
     * org.ops4j.pax.exam.junit.extender.impl
     * wrap_mvn_org.ops4j.pax.exam_pax-exam-junit
     * org.ops4j.pax.logging.pax-logging-api
     * org.ops4j.pax.logging.pax-logging-service
     * org.ops4j.pax.url.mvn
     * org.eclipse.osgi.services
     * org.eclipse.equinox.region
     * org.apache.aries.testsupport.unit
     * org.apache.aries.application.api
     * org.apache.aries.util
     * org.apache.aries.application.utils
     * org.apache.felix.bundlerepository
     * org.apache.felix.resolver
     * org.eclipse.equinox.coordinator
     * org.eclipse.equinox.event
     * org.apache.aries.subsystem.api
     * org.apache.aries.subsystem.core
     * org.apache.aries.subsystem.itest.interfaces
     * com.springsource.org.junit
     * org.ops4j.pax.exam.rbc
     * org.osgi.service.subsystem.region.context.0
     * pax-exam-probe
     * 
	 */
	@Test
	public void verifyNoUnexpectedBundlesProvisioned() 
	{ 
		Bundle[] bundles = bundleContext.getBundles();
		assertEquals ("Wrong number of bundles in the Root subsystem", 25, bundles.length);
		for (Bundle b: bundles) {
			assertTrue ("Bundle F should not have been provisioned!", !b.getSymbolicName().equals(BUNDLE_F));
			assertTrue ("Bundle G should not have been provisioned!", !b.getSymbolicName().equals(BUNDLE_G));
		}
	}

	private static void createTestApplicationA() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		String appContent = BUNDLE_A +","+ BUNDLE_B + "," + BUNDLE_C
			+ "," + BUNDLE_D + "," + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(APPLICATION_A + ".mf", attributes);
		createSubsystem(APPLICATION_A);
	}

}

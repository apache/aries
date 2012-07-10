package org.apache.aries.subsystem.ctt.itests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 *  E) Test acceptDependencies policy
    1. Root is the only acceptDependencies policy
       a. - Register repository R2
          - Using the Root subsystem, install a composite subsystem S1 with 
            - no content bundles 
            - imports package x, requires bundle A and requires capability y
          - Using the subsystem S1, install an application S2 with
            - content bundles C, D and E
          - Verify that bundles A and B got installed into the Root Subsystem
          - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
          - Repeat test with S2 as a composite that imports package x, requires bundle A and required capability y
          - Repeat test with S2 as a feature
       b. - same as 4E1a except S2 is a content resource of S1
          - There are 6 combinations to test
            - app_app, app_comp, app_feat, comp_app, comp_comp, comp_feat
    2. A non-Root subsystem has acceptDependencies policy
       a. - Register repository R2
          - Using the Root subsystem, install a composite subsystem S1 with 
            - no content bundles 
            - acceptTransitive policy
            - no sharing policy
          - Using the subsystem S1, install an application S2 with
            - content bundles C, D and E
            - note sharing policy gets computed
          - Verify that bundles A and B got installed into the S1 Subsystem
          - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
          - Repeat test with S2 as a composite that imports package x, requires bundle A and required capability y
          - Repeat test with S2 as a feature
       b. - same as 4E2a except S2 is a content resource of S1
          - There are 6 combinations to test
            - app_app, app_comp, app_feat, comp_app, comp_comp, comp_feat
    3. Invalid sharing policy prevents dependency installation
       a. - Register repository R2
          - Using the Root subsystem, install a composite subsystem S1 with 
            - no content bundles 
            - NO acceptDependency policy
            - no sharing policy
          - Using the subsystem S1, install an application S2 with
            - content bundles C, D and E
            - note the sharing policy gets computed
          - Verify the installation of S2 fails because there is no valid place to install the 
            required transitive resources A and B that allow S2 constituents to have access.
          - Verify resources A and B are not installed in the Root subsystem.
          - Repeat test with S2 as a composite that imports package x, requires bundle A and requires capability y.
          - Repeat test with S2 as a feature
       c. - same as 4E3a except S1 is a composite that has S2 in its Subsystem-Content; S1 fails to install
 */
public class SubsystemDependency_4ETest extends SubsystemDependencyTestBase 
{
	/*
	1. Root is the only acceptDependencies policy
    a. - Register repository R2
       - Using the Root subsystem, install a composite subsystem S1 with 
         - no content bundles 
         - imports package x, requires bundle A and requires capability y
       - Using the subsystem S1, install an application S2 with
         - content bundles C, D and E
       - Verify that bundles A and B got installed into the Root Subsystem
       - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
       - Repeat test with S2 as a composite that imports package x, requires bundle A and required capability y
       - Repeat test with S2 as a feature
    b. - same as 4E1a except S2 is a content resource of S1
       - There are 6 combinations to test
         - app_app, app_comp, app_feat, comp_app, comp_comp, comp_feat
	*/
	private static final String SUBSYSTEM_4E_S1 = "sdt_composite4e_s1.esa";
	private static final String SUBSYSTEM_4E_S2 = "sdt_application4e_s1.esa";
	private static boolean _testSubsystemsCreated = false;
	private Subsystem subsystem_s1;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		if (!_testSubsystemsCreated) { 
			createComposite4E_S1();
			createApplication4E_S2();
		}
		registerRepositoryR2();
	}
	
	/*
	 * Using the subsystem S1, install an application S2 with
         - content bundles C, D and E
       - Verify that bundles A and B got installed into the Root Subsystem
	 */
	@Test
	public void verifyBundlesAandBInstalledIntoRootSubsystem() throws Exception
	{
		Subsystem s1 = installSubsystemFromFile(SUBSYSTEM_4E_S1);
		startSubsystem(s1);
		
		Subsystem s2 = installSubsystemFromFile(s1, SUBSYSTEM_4E_S2);
		startSubsystem(s2);
		
		verifyBundlesInstalled (bundleContext, "Root", BUNDLE_A, BUNDLE_B);
		
		stopSubsystem(s2);
		stopSubsystem(s1);
		uninstallSubsystem(s2);
		uninstallSubsystem(s1);
		
	}
	
	/*
	 * a composite subsystem S1 with 
         - no content bundles 
         - imports package x, requires bundle A and requires capability y
	 */
	private static void createComposite4E_S1() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E_S1);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y"); 
		createManifest(SUBSYSTEM_4E_S1 + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E_S1);
	}

	/* an application S2 with
    - content bundles C, D and E */
	private static void createApplication4E_S2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E_S2);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		String appContent = BUNDLE_C + ", " + BUNDLE_D + ", " + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_4E_S2 + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E_S2);
	}


	
}

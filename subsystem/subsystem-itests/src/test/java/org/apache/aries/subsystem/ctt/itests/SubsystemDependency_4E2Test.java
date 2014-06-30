package org.apache.aries.subsystem.ctt.itests;

import static org.osgi.service.subsystem.SubsystemConstants.SUBSYSTEM_SYMBOLICNAME;
import static org.osgi.service.subsystem.SubsystemConstants.SUBSYSTEM_TYPE;
import static org.osgi.service.subsystem.SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION;
import static org.osgi.service.subsystem.SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE;
import static org.osgi.service.subsystem.SubsystemConstants.SUBSYSTEM_TYPE_FEATURE;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * 2. A non-Root subsystem has acceptDependencies policy
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
public class SubsystemDependency_4E2Test extends SubsystemDependencyTestBase 
{
	private static final String SUBSYSTEM_4E2_S1_COMP = "sdt_composite4e2_s1.esa";
	private static final String SUBSYSTEM_4E2_S2_APP = "sdt_application4e2_s2.esa";
	private static final String SUBSYSTEM_4E2_S2_COMP = "sdt_composite4e2_s2.esa";
	private static final String SUBSYSTEM_4E2_S2_FEATURE = "sdt_feature4e2_s2.esa";
	
	@Override
	public void createApplications() throws Exception {
		super.createApplications();
		createComposite4E2_S1();
		createApplication4E2_S2();
		createComposite4E2_S2();
		createFeature4E2_S2();
		registerRepositoryR2();
	}
	
	/*
	 * - Using the Root subsystem, install a composite subsystem S1 with 
            - no content bundles 
            - acceptTransitive policy
            - no sharing policy
        - Using the subsystem S1, install an application S2 with
            - content bundles C, D and E
            - note sharing policy gets computed
          - Verify that bundles A and B got installed into the S1 Subsystem
	 */
	@Test
	public void test4E2A_where_S2isAnApplication() throws Exception
	{
		Subsystem s1 = installSubsystemFromFile (SUBSYSTEM_4E2_S1_COMP);
		startSubsystem(s1);
		
		Subsystem s2 = installSubsystemFromFile (s1, SUBSYSTEM_4E2_S2_APP);
		startSubsystem(s2);
		
		verifyBundlesInstalled (s1.getBundleContext(), "s1", BUNDLE_A, BUNDLE_B);
		
		//  - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
		checkBundlesCDandEWiredToAandB(s2);
		
		stop(s1, s2);
	}
	
	/* Repeat test [4e2a.app] with S2 as a composite 
	 * that imports package x, requires bundle A and required capability y
	 */
	@Test
	public void test4E2A_where_S2isAComposite() throws Exception
	{
		Subsystem s1 = installSubsystemFromFile (SUBSYSTEM_4E2_S1_COMP);
		startSubsystem(s1);
		Subsystem s2 = installSubsystemFromFile (s1, SUBSYSTEM_4E2_S2_COMP);
		startSubsystem(s2);
		verifyBundlesInstalled (s1.getBundleContext(), "s1", BUNDLE_A, BUNDLE_B);
		//  - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
		checkBundlesCDandEWiredToAandB(s2);
		stop(s1, s2);
	}
	
	/*
	 * - Repeat test [4e2a.app] with S2 as a feature
	 */
	@Test
	public void test4E2A_where_S2isAFeature() throws Exception
	{
		Subsystem s1 = installSubsystemFromFile (SUBSYSTEM_4E2_S1_COMP);
		startSubsystem(s1);
		Subsystem s2 = installSubsystemFromFile (s1, SUBSYSTEM_4E2_S2_FEATURE);
		startSubsystem(s2);
		verifyBundlesInstalled (s1.getBundleContext(), "s1", BUNDLE_A, BUNDLE_B);
		//  - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
		checkBundlesCDandEWiredToAandB(s2);
		stop(s1, s2);
	}
	
	/*
	 * 4e2b: - same as 4E2a except S2 is a content resource of S1
             - There are 6 combinations to test
               - app_app, app_comp, app_feat, comp_app, comp_comp, comp_feat
	 */
	@Test
	public void FourE2b_App_App() throws Exception
	{
		combinationTest_4e2b ("4e2b_App_App.esa", SUBSYSTEM_TYPE_APPLICATION, 
				SUBSYSTEM_4E2_S2_APP, SUBSYSTEM_TYPE_APPLICATION);
	}
	
	@Test
	public void FourE2b_App_Comp() throws Exception
	{
		combinationTest_4e2b ("4e2b_App_Comp.esa", SUBSYSTEM_TYPE_APPLICATION, 
				SUBSYSTEM_4E2_S2_COMP, SUBSYSTEM_TYPE_COMPOSITE);
	}
	
	@Test
	public void FourE2b_App_Feature() throws Exception
	{
		combinationTest_4e2b ("4e2b_App_Feature.esa", SUBSYSTEM_TYPE_APPLICATION, 
				SUBSYSTEM_4E2_S2_FEATURE, SUBSYSTEM_TYPE_FEATURE);
	}
	
	@Test
	public void FourE2b_Comp_App() throws Exception
	{
		combinationTest_4e2b ("4e2b_App_App.esa", SUBSYSTEM_TYPE_APPLICATION, 
				SUBSYSTEM_4E2_S2_APP, SUBSYSTEM_TYPE_APPLICATION);
	}
	
	@Test
	public void FourE2b_Comp_Comp() throws Exception
	{
		combinationTest_4e2b ("4e2b_Comp_Comp.esa", SUBSYSTEM_TYPE_COMPOSITE, 
				SUBSYSTEM_4E2_S2_COMP, SUBSYSTEM_TYPE_COMPOSITE);
	}
	@Test
	public void FourE2b_Comp_Feature() throws Exception
	{
		combinationTest_4e2b ("4e2b_Comp_Feature.esa", SUBSYSTEM_TYPE_COMPOSITE, 
				SUBSYSTEM_4E2_S2_FEATURE, SUBSYSTEM_TYPE_FEATURE);
	}
	
	/*
	 * Build a subsystem called combinedSubsystemName with a parent of parentType and a child of childType
	 * Start the subsystem
	 *   - Verify that bundles A and B got installed into the S1 Subsystem
     *   - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
	 * Stop and uninstall the combination
	 */
	private void combinationTest_4e2b (String combinedSubsystemName, String parentType, String childName, String childType) throws Exception
	{
		createCombinedSubsystem (combinedSubsystemName, parentType, childName, childType);
		Subsystem s = installSubsystemFromFile(combinedSubsystemName);
		startSubsystem(s);
		
		verifyBundlesInstalled (s.getBundleContext(), "s1", BUNDLE_A, BUNDLE_B);
		
		Collection<Subsystem> children = s.getChildren();  
		// we only expect one child
		Subsystem child = children.iterator().next();
		checkBundlesCDandEWiredToAandB (child);
		
		stopSubsystem(s);
		uninstallSubsystem(s);
	}
	
	private void createCombinedSubsystem (String symbolicName, String parentType, String childSubsystem, String childSubsystemType) throws Exception
	{
		File f = new File (symbolicName);
		if (!f.exists()) { 
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put(SUBSYSTEM_SYMBOLICNAME, symbolicName);
			attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
			attributes.put(SUBSYSTEM_TYPE, parentType
					+ ";" + SubsystemConstants.PROVISION_POLICY_DIRECTIVE 
					+ ":=" + SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES); 
			
			StringBuffer subsystemContent = new StringBuffer();
			subsystemContent.append (childSubsystem + ";" + Constants.VERSION_ATTRIBUTE 
					+ "=\"[1.0.0,1.0.0]\";");
			// I'm not sure that this is the best "type" attribute to use, but it will do. 
			subsystemContent.append(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + "=");
			subsystemContent.append(childSubsystemType);
			attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, subsystemContent.toString());
			
			// This seems to be necessary to get Comp_Comp and Comp_Feature to work
			 if (parentType == SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE) { 
				attributes.put(Constants.IMPORT_PACKAGE, "x");
				attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
				attributes.put(Constants.REQUIRE_CAPABILITY, "y");
			}
			
			createManifest(symbolicName + ".mf", attributes);
			createSubsystem(symbolicName, childSubsystem);
		}
	}
	
	
	/*
	 * Stop s2, stop s1, uninstall s2, uninstall s1
	 */
	private void stop(Subsystem s1, Subsystem s2) throws Exception
	{
		stopSubsystem(s2);
		stopSubsystem(s1);
		uninstallSubsystem(s2);
		uninstallSubsystem(s1);
	}
	
	/* a feature S2 with
     *  - content bundles C, D and E
	 */
	private static void createFeature4E2_S2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E2_S2_FEATURE);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		String appContent = BUNDLE_C + ", " + BUNDLE_D + ", " + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_4E2_S2_FEATURE + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E2_S2_FEATURE);
	}
	
	/* an application S2 with
     *  - content bundles C, D and E
	 */
	private static void createApplication4E2_S2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E2_S2_APP);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SUBSYSTEM_TYPE_APPLICATION);
		String appContent = BUNDLE_C + ", " + BUNDLE_D + ", " + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_4E2_S2_APP + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E2_S2_APP);
	}
	
	/*
	 * a composite [S2] 
	 * that imports package x, requires bundle A and required capability y
	 */
	private static void createComposite4E2_S2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E2_S2_COMP);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y"); 
		String appContent = BUNDLE_C + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
		+ ", " + BUNDLE_D + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
		+ ", " + BUNDLE_E + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_4E2_S2_COMP + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E2_S2_COMP);
	}
	
	
	/* 
	 * a composite subsystem S1 with 
	 *   - no content bundles 
     *   - acceptTransitive policy
     *   - no sharing policy
	 */
	private static void createComposite4E2_S1() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E2_S1_COMP);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE
				+ ";" + SubsystemConstants.PROVISION_POLICY_DIRECTIVE 
				+ ":=" + SubsystemConstants.PROVISION_POLICY_ACCEPT_DEPENDENCIES); 
		createManifest(SUBSYSTEM_4E2_S1_COMP + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E2_S1_COMP);
	}
}

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
    
 */
public class SubsystemDependency_4E1Test extends SubsystemDependencyTestBase 
{
	private static final String SUBSYSTEM_4E_S1_COMP = "sdt_composite4e_s1.esa";
	private static final String SUBSYSTEM_4E_S2_APP = "sdt_application4e_s2.esa";
	private static final String SUBSYSTEM_4E_S2_COMP = "sdt_composite4e_s2.esa";
	private static final String SUBSYSTEM_4E_S2_FEATURE = "sdt_feature4e_s2.esa";
	
	@Override
	public void createApplications() throws Exception
	{
		super.createApplications();
		createComposite4E_S1();
		createApplication4E_S2();
		createComposite4E_S2();
		createFeature4E_S2();
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
		Subsystem s1 = installSubsystemFromFile(SUBSYSTEM_4E_S1_COMP);
		startSubsystem(s1);
		Subsystem s2 = installSubsystemFromFile(s1, SUBSYSTEM_4E_S2_APP);
		startSubsystem(s2);
		
		verifyBundlesInstalled (bundleContext, "Root", BUNDLE_A, BUNDLE_B);
		
		stop(s1, s2);
	}
	
	
	@Test
	public void verifyExpectedWiringsForS2_App() throws Exception
	{
		Subsystem s1 = installSubsystemFromFile(SUBSYSTEM_4E_S1_COMP);
		startSubsystem(s1);
		Subsystem s2 = installSubsystemFromFile(s1, SUBSYSTEM_4E_S2_APP);
		startSubsystem(s2);
		
		// - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
		verifySinglePackageWiring (s2, BUNDLE_C, "x", BUNDLE_A);
		verifyRequireBundleWiring (s2, BUNDLE_D, BUNDLE_A);
		verifyCapabilityWiring (s2, BUNDLE_E, "y", BUNDLE_B);

		stop(s1, s2);
	}
	
	// - Repeat test with S2 as a composite that 
	//     imports package x, requires bundle A and required capability y
	@Test
	public void verifyExpectedWiringsForS2_Composite() throws Exception
	{
		Subsystem s1 = installSubsystemFromFile(SUBSYSTEM_4E_S1_COMP);
		startSubsystem(s1);
		Subsystem s2 = installSubsystemFromFile(s1, SUBSYSTEM_4E_S2_COMP);
		startSubsystem(s2);
		checkBundlesCDandEWiredToAandB(s2);
		stop(s1, s2);
	}
	
	// - Repeat test with S2 as a feature
	@Test
	public void verifyExpectedWiringsForS2_Feature() throws Exception
	{
		Subsystem s1 = installSubsystemFromFile(SUBSYSTEM_4E_S1_COMP);
		startSubsystem(s1);
		Subsystem s2 = installSubsystemFromFile(s1, SUBSYSTEM_4E_S2_FEATURE);
		startSubsystem(s2);
		checkBundlesCDandEWiredToAandB (s2);
		stop(s1, s2);
	}
	
	// b. - same as 4E1a except S2 is a content resource of S1
    //  - There are 6 combinations to test
    //   - app_app, app_comp, app_feat, comp_app, comp_comp, comp_feat

	@Test
	public void FourE1b_App_App() throws Exception
	{
		combinationTest ("4eS1_App_App.esa", SUBSYSTEM_TYPE_APPLICATION, SUBSYSTEM_4E_S2_APP, SUBSYSTEM_TYPE_APPLICATION);
	}
	
	@Test
	public void FourE1b_App_Comp() throws Exception
	{
		combinationTest ("4eS1_App_Comp.esa", SUBSYSTEM_TYPE_APPLICATION, SUBSYSTEM_4E_S2_COMP, SUBSYSTEM_TYPE_COMPOSITE);
	}
	
	@Test
	public void FourE1b_App_Feature() throws Exception
	{
		combinationTest ("4eS1_App_Feature.esa", SUBSYSTEM_TYPE_APPLICATION, SUBSYSTEM_4E_S2_FEATURE, SUBSYSTEM_TYPE_FEATURE);
	}
	
	@Test
	public void FourE1b_Comp_App() throws Exception
	{
		combinationTest ("4eS1_Comp_App.esa", SUBSYSTEM_TYPE_COMPOSITE, SUBSYSTEM_4E_S2_APP, SUBSYSTEM_TYPE_APPLICATION);
	}
	
	@Test
	public void FourE1b_Comp_Comp() throws Exception
	{
		combinationTest ("4eS1_Comp_Comp.esa", SUBSYSTEM_TYPE_COMPOSITE, SUBSYSTEM_4E_S2_COMP, SUBSYSTEM_TYPE_COMPOSITE);
	}
	
	@Test
	public void FourE1b_Comp_Feature() throws Exception
	{
		combinationTest ("4eS1_Comp_Feature.esa", SUBSYSTEM_TYPE_COMPOSITE, SUBSYSTEM_4E_S2_FEATURE, SUBSYSTEM_TYPE_FEATURE);
	}
	
	/*
	 * Build a subsystem called combinedSubsystemName with a parent of parentType and a child of childType
	 * Start the subsystem
	 * runChecks on the comination
	 * Stop and uninstall the combination
	 */
	private void combinationTest (String combinedSubsystemName, String parentType, String childName, String childType) throws Exception
	{
		createCombinedSubsystem (combinedSubsystemName, parentType, childName, childType);
		Subsystem s = installSubsystemFromFile(combinedSubsystemName);
		startSubsystem(s);
		
		Collection<Subsystem> children = s.getChildren();  
		// we only expect one child
		Subsystem child = children.iterator().next();
		checkBundlesCDandEWiredToAandB (child);
		
		stopSubsystem(s);
		uninstallSubsystem(s);
	}
	
	/*
	 * Create a nested parent/child subsystem with symbolicName, where parent is of type and child is the 
	 * previously-created childSubsystem
	 */
	private void createCombinedSubsystem (String symbolicName, String parentType, String childSubsystem, String childSubsystemType) throws Exception
	{
		File f = new File (symbolicName);
		if (!f.exists()) { 
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put(SUBSYSTEM_SYMBOLICNAME, symbolicName);
			attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
			attributes.put(SUBSYSTEM_TYPE, parentType);
			if (parentType == SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE) { 
				attributes.put(Constants.IMPORT_PACKAGE, "x");
				attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
				attributes.put(Constants.REQUIRE_CAPABILITY, "y");
			}
			StringBuffer subsystemContent = new StringBuffer();
			subsystemContent.append (childSubsystem + ";" + Constants.VERSION_ATTRIBUTE 
					+ "=\"[1.0.0,1.0.0]\";");
			// I'm not sure that this is the best "type" attribute to use, but it will do. 
			subsystemContent.append(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + "=");
			subsystemContent.append(childSubsystemType);
			attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, subsystemContent.toString());
			createManifest(symbolicName + ".mf", attributes);
			createSubsystem(symbolicName, childSubsystem);
		}
	}
	
	private void stop(Subsystem s1, Subsystem s2) throws Exception
	{
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
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E_S1_COMP);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y"); 
		createManifest(SUBSYSTEM_4E_S1_COMP + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E_S1_COMP);
	}

	/* an application S2 with
    - content bundles C, D and E */
	private static void createApplication4E_S2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E_S2_APP);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SUBSYSTEM_TYPE_APPLICATION);
		String appContent = BUNDLE_C + ", " + BUNDLE_D + ", " + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_4E_S2_APP + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E_S2_APP);
	}
	
	/* a feature S2 with
    - content bundles C, D and E */
	private static void createFeature4E_S2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E_S2_FEATURE);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		String appContent = BUNDLE_C + ", " + BUNDLE_D + ", " + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_4E_S2_FEATURE + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E_S2_FEATURE);
	}
	
	/* a composite S2 that 
	//     imports package x, requires bundle A and required capability y */
	private static void createComposite4E_S2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_4E_S2_COMP);
		attributes.put(SubsystemConstants.SUBSYSTEM_VERSION, "1.0.0");
		attributes.put(SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		String appContent = BUNDLE_C + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_D + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_E + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y"); 
		createManifest(SUBSYSTEM_4E_S2_COMP + ".mf", attributes);
		createSubsystem(SUBSYSTEM_4E_S2_COMP);
	}
}

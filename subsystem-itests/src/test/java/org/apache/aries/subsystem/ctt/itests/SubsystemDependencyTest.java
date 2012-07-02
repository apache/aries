package org.apache.aries.subsystem.ctt.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
- The following bundles are used for the tests
- Bundle A that export package x
- Bundle B that provides capability y
- Bundle C that imports package x
- Bundle D that requires bundle A
- Bundle E that requires capability y
- Bundle F that export package x
- Bundle G that provides capability y
- The following repositories are defined
- Repository R1
  - Bundle A
  - Bundle B
  - Bundle C
  - Bundle D
  - Bundle E
  - Bundle F
  - Bundle G
- Repository R2
  - Bundle A
  - Bundle B
  - Bundle C
  - Bundle D
  - Bundle E
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

/*
 * This is going to look a bit like ProvisionPolicyTest with a bit of 
 * DependencyLifecycle thrown in
 */

@RunWith(JUnit4TestRunner.class)
public class SubsystemDependencyTest extends SubsystemTest {

	private boolean _createdResources = false;
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		if (_createdResources)
			return;
		createBundleA();
		createBundleB();
		createBundleC();
		createBundleD();
		createBundleE();
		createBundleF();
		createBundleG();
		registerRepositoryR1();
		createTestApplication();
		_createdResources = true;
	}
	
	@After
	public void tearDown() 
	{ 
		super.tearDown();
	}
	
	@Test
	public void verifyBundleCWiredToPackageXFromBundleA() throws Exception
	{ 
		Subsystem s = installSubsystemFromFile(APPLICATION_A);
		startSubsystem(s);
		Bundle bundleC = getBundle(s, BUNDLE_C);
		assertNotNull ("bundleC not found", bundleC);
		
		BundleWiring wiring = bundleC.adapt(BundleWiring.class);
		List<BundleWire> wiredPackages = wiring.getRequiredWires(PACKAGE_NAMESPACE);
		assertEquals ("Only one package expected", 1, wiredPackages.size());
		
		String packageName = (String) 
			wiredPackages.get(0).getCapability().getAttributes().get(PACKAGE_NAMESPACE);
		assertEquals ("Wrong package found", "x", packageName);
		
		String providingBundle = wiredPackages.get(0).getProvider().getSymbolicName();
		assertEquals ("Provider A expected", BUNDLE_A, providingBundle);
 
		stopSubsystem(s);
	}
	
	@Test
	public void verifyBundleDWiredToBundleA() throws Exception
	{ 
		Subsystem s = installSubsystemFromFile(APPLICATION_A);
		startSubsystem(s);
		Bundle bundleD = getBundle(s, BUNDLE_D);
		assertNotNull ("bundleD not found", bundleD);
		
		BundleWiring wiring = bundleD.adapt(BundleWiring.class);
		List<BundleWire> wiredBundles = wiring.getRequiredWires(BUNDLE_NAMESPACE);
		assertEquals ("Only one bundle expected", 1, wiredBundles.size());
		
		String requiredBundleName = (String)
			wiredBundles.get(0).getCapability().getAttributes().get(BUNDLE_NAMESPACE);
		assertEquals ("Wrong bundle requirement", BUNDLE_A, requiredBundleName);
		
		String providingBundle = wiredBundles.get(0).getProvider().getSymbolicName();
		assertEquals ("Wrong bundle provider", BUNDLE_A, providingBundle);
		
		stopSubsystem(s);
	}
	
	@Test
	public void verifyBundleEWiredToCapability_yFromBundleB() throws Exception
	{
		Subsystem s = installSubsystemFromFile (APPLICATION_A);
		startSubsystem(s);
		Bundle bundleE = getBundle(s, BUNDLE_E);
		assertNotNull ("BundleE not found", bundleE);
		
		BundleWiring wiring = bundleE.adapt(BundleWiring.class);
		List<BundleWire> wiredProviders = wiring.getRequiredWires("y");
		assertEquals("Only one wire for capability y expected", 1, wiredProviders.size());
		
		String capabilityNamespace = (String)
			wiredProviders.get(0).getCapability().getNamespace();
		assertEquals ("Wrong namespace", "y", capabilityNamespace);
		
		String providingBundle = wiredProviders.get(0).getProvider().getSymbolicName();
		assertEquals ("Wrong bundle provider", BUNDLE_B, providingBundle);
		
		stopSubsystem(s);
	}
	
	@Test
	public void verifyNoUnexpectedBundlesProvisioned() 
	{ 
		// TODO
	}
	
	private static String BUNDLE_A = "sdt_bundle.a.jar";
	private void createBundleA() throws Exception
	{ 
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.EXPORT_PACKAGE, "x");
		createBundle(BUNDLE_A, headers);
	}
	
	private static String BUNDLE_B = "sdt_bundle.b.jar";
	private void createBundleB() throws Exception
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.PROVIDE_CAPABILITY, "y;bug=true"); // TODO: see comment below about bug=true
		createBundle(BUNDLE_B, headers);
	}
	
	private static String BUNDLE_C = "sdt_bundle.c.jar";
	private void createBundleC() throws Exception
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.IMPORT_PACKAGE, "x");
		createBundle(BUNDLE_C, headers);
	}
	
	private static String BUNDLE_D = "sdt_bundle.d.jar";
	private void createBundleD() throws Exception
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		createBundle(BUNDLE_D, headers);
	}
	
	private static String BUNDLE_E = "sdt_bundle.e.jar";
	private void createBundleE() throws Exception 
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.REQUIRE_CAPABILITY, "y;filter:=\"(bug=true)\"");
		// TODO:
		/*
		 * According to the OSGi Core Release 5 spec section 3.3.6 page 35, 
		 *   "A filter is optional, if no filter directive is specified the requirement 
		 *    always matches."
		 *  
		 * If omitted, we first get an NPE in DependencyCalculator.MissingCapability.initializeAttributes(). 
		 * If that's fixed, we get exceptions of the form, 
		 * 
		 *  Caused by: java.lang.IllegalArgumentException: The filter must not be null.
		 *  at org.eclipse.equinox.internal.region.StandardRegionFilterBuilder.allow(StandardRegionFilterBuilder.java:49)
		 *  at org.apache.aries.subsystem.core.internal.SubsystemResource.setImportIsolationPolicy(SubsystemResource.java:655)
	     * 
	     * This looks to be an Equinox defect - at least in the level of 3.8.0 currently being used by 
	     * these tests. 
		 */
		createBundle(BUNDLE_E, headers);
	}

	private static String BUNDLE_F = "sdt_bundle.f.jar";
	private void createBundleF() throws Exception 
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.EXPORT_PACKAGE, "x");
		createBundle(BUNDLE_F, headers);
	}
	
	private static String BUNDLE_G = "sdt_bundle.g.jar";
	private void createBundleG() throws Exception 
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.PROVIDE_CAPABILITY, "y;bug=true");      // TODO: see comment above about bug=true
		createBundle(BUNDLE_G, headers);
	}
	
	private void registerRepositoryR1() throws Exception
	{ 
		registerRepositoryService(BUNDLE_A, BUNDLE_B, 
				BUNDLE_C, BUNDLE_D, BUNDLE_E, BUNDLE_F, BUNDLE_G);
	}
	
	private static String APPLICATION_A="sdt_application.a.esa";
	private void createTestApplication() throws Exception
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
	
	/* This isn't working as a means of turning debug on for this test only
	@org.ops4j.pax.exam.junit.Configuration
	public static Option[] configuration(Option[] options) 
	{
		System.out.println ("SubsystemDependency.configuration() called");
		Option[] debugOptions = options(
				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777")
		);
		Option[] mainOptions = SubsystemTest.configuration();
		debugOptions = combine(mainOptions, debugOptions);
		return combine(debugOptions, options);
	}
	*/
}

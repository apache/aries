package org.apache.aries.subsystem.ctt.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.subsystem.itests.Header;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.subsystem.Subsystem;

/*
 * A set of tests to cover OSGi Subsystems CTT section 4, "Subsystem Dependency Tests"
 * This is going to look a bit like ProvisionPolicyTest with a bit of 
 * DependencyLifecycle thrown in. 
 * 
 * 	- The following bundles are used for the tests
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
 */

@ExamReactorStrategy(PerMethod.class)
public abstract class SubsystemDependencyTestBase extends SubsystemTest 
{
	protected static String BUNDLE_A = "sdt_bundle.a.jar";
	protected static String BUNDLE_B = "sdt_bundle.b.jar";
	protected static String BUNDLE_C = "sdt_bundle.c.jar";
	protected static String BUNDLE_D = "sdt_bundle.d.jar";
	protected static String BUNDLE_E = "sdt_bundle.e.jar";
	protected static String BUNDLE_F = "sdt_bundle.f.jar";
	protected static String BUNDLE_G = "sdt_bundle.g.jar";

	@Override
	protected void createApplications() throws Exception {
		// We'd like to do this in an @BeforeClass method, but files written in @BeforeClass
		// go into the project's target/ directory whereas those written in @Before go into 
		// paxexam's temp directory, which is where they're needed. 
		createBundleA();
		createBundleB();
		createBundleC();
		createBundleD();
		createBundleE();
		createBundleF();
		createBundleG();
	}
	
	private void createBundleA() throws Exception
	{ 
		createBundle(name(BUNDLE_A), version("1.0.0"), exportPackage("x"));
	}
	
	private void createBundleB() throws Exception
	{
		// TODO: see comment below about bug=true
		createBundle(name(BUNDLE_B), version("1.0.0"), new Header(Constants.PROVIDE_CAPABILITY, "y;y=randomNamespace"));
	}
	
	private void createBundleC() throws Exception
	{
		createBundle(name(BUNDLE_C), version("1.0.0"), importPackage("x"));
	}
	
	private void createBundleD() throws Exception
	{
		createBundle(name(BUNDLE_D), version("1.0.0"), requireBundle(BUNDLE_A));
	}

	// TODO:
	/*
	 * According to the OSGi Core Release 5 spec section 3.3.6 page 35, 
	 *   "A filter is optional, if no filter directive is specified the requirement always matches."
	 *  
	 * If omitted, we first get an NPE in DependencyCalculator.MissingCapability.initializeAttributes(). 
	 * If that's fixed, we get exceptions of the form, 
	 * 
	 *  Caused by: java.lang.IllegalArgumentException: The filter must not be null.
	 *    at org.eclipse.equinox.internal.region.StandardRegionFilterBuilder.allow(StandardRegionFilterBuilder.java:49)
	 *    at org.apache.aries.subsystem.core.internal.SubsystemResource.setImportIsolationPolicy(SubsystemResource.java:655)
     * 
     * This looks to be an Equinox defect - at least in the level of 3.8.0 currently being used by these tests. 
	 */
	private void createBundleE() throws Exception 
	{
		createBundle(name(BUNDLE_E), version("1.0.0"), new Header(Constants.REQUIRE_CAPABILITY, "y"));
	}

	private void createBundleF() throws Exception 
	{
		createBundle(name(BUNDLE_F), version("1.0.0"), exportPackage("x"));
	}
	
	// TODO: see comment above about bug=true
	private void createBundleG() throws Exception 
	{
		createBundle(name(BUNDLE_G), version("1.0.0"), new Header(Constants.PROVIDE_CAPABILITY, "y;y=randomNamespace"));
	}
	
	protected void registerRepositoryR1() throws Exception
	{ 
		registerRepositoryService(BUNDLE_A, BUNDLE_B, 
				BUNDLE_C, BUNDLE_D, BUNDLE_E, BUNDLE_F, BUNDLE_G);
	}
	
	protected void registerRepositoryR2() throws Exception
	{
		registerRepositoryService(BUNDLE_A, BUNDLE_B, 
				BUNDLE_C, BUNDLE_D, BUNDLE_E);
	}
	
	/**
	 *  - Verify that bundles C, D and E in subsystem s wire to A->x, A, B->y respectively
	 */
	protected void checkBundlesCDandEWiredToAandB (Subsystem s) 
	{
		verifySinglePackageWiring (s, BUNDLE_C, "x", BUNDLE_A);
		verifyRequireBundleWiring (s, BUNDLE_D, BUNDLE_A);
		verifyCapabilityWiring (s, BUNDLE_E, "y", BUNDLE_B);
	}

	/**
	 * Check that wiredBundleName in subsystem s is wired to a single package, 
	 * expectedPackage, from expectedProvidingBundle
	 * @param s
	 * @param wiredBundleName
	 * @param expectedPackage
	 * @param expectedProvidingBundle
	 */
	protected void verifySinglePackageWiring (Subsystem s, String wiredBundleName, String expectedPackage, String expectedProvidingBundle)
	{
		Bundle wiredBundle = context(s).getBundleByName(wiredBundleName);
		assertNotNull ("Bundle not found", wiredBundleName);

		BundleWiring wiring = wiredBundle.adapt(BundleWiring.class);
		List<BundleWire> wiredPackages = wiring.getRequiredWires(PACKAGE_NAMESPACE);
		assertEquals ("Only one package expected", 1, wiredPackages.size());
		
		String packageName = (String) 
			wiredPackages.get(0).getCapability().getAttributes().get(PACKAGE_NAMESPACE);
		assertEquals ("Wrong package found", expectedPackage, packageName);
		
		String providingBundle = wiredPackages.get(0).getProvider().getSymbolicName();
		assertEquals ("Package provided by wrong bundle", expectedProvidingBundle, providingBundle);
	}
	
	/**
	 * Verify that the Require-Bundle of wiredBundleName in subsystem s is met by a wire
	 * to expectedProvidingBundleName
	 * @param s
	 * @param wiredBundleName
	 * @param expectedProvidingBundleName
	 */
	protected void verifyRequireBundleWiring (Subsystem s, String wiredBundleName, String expectedProvidingBundleName)
	{
		Bundle wiredBundle = context(s).getBundleByName(BUNDLE_D);
		assertNotNull ("Target bundle " + wiredBundleName + " not found", wiredBundle);
	
		BundleWiring wiring = wiredBundle.adapt(BundleWiring.class);
		List<BundleWire> wiredBundles = wiring.getRequiredWires(BUNDLE_NAMESPACE);
		assertEquals ("Only one bundle expected", 1, wiredBundles.size());
	
		String requiredBundleName = (String)
			wiredBundles.get(0).getCapability().getAttributes().get(BUNDLE_NAMESPACE);
		assertEquals ("Wrong bundle requirement", BUNDLE_A, requiredBundleName);
	
		String providingBundle = wiredBundles.get(0).getProvider().getSymbolicName();
		assertEquals ("Wrong bundle provider", expectedProvidingBundleName, providingBundle);
	}
	
	/**
	 * Verify that a bundle with wiredBundleName imports a single capability in namespace
	 * from expectedProvidingBundleName
	 * @param s
	 * @param wiredBundleName
	 * @param namespace
	 * @param expectedProvidingBundleName
	 */
	protected void verifyCapabilityWiring (Subsystem s, String wiredBundleName, 
			String namespace, String expectedProvidingBundleName)
	{
		Bundle wiredBundle = context(s).getBundleByName(wiredBundleName);
		assertNotNull ("Targt bundle " + wiredBundleName + " not found", wiredBundleName);
		
		BundleWiring wiring = wiredBundle.adapt(BundleWiring.class);
		List<BundleWire> wiredProviders = wiring.getRequiredWires(namespace);
		assertEquals("Only one wire for capability namespace " + namespace +" expected", 
				1, wiredProviders.size());
		
		String capabilityNamespace = (String)
			wiredProviders.get(0).getCapability().getNamespace();
		assertEquals ("Wrong namespace", namespace, capabilityNamespace);
		
		String providingBundle = wiredProviders.get(0).getProvider().getSymbolicName();
		assertEquals ("Wrong bundle provider", expectedProvidingBundleName, providingBundle);
	}

	/**
	 * Verify that bundles with names bundleNames are installed into the subsystem with subsystemName
	 * and bundle context bc
	 * @param bc
	 * @param subsystemName
	 * @param bundleNames
	 */
	protected void verifyBundlesInstalled (BundleContext bc, String subsystemName, String ... bundleNames)
	{
		for (String bundleName: bundleNames) {
			boolean bundleFound = false;
			inner: for (Bundle b: bc.getBundles()) { 
				if (b.getSymbolicName().equals(bundleName)) { 
					bundleFound = true;
					break inner;
				}
			}
			assertTrue ("Bundle " + bundleName + " not found in subsystem " + subsystemName, bundleFound);
		}
	}
	
	/**
	 * Check that no new bundles have been provisioned by [x]
	 * @param failText where the failure occurred
	 * @param rootBundlesBefore Bundles before [x]
	 * @param rootBundlesAfter Bundles after [x]
	 */
	protected void checkNoNewBundles(String failText, Bundle[] rootBundlesBefore, Bundle[] rootBundlesAfter) {
		Set<String> bundlesBefore = new HashSet<String>();
		for (Bundle b : rootBundlesBefore) { 
			bundlesBefore.add(b.getSymbolicName() + "_" + b.getVersion().toString());
		}
		
		Set<String> bundlesAfter = new HashSet<String>();
		for (Bundle b : rootBundlesAfter) { 
			bundlesAfter.add(b.getSymbolicName() + "_" + b.getVersion().toString());
		}
		
		boolean unchanged = bundlesBefore.containsAll(bundlesAfter) && 
			bundlesAfter.containsAll(bundlesBefore);
		
		if (!unchanged) { 
			bundlesAfter.removeAll(bundlesBefore);
			fail ("Extra bundles provisioned in " + failText + " : " + bundlesAfter);
		}
	}
}

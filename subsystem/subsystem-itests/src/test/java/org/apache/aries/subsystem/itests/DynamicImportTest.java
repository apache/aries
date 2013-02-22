package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.JarFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ManifestFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

import aQute.lib.osgi.Constants;

/*
 * Simple iTest for dynamic imports. In the first instance we'll use a 
 * DynamicImport-Package header because it's the simplest to set up. 
 * _Hopefully_ if this works, then packages added by WeavingProxy services
 * will also work. If not, we'll need extra tests :-/ 
 */
@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class DynamicImportTest extends SubsystemTest 
{
	private static boolean _testAppCreated = false;
	
	@Before
	public void setUp() throws Exception 
	{
		super.setUp();
		if (!_testAppCreated) { 
			createApplication("dynamicImport", new String[]{"dynamicImport.jar"});
			createEmptyClass();
			createBundleA();
			createApplicationA();
			_testAppCreated = true;
		}
	}

	/*
	 * Install an .esa containing a bundle with a BundleActivator, and a 
	 * DynamicImport-Package on org.apache.aries.subsystem.itests.hello.api.
	 * This app should fail to start because we've not yet intervened to permit 
	 * this dynamic package wiring requirement from being met. 
	 */
	@Test
	public void verifyThatDynamicImportNeedsHandling() throws Exception
	{
		Subsystem subsystem = installSubsystemFromFile ("dynamicImport.esa");
		try { 
			startSubsystem(subsystem);
			Bundle[] bundles = subsystem.getBundleContext().getBundles();
			for (Bundle b : bundles) { 
				System.out.println (b.getSymbolicName() + " -> " + b.getState());
			}
			fail ("dynamicImport.esa started when we didn't expect it to");
		} catch (SubsystemException sx) { 
			Throwable cause = sx.getCause();
			assertTrue("BundleException expected", cause instanceof BundleException);
		}
	}
	
	class TokenWeaver implements WeavingHook {
		@Override
		public void weave(WovenClass arg0) {} 
	}
	
	@Test
	public void testFirstPassWeavingApproach() throws Exception
	{
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("osgi.woven.packages", "some.woven.package, org.apache.aries.subsystem.itests.hello.api");
		ServiceRegistration<?> sr = bundleContext.registerService(WeavingHook.class, new TokenWeaver(), props);
		try { 
			Subsystem subsystem = installSubsystemFromFile ("dynamicImport.esa");
			startSubsystem(subsystem);
		
			BundleContext bc = subsystem.getBundleContext();
			Hello h = getOsgiService(bc, Hello.class, null, DEFAULT_TIMEOUT);
			String message = h.saySomething();
			assertEquals ("Wrong message back", "Hello, this is something", message); // DynamicImportHelloImpl.java
		
			stopSubsystem(subsystem);
			uninstallSubsystem(subsystem);
		} finally { 
			sr.unregister();
		}
	}
	
	@Configuration
	public static Option[] extraBundles() 
	{
		return options(
				mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.itest.interfaces")
//				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777")
		);
	}
	
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private static void createBundleA() throws IOException {
		JarFixture bundle = ArchiveFixture.newJar();
		bundle.binary("Empty.class", new FileInputStream("Empty.class"));
		ManifestFixture manifest = bundle.manifest();
		manifest.attribute(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A);
		write(BUNDLE_A, bundle);
	}
	
	/*
	 * Dynamic package imports added by a weaver to a woven class should be
	 * added to the region's sharing policy even if the subsystem has no
	 * Import-Package header.
	 */
	@Test
	public void testDynamicPackageImportsAddedToSharingPolicyWhenNoImportPackageHeader() throws Exception {
		final AtomicBoolean weavingHookCalled = new AtomicBoolean(false);
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("osgi.woven.packages", "org.osgi.framework");
		ServiceRegistration<WeavingHook> reg = bundleContext.registerService(
				WeavingHook.class, 
				new WeavingHook() {
					@Override
					public void weave(WovenClass wovenClass) {
						if (BUNDLE_A.equals(wovenClass.getBundleWiring().getBundle().getSymbolicName())) {
							wovenClass.getDynamicImports().add("org.osgi.framework");
							weavingHookCalled.set(true);
						}
					}
				}, 
				props);
		try {
			Subsystem s = installSubsystemFromFile(APPLICATION_A);
			try {
				assertNull("Import-Package header should not exist", s.getSubsystemHeaders(null).get(Constants.IMPORT_PACKAGE));
				Bundle a = getConstituentAsBundle(s, BUNDLE_A, null, null);
				// Force the class load so the weaving hook gets called.
				a.loadClass("Empty");
				assertTrue("Weaving hook not called", weavingHookCalled.get());
				try {
					// Try to load a class from the dynamically imported package.
					a.loadClass("org.osgi.framework.Bundle");
				}
				catch (Exception e) {
					fail("Woven dynamic package import not added to the region's sharing policy");
				}
			}
			finally {
				try {
					s.uninstall();
				}
				catch (Exception e) {}
			}
		}
		finally {
			try {
				reg.unregister();
			}
			catch (Exception e) {}
		}
	}
}

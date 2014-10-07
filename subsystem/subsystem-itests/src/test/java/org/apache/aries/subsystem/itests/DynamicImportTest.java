package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.itest.RichBundleContext;
import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.JarFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ManifestFixture;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * Simple iTest for dynamic imports. In the first instance we'll use a 
 * DynamicImport-Package header because it's the simplest to set up. 
 * _Hopefully_ if this works, then packages added by WeavingProxy services
 * will also work. If not, we'll need extra tests :-/ 
 */
@ExamReactorStrategy(PerMethod.class)
public class DynamicImportTest extends SubsystemTest 
{
	@Override
	protected void createApplications() throws Exception {
		createApplication("dynamicImport", "dynamicImport.jar");
		createEmptyClass();
		createBundleA();
		createApplicationA();
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
	
	@SuppressWarnings("rawtypes")
    @Test
	public void testFirstPassWeavingApproach() throws Exception
	{
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("osgi.woven.packages", "some.woven.package, org.apache.aries.subsystem.itests.hello.api");
		ServiceRegistration sr = bundleContext.registerService(WeavingHook.class, new TokenWeaver(), props);
		try { 
			Subsystem subsystem = installSubsystemFromFile ("dynamicImport.esa");
			startSubsystem(subsystem);
		
			BundleContext bc = subsystem.getBundleContext();
			Hello h = new RichBundleContext(bc).getService(Hello.class);
			String message = h.saySomething();
			assertEquals ("Wrong message back", "Hello, this is something", message); // DynamicImportHelloImpl.java
		
			stopSubsystem(subsystem);
			uninstallSubsystem(subsystem);
		} finally { 
			sr.unregister();
		}
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
	@SuppressWarnings("rawtypes")
    @Test
	public void testDynamicPackageImportsAddedToSharingPolicyWhenNoImportPackageHeader() throws Exception {
		final AtomicBoolean weavingHookCalled = new AtomicBoolean(false);
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("osgi.woven.packages", "org.osgi.framework");
		ServiceRegistration reg = bundleContext.registerService(
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
	
	protected static final byte[] EMPTY_CLASS = new byte[] {
	     (byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe, 
	     (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x32, 
	     (byte)0x00, (byte)0x12, (byte)0x07, (byte)0x00, 
	     (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x05, 
	     (byte)0x45, (byte)0x6d, (byte)0x70, (byte)0x74, 
	     (byte)0x79, (byte)0x07, (byte)0x00, (byte)0x04, 
	     (byte)0x01, (byte)0x00, (byte)0x10, (byte)0x6a, 
	     (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2f, 
	     (byte)0x6c, (byte)0x61, (byte)0x6e, (byte)0x67, 
	     (byte)0x2f, (byte)0x4f, (byte)0x62, (byte)0x6a, 
	     (byte)0x65, (byte)0x63, (byte)0x74, (byte)0x07, 
	     (byte)0x00, (byte)0x06, (byte)0x01, (byte)0x00, 
	     (byte)0x14, (byte)0x6a, (byte)0x61, (byte)0x76, 
	     (byte)0x61, (byte)0x2f, (byte)0x69, (byte)0x6f, 
	     (byte)0x2f, (byte)0x53, (byte)0x65, (byte)0x72, 
	     (byte)0x69, (byte)0x61, (byte)0x6c, (byte)0x69, 
	     (byte)0x7a, (byte)0x61, (byte)0x62, (byte)0x6c, 
	     (byte)0x65, (byte)0x01, (byte)0x00, (byte)0x06, 
	     (byte)0x3c, (byte)0x69, (byte)0x6e, (byte)0x69, 
	     (byte)0x74, (byte)0x3e, (byte)0x01, (byte)0x00, 
	     (byte)0x03, (byte)0x28, (byte)0x29, (byte)0x56, 
	     (byte)0x01, (byte)0x00, (byte)0x04, (byte)0x43, 
	     (byte)0x6f, (byte)0x64, (byte)0x65, (byte)0x0a, 
	     (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x0b, 
	     (byte)0x0c, (byte)0x00, (byte)0x07, (byte)0x00, 
	     (byte)0x08, (byte)0x01, (byte)0x00, (byte)0x0f, 
	     (byte)0x4c, (byte)0x69, (byte)0x6e, (byte)0x65, 
	     (byte)0x4e, (byte)0x75, (byte)0x6d, (byte)0x62, 
	     (byte)0x65, (byte)0x72, (byte)0x54, (byte)0x61, 
	     (byte)0x62, (byte)0x6c, (byte)0x65, (byte)0x01, 
	     (byte)0x00, (byte)0x12, (byte)0x4c, (byte)0x6f, 
	     (byte)0x63, (byte)0x61, (byte)0x6c, (byte)0x56, 
	     (byte)0x61, (byte)0x72, (byte)0x69, (byte)0x61, 
	     (byte)0x62, (byte)0x6c, (byte)0x65, (byte)0x54, 
	     (byte)0x61, (byte)0x62, (byte)0x6c, (byte)0x65, 
	     (byte)0x01, (byte)0x00, (byte)0x04, (byte)0x74, 
	     (byte)0x68, (byte)0x69, (byte)0x73, (byte)0x01, 
	     (byte)0x00, (byte)0x07, (byte)0x4c, (byte)0x45, 
	     (byte)0x6d, (byte)0x70, (byte)0x74, (byte)0x79, 
	     (byte)0x3b, (byte)0x01, (byte)0x00, (byte)0x0a, 
	     (byte)0x53, (byte)0x6f, (byte)0x75, (byte)0x72, 
	     (byte)0x63, (byte)0x65, (byte)0x46, (byte)0x69, 
	     (byte)0x6c, (byte)0x65, (byte)0x01, (byte)0x00, 
	     (byte)0x0a, (byte)0x45, (byte)0x6d, (byte)0x70, 
	     (byte)0x74, (byte)0x79, (byte)0x2e, (byte)0x6a, 
	     (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x00, 
	     (byte)0x21, (byte)0x00, (byte)0x01, (byte)0x00, 
	     (byte)0x03, (byte)0x00, (byte)0x01, (byte)0x00, 
	     (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00, 
	     (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, 
	     (byte)0x07, (byte)0x00, (byte)0x08, (byte)0x00, 
	     (byte)0x01, (byte)0x00, (byte)0x09, (byte)0x00, 
	     (byte)0x00, (byte)0x00, (byte)0x2f, (byte)0x00, 
	     (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, 
	     (byte)0x00, (byte)0x00, (byte)0x05, (byte)0x2a, 
	     (byte)0xb7, (byte)0x00, (byte)0x0a, (byte)0xb1, 
	     (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02, 
	     (byte)0x00, (byte)0x0c, (byte)0x00, (byte)0x00, 
	     (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x01, 
	     (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x04, 
	     (byte)0x00, (byte)0x0d, (byte)0x00, (byte)0x00, 
	     (byte)0x00, (byte)0x0c, (byte)0x00, (byte)0x01, 
	     (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x05, 
	     (byte)0x00, (byte)0x0e, (byte)0x00, (byte)0x0f, 
	     (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, 
	     (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, 
	     (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x11
	};

	protected static void createEmptyClass() throws IOException {
	    FileOutputStream fos = new FileOutputStream("Empty.class");
	    fos.write(EMPTY_CLASS);
	    fos.close();
	}
}

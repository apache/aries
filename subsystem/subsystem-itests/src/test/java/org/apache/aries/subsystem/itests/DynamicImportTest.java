package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

/*
 * Simple iTest for dynamic imports. In the first instance we'll use a 
 * DynamicImport-Package header because it's the simplest to set up. 
 * _Hopefully_ if this works, then packages added by WeavingProxy services
 * will also work. If not, we'll need extra tests :-/ 
 */
@RunWith(JUnit4TestRunner.class)
public class DynamicImportTest extends SubsystemTest 
{
	private static boolean _testAppCreated = false;
	
	@Before
	public void setUp() throws Exception 
	{
		super.setUp();
		if (!_testAppCreated) { 
			createApplication("dynamicImport", new String[]{"dynamicImport.jar"});
			_testAppCreated = true;
		}
	}

	/*
	 * Install an .esa containing a bundle with a BundleActivator, and a 
	 * DynamicImport-Package on org.osgi.framework. This app should fail to 
	 * start unless we do something to stop the SubsystemResolverHook 
	 * from preventing this package from wiring. 
	 */
	@Test
	public void verifyThatDynamicImportNeedsHandling() throws Exception
	{
		
		System.out.println ("Into verifyThatDynamicImportNeedsHandling");
		
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
/*	
	@Test
	public void testHello() throws Exception 
	{
		Subsystem subsystem = installSubsystemFromFile("hello.esa");
		try {
			subsystem.start();
			BundleContext bc = subsystem.getBundleContext();
			Hello h = getOsgiService(bc, Hello.class, null, DEFAULT_TIMEOUT);
			String message = h.saySomething();
			assertEquals ("Wrong message back", "something", message);
			subsystem.stop();
		}
		finally {
			uninstallSubsystemSilently(subsystem);
		}
	} */
	
	protected static Option[] updateOptions(Option[] options) 
	{
		Option[] helloOptions = options(
				mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.itest.interfaces")
//				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
		);
		return combine(helloOptions, options);
	}
}

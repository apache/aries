package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.options;

import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.subsystem.Subsystem;

/*
 * iTest for blueprint with subsystems
 */
@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class BlueprintTest extends SubsystemTest 
{
	private static boolean _testAppCreated = false;
	
	@Before
	public void setUp() throws Exception 
	{
		super.setUp();
		if (!_testAppCreated) { 
			createApplication("blueprint", new String[]{"blueprint.jar"});
			_testAppCreated = true;
		}
	}

	@Test
	public void checkBlueprint() throws Exception
	{
		Subsystem subsystem = installSubsystemFromFile ("blueprint.esa");
		try { 
			startSubsystem(subsystem);
			BundleContext bc = subsystem.getBundleContext();
			Hello h = getOsgiService(bc, Hello.class, null, DEFAULT_TIMEOUT);
			String message = h.saySomething();
			assertEquals("Wrong message back", "messageFromBlueprint", message);
		} finally { 
			stopSubsystem(subsystem);
			uninstallSubsystem(subsystem);
		}
	}
	
	@Configuration
	public static Option[] extraBundles() 
	{
		return options(
				mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.itest.interfaces"),
		        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
		        mavenBundle("org.ow2.asm", "asm-all"),
		        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy")
//				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777")
		);
	}
}

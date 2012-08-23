package org.apache.aries.subsystem.itests;

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

/*
 * iTest for blueprint with subsystems
 */
@RunWith(JUnit4TestRunner.class)
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
				mavenBundle("org.apache.aries", "org.apache.aries.util"),
		        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
		        mavenBundle("org.ow2.asm", "asm-all"),
		        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy")
//				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7777")
		);
	}
}

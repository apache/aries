package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.subsystem.Subsystem;

@RunWith(JUnit4TestRunner.class)
public class HelloWorldTest extends InstallTest {
	
	@Before
	public void installTestApp() throws Exception 
	{
		createApplication("hello", new String[]{"helloImpl.jar"});
	}

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
	} 
	
	protected static Option[] updateOptions(Option[] options) 
	{
		Option[] helloOptions = options(
				mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.itest.interfaces")
//				org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
		);
		return combine(helloOptions, options);
	}
}

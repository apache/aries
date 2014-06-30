package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;

import org.apache.aries.itest.RichBundleContext;
import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.subsystem.Subsystem;

/*
 * iTest for blueprint with subsystems
 */
public class BlueprintTest extends SubsystemTest 
{
	@Override
	public void createApplications() throws Exception {
		createApplication("blueprint", "blueprint.jar");
	}

	@Test
	public void checkBlueprint() throws Exception
	{
		Subsystem subsystem = installSubsystemFromFile ("blueprint.esa");
		try { 
			startSubsystem(subsystem);
			BundleContext bc = subsystem.getBundleContext();
			Hello h = new RichBundleContext(bc).getService(Hello.class);
			String message = h.saySomething();
			assertEquals("Wrong message back", "messageFromBlueprint", message);
		} finally { 
			stopSubsystem(subsystem);
			uninstallSubsystem(subsystem);
		}
	}
	
}

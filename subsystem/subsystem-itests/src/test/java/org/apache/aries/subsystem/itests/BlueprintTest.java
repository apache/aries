package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.apache.aries.itest.RichBundleContext;
import org.apache.aries.subsystem.itests.bundles.blueprint.BPHelloImpl;
import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;

/*
 * iTest for blueprint with subsystems
 */
public class BlueprintTest extends SubsystemTest 
{
    private static final String BLUEPRINT_ESA = "target/blueprint.esa";

    protected void init() throws Exception {
        writeToFile(createBlueprintEsa(), BLUEPRINT_ESA);
    }

	@Test
	public void checkBlueprint() throws Exception
	{
	    Subsystem subsystem = installSubsystemFromFile(BLUEPRINT_ESA);
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

    private InputStream createBlueprintEsa() throws Exception {
	    return TinyBundles.bundle()
	        .add("OSGI-INF/SUBSYSTEM.MF", getResource("blueprint/OSGI-INF/SUBSYSTEM.MF"))
	        .add("blueprint.jar", createBlueprintTestBundle())
	        .build(TinyBundles.withBnd());
    }

    private InputStream createBlueprintTestBundle() {
        return TinyBundles.bundle()
	        .add(BPHelloImpl.class)
	        .add("OSGI-INF/blueprint/blueprint.xml", getResource("blueprint/OSGI-INF/blueprint/blueprint.xml"))
	        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.subsystem.itests.blueprint")
	        .build(TinyBundles.withBnd());
    }
	
}

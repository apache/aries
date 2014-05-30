package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertNull;

import org.apache.aries.itest.RichBundleContext;
import org.junit.Test;
import org.osgi.service.subsystem.Subsystem;

public class ModelledResourceManagerTest extends SubsystemTest {
	
	public ModelledResourceManagerTest() {
		super(false);
	}
	
	@Override
	public void createApplications() throws Exception {
		createApplication("feature3", new String[]{"tb3.jar"});
		createApplication("application1", new String[]{"tb1.jar"});
	}
	
	public void setUp() throws Exception {
		super.setUp();
		RichBundleContext rootContext = context(getRootSubsystem());
		assertNull("Modeller is installed", rootContext.getBundleByName("org.apache.aries.application.modeller"));
		assertNull("Blueprint is installed", rootContext.getBundleByName("org.apache.aries.blueprint"));
		assertNull("Proxy is installed", rootContext.getBundleByName("org.apache.aries.proxy"));
	}

	@Test
	public void testNoModelledResourceManagerService() throws Exception {
		Subsystem feature3 = installSubsystemFromFile("feature3.esa");
		try {
			Subsystem application1 = installSubsystemFromFile("application1.esa");
			try {
				startSubsystem(application1);
			}
			finally {
				stopAndUninstallSubsystemSilently(application1);
			}
		}
		finally {
			uninstallSubsystemSilently(feature3);
		}
	}
}

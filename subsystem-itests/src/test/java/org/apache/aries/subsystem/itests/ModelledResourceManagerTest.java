package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.service.subsystem.Subsystem;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class ModelledResourceManagerTest extends SubsystemTest {
	@org.ops4j.pax.exam.junit.Configuration
	public static Option[] configuration() {
		List<Option> options = new ArrayList<Option>(Arrays.asList(defineOptions()));
		for (Iterator<Option> i = options.iterator(); i.hasNext();) {
			Option option = i.next();
			if (option instanceof MavenArtifactProvisionOption) {
				MavenArtifactProvisionOption mapo = (MavenArtifactProvisionOption)option;
				String url = mapo.getURL();
				if (url.contains("org.apache.aries.application.modeller")
						|| url.contains("org.apache.aries.blueprint")
						|| url.contains("org.apache.aries.proxy")) {
					i.remove();
				}
			}
		}
		Option[] result = options.toArray(new Option[options.size()]);
		result = updateOptions(result);
		return result;
	}
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		createApplication("feature3", new String[]{"tb3.jar"});
		createApplication("application1", new String[]{"tb1.jar"});
		createdApplications = true;
	}
	
	public void setUp() throws Exception {
		super.setUp();
		assertNull("Modeller is installed", getBundle(getRootSubsystem(), "org.apache.aries.application.modeller"));
		assertNull("Blueprint is installed", getBundle(getRootSubsystem(), "org.apache.aries.blueprint"));
		assertNull("Proxy is installed", getBundle(getRootSubsystem(), "org.apache.aries.proxy"));
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

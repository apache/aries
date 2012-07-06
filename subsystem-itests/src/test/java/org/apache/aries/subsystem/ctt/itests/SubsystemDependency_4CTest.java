package org.apache.aries.subsystem.ctt.itests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemDependency_4CTest extends SubsystemDependencyTestBase 
{
	/*
	 C) Test with pre-installed transitive resources
     - Register repository R1
     - Using the Root subsystem, install a composite subsystem S1 with the following content bundles (with no import/export policy)
       - Bundle A
       - Bundle B
     - Using the subsystem S1, install a composite S2 that imports package x, requires bundle A and required capability y
     - Verify the wiring of C, D and E wire to A->x, A, B->y respectively 
     - Verify no new bundles are installed into the Root or S1 subsystems
	 */
	private static final String SUBSYSTEM_S1 = "sdt_composite.s1.esa";
	
	private static boolean _testSubsystemCreated = false;
	@Before
	public void setUp() throws Exception
	{ 
		super.setUp();
		if (!_testSubsystemCreated) { 
			createSubsystemS1();
			_testSubsystemCreated = true;
		}
	}
	
	@Test
	public void verifyCinS1WiresToAxInS2() 
	{
		
	}
	
	private static void createSubsystemS1() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_S1);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		String appContent = BUNDLE_C + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_D + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_E + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y;filter:=\"(bug=true)\"");
		createManifest(SUBSYSTEM_S1 + ".mf", attributes);
		createSubsystem(SUBSYSTEM_S1);
	}

}

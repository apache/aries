package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.itest.RichBundleContext;
import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IDirectoryFinder;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class HelloWorldTest extends SubsystemTest 
{

	/*
	 * An implementation of the IDirectoryFinder interface that provides the
	 * IDirectory that corresponds to some id URI. In practice this could come 
	 * from anywhere (exploded archive on filesystem, in-memory, IDE etc) but 
	 * for the test just use an archive file. 
	 */
  static class TestIDirectoryFinder implements IDirectoryFinder {
    static final String IDIR_FINDERID_VALUE = "TestIDirectoryFinder";
    static final String IDIR_DIRECTORYID_VALUE = "hello.esa";
    static final URI HELLO_ID_URI = 
     URI.create(IDIR_SCHEME + "://?" + IDIR_FINDERID_KEY + "=" + IDIR_FINDERID_VALUE
      + "&" + IDIR_DIRECTORYID_KEY + "=" + IDIR_DIRECTORYID_VALUE);
    static final String HELLO_ID_STRING = HELLO_ID_URI.toString();
    
    public IDirectory retrieveIDirectory(URI idirectoryId) {
      if (HELLO_ID_URI.equals(idirectoryId)) {
        File helloEsaFile = new File("hello.esa");
        IDirectory helloEsaIDir = FileSystem.getFSRoot(helloEsaFile);
        return helloEsaIDir;
      } else {
        return null;
      }
    }
  }

	@Override
	public void createApplications() throws Exception {
		createApplication("hello", "helloImpl.jar");
	}

	void checkHelloSubsystem(Subsystem helloSubsystem) throws Exception
	{
    helloSubsystem.start();
    BundleContext bc = helloSubsystem.getBundleContext();
    Hello h = new RichBundleContext(bc).getService(Hello.class);
    String message = h.saySomething();
    assertEquals ("Wrong message back", "something", message);
    helloSubsystem.stop();
	}
	
	@Test
	public void testHelloFromFile() throws Exception 
	{
		Subsystem subsystem = installSubsystemFromFile("hello.esa");
		try {
		  checkHelloSubsystem(subsystem);
		} finally {
			uninstallSubsystemSilently(subsystem);
		}
	} 
	
  @Test
  public void testHelloFromIDirectory() throws Exception 
  {
    // Sanity check, application should not install if no IDirectoryFinder 
    // services are registered, which should be the case on entry to this test.
    try {
      installSubsystem(getRootSubsystem(), TestIDirectoryFinder.HELLO_ID_STRING, null);
      fail("installed esa application from idir without an idirfinder service, shouldn't be possible.");
    } catch (SubsystemException se) {
      // expected exception
    }
    
    // The root subsystem already exists and has a service tracker for
    // IDirectoryFinder services, so it will be notified on service registration. 
    Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put(IDirectoryFinder.IDIR_FINDERID_KEY, TestIDirectoryFinder.IDIR_FINDERID_VALUE);
    ServiceRegistration serviceRegistration =
     bundleContext.registerService(IDirectoryFinder.class, new TestIDirectoryFinder(), properties);
    
    // Call the SubsystemTest.installSubsystem method that does not create a URL
    // and stream from the location, as we just need the location string passed 
    // through to the installing root subsystem.
    Subsystem subsystem = installSubsystem(getRootSubsystem(), TestIDirectoryFinder.HELLO_ID_STRING, null);    
    try {
      checkHelloSubsystem(subsystem);
    } finally {
      uninstallSubsystemSilently(subsystem);
      if (serviceRegistration!=null)
        serviceRegistration.unregister();
    }
  } 
  
}

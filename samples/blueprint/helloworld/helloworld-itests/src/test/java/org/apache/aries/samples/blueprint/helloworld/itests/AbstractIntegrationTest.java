/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.blueprint.helloworld.itests;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.url.mvn.Handler;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


public abstract class AbstractIntegrationTest {

    private static final int CONNECTION_TIMEOUT = 30000;
    public static final long DEFAULT_TIMEOUT = 60000;

	@Inject
  	protected BundleContext bundleContext;

    private List<ServiceTracker> srs;

    @Before
    public void setUp() {
        srs = new ArrayList<ServiceTracker>();
    }
    
    @After
    public void tearDown() throws Exception{
        for (ServiceTracker st : srs) {
            if (st != null) {
                st.close();
            }  
        }
    }

	public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
    }

    public static MavenArtifactProvisionOption mavenBundleInTest(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).version(getArtifactVersion(groupId, artifactId));
    }

	public static String getArtifactVersion( final String groupId,
                                             final String artifactId )
    {
        final Properties dependencies = new Properties();
        try
        {
            InputStream in = getFileFromClasspath("META-INF/maven/dependencies.properties");
            try {
                dependencies.load(in);
            } finally {
                in.close();
            }
            final String version = dependencies.getProperty( groupId + "/" + artifactId + "/version" );
            if( version == null )
            {
                throw new RuntimeException(
                    "Could not resolve version. Do you have a dependency for " + groupId + "/" + artifactId
                    + " in your maven project?"
                );
            }
            return version;
        }
        catch( IOException e )
        {
            // TODO throw a better exception
            throw new RuntimeException(
             "Could not resolve version. Did you configured the plugin in your maven project?"
             + "Or maybe you did not run the maven build and you are using an IDE?"
            );
        }
     }


	protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }

	private static InputStream getFileFromClasspath( final String filePath )
        throws FileNotFoundException
    {
        try
        {
            URL fileURL = AbstractIntegrationTest.class.getClassLoader().getResource( filePath );
            if( fileURL == null )
            {
                throw new FileNotFoundException( "File [" + filePath + "] could not be found in classpath" );
            }
            return fileURL.openStream();
        }
        catch (IOException e)
        {
            throw new FileNotFoundException( "File [" + filePath + "] could not be found: " + e.getMessage() );
        }
    }

	protected void listBundleServices(Bundle b) {
    	ServiceReference []srb = b.getRegisteredServices();
		for(ServiceReference sr:srb){
			System.out.println(b.getSymbolicName() + " SERVICE: "+sr);
    	}	
	}

	protected Boolean isServiceRegistered(Bundle b) {
		ServiceReference []srb = b.getRegisteredServices();
		if(srb == null) {
			return false;
		}
    	return true;
	}

    protected void waitForServices(Bundle b, String sclass) {
		try {
			BundleContext bc = b.getBundleContext();
    		String bsn = b.getSymbolicName();
			ServiceTracker st = new ServiceTracker(bc, sclass, null);
    		st.open();
    		Object bac = st.waitForService(DEFAULT_TIMEOUT);
			/* Uncomment for debug */
			/*
			if(bac == null) {
				System.out.println("SERVICE NOTFOUND " + bsn);
			} else {
				System.out.println("SERVICE FOUND " + bsn);
			}
			*/
			st.close();
			return;
		} 
		catch (Exception e) {
			System.out.println("Failed to register services for " + b.getSymbolicName() + e.getMessage());	
		}
	}


	protected static Option[] updateOptions(Option[] options) {
	if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            Option[] ibmOptions = options(
                wrappedBundle(mavenBundle("org.ops4j.pax.exam", "pax-exam-junit"))
            );
            options = combine(ibmOptions, options);
        }

        return options;
    }

  public static String getHTTPResponse(HttpURLConnection conn) throws IOException
  {
    StringBuilder response = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
        "ISO-8859-1"));
    try {
      for (String s = reader.readLine(); s != null; s = reader.readLine()) {
        response.append(s).append("\r\n");
      }
    } finally {
      reader.close();
    }

    return response.toString();
  }

  public static HttpURLConnection makeConnection(String contextPath) throws IOException
  {
    URL url = new URL(contextPath);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setConnectTimeout(CONNECTION_TIMEOUT);
    conn.connect();

    return conn;
  }
  
  protected <T> T getOsgiService(Class<T> type, long timeout) {
      return getOsgiService(type, null, timeout);
  }

  protected <T> T getOsgiService(Class<T> type) {
    return getOsgiService(type, null, DEFAULT_TIMEOUT);
  }
    
  protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
    return getOsgiService(null, type, filter, timeout);
  }

  protected <T> T getOsgiService(BundleContext bc, Class<T> type,
      String filter, long timeout) {
    ServiceTracker tracker = null;
    try {
      String flt;
      if (filter != null) {
        if (filter.startsWith("(")) {
          flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")"
              + filter + ")";
        } else {
          flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")("
              + filter + "))";
        }
      } else {
        flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
      }
      Filter osgiFilter = FrameworkUtil.createFilter(flt);
      tracker = new ServiceTracker(bc == null ? bundleContext : bc, osgiFilter,
          null);
      tracker.open();
      
      // add tracker to the list of trackers we close at tear down
      srs.add(tracker);
      Object x = tracker.waitForService(timeout);
      Object svc = type.cast(x);
      if (svc == null) {
        throw new RuntimeException("Gave up waiting for service " + flt);
      }
      return type.cast(svc);
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid filter", e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static URL getUrlToEba(String groupId, String artifactId) throws MalformedURLException {
    String artifactVersion = getArtifactVersion(groupId, artifactId);

    // Need to use handler from org.ops4j.pax.url.mvn
    URL urlToEba = new URL(null,
        ServiceConstants.PROTOCOL + ":" + groupId + "/" +artifactId + "/"
            + artifactVersion + "/eba", new Handler());
    return urlToEba;
  }

}

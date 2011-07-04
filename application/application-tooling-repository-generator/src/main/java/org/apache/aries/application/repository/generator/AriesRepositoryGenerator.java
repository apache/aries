/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.application.repository.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.apache.aries.application.management.spi.repository.RepositoryGenerator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class AriesRepositoryGenerator {

  private Framework framework = null;
  private List<String> ignoreList = new ArrayList<String>();
  private List<ServiceTracker> srs = new ArrayList<ServiceTracker>();
  public static final long DEFAULT_TIMEOUT = 60000; 
  public static final String ERROR_LEVEL = "ERROR";
  public static final String DEFAULT_REPO_NAME="repository.xml";

  /**
   * Start OSGi framework and install the necessary bundles
   * @return
   * @throws BundleException
   */
  public BundleContext startFramework() throws BundleException{
    ServiceLoader<FrameworkFactory> factoryLoader =
      ServiceLoader.load(FrameworkFactory.class, getClass().getClassLoader());

    Iterator<FrameworkFactory> factoryIterator = factoryLoader.iterator();
    //Map<String, String> osgiPropertyMap = new HashMap<String, String>();
    if (!!!factoryIterator.hasNext()) {
      System.out.println( "Unable to locate the osgi jar");
    }
    
    try {
      FrameworkFactory frameworkFactory = factoryIterator.next();
      framework = frameworkFactory.newFramework(Collections.EMPTY_MAP);
    } catch (ServiceConfigurationError sce) {
      sce.printStackTrace();
    }
    framework.init();
    framework.start();
    // install the bundles in the current directory
    Collection<Bundle> installedBundles = new ArrayList<Bundle>();
    File bundleDir = new File(".");
    File[] jars = bundleDir.listFiles();
    try {
      for (File jar : jars) {
        if (jar.isFile() && (jar.getName().endsWith(".jar"))) {
          String location = URLDecoder.decode(jar.toURI().toURL().toExternalForm(), "UTF-8");
          if (shouldInstall(location, getIgnoreList())) {
            installedBundles.add(framework.getBundleContext().installBundle(
                "reference:" + location));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    ServiceReference paRef = framework.getBundleContext().getServiceReference(PackageAdmin.class.getCanonicalName());
    if (paRef != null) {
      try {
        PackageAdmin admin = (PackageAdmin) framework.getBundleContext().getService(paRef);
        admin.resolveBundles(installedBundles.toArray(new Bundle[installedBundles.size()]));
      } finally {
        framework.getBundleContext().ungetService(paRef);
      }
    } else {
      System.out.println("Unable to find the service reference for package admin");
    }


    //start the bundles
    //loop through the list of installed bundles to start them
    for (Bundle bundle : installedBundles) {
      try {
        if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
          //start the bundle using its activiation policy
          bundle.start(Bundle.START_ACTIVATION_POLICY);
        } else {
          //nothing to start, we have got a fragment
        }
      } catch (BundleException e) {
        e.printStackTrace();
        continue;
      }
    }
    return framework.getBundleContext();
  }

  private  void stopFramework() throws Exception{
    for (ServiceTracker st : srs) {
      if (st != null) {
        st.close();
      }  
    }
    if (framework != null) {
      framework.stop();
    }
  }
  private Object getOsgiService(BundleContext bc, String className) {
    ServiceTracker tracker = null;
    try {
      String flt = "(" + Constants.OBJECTCLASS + "=" + className + ")";
      Filter osgiFilter = FrameworkUtil.createFilter(flt);
      tracker = new ServiceTracker(bc, osgiFilter, null);
      tracker.open();
      // add tracker to the list of trackers we close at tear down
      srs.add(tracker);
      Object x = tracker.waitForService(DEFAULT_TIMEOUT);
      if (x == null) {
        throw new RuntimeException("Gave up waiting for service " + flt);
      }
      return x;
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid filter", e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void usage() {
    System.out.println("Invalid parameter specifed. See program usage below.");
    System.out.println("========================= Usage ===============================");
    System.out.println("Parameter list:  [Reporsitory File Location] url1 [url2 url3 ...]");
    System.out.println();
    System.out.println("The parameter of the repository file location is the location for the genenerated reporsitory xml, e.g. /test/rep/repo.xml. It must end with .xml. If the parameter is not present, it will generate a repository.xml in the current directory.");
    System.out.println();
    System.out.println("The paremater of url1 [url2 url 3 ...] is a list of urls. If the url starts with file:, it can be a directory, which means all jar or war files in that directory to be included in the reposiotry.");
    System.out.println("===============================================================");
  }
  /**
   * Execute this program using 
   * java -jar thisbundlename arg0 arg1...
   * arg0 can be the location of the repository xml or a url
   * arg1... a list of url. If there is a file url, all jar/wars under the url are to be included.
   * e.g. file:///c:/temp/, all jars/wars under temp or its subdirectory are to be included.
   * @param args
   */
  public static void main(String[] args) {
    String[] urlArray = args;
    if (args.length == 0){
      usage();
      System.exit(0);
    } else {
      AriesRepositoryGenerator generator = new AriesRepositoryGenerator();
      
      // set the system property for logging if not set in the command line
      String loggerLevelProp = "org.ops4j.pax.logging.DefaultServiceLog.level";
      if (System.getProperty(loggerLevelProp) == null) {
        System.setProperty(loggerLevelProp, ERROR_LEVEL);
      }
      FileOutputStream fout = null;
      try {
        BundleContext ctx = generator.startFramework();
        // get the object of repositoryGenerator and call its method
        
        File xmlFile = new File(DEFAULT_REPO_NAME);
        if (args[0].endsWith(".xml")) {
           xmlFile = new File(args[0]);
          // get the directors
          File parentDir = xmlFile.getAbsoluteFile().getParentFile();
          if (!!!parentDir.exists()) {
            parentDir.mkdirs();
          }

          // put the rest of the args to the list of urls
          if (args.length >1) {
            urlArray = Arrays.copyOfRange(args, 1, args.length);
          } else {
            usage();
            System.exit(0);
          }
        } 
        // Use reflection to get around the class loading issue
        fout = new FileOutputStream(xmlFile);
        Object repoGen = generator.getOsgiService(ctx, RepositoryGenerator.class.getName());
        Class gen= repoGen.getClass();
        Method m = gen.getDeclaredMethod("generateRepository", new Class[]{String[].class, OutputStream.class});
        m.invoke(repoGen, urlArray, fout);
        generator.stopFramework();
        System.out.println("The reporsitory xml was generated successfully under " + xmlFile.getAbsolutePath() + ".");

      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      } finally {
        try {
          if (fout != null)
            fout.close();
        }
        catch (IOException e) {
          fout = null;
        }
      }
    }
  }

  /**
   * Return whether to install the specified bundle
   * @param location bundle location
   * @param ignoreList the ignore list containing the bundles to be ignored
   * @return
   */
  private boolean shouldInstall(String location, List<String> ignoreList)
  {
    String name = location.substring(location.lastIndexOf('/') + 1);
    //check that it isn't one of the files we should ignore
    boolean inIgnoreList = false;
    for (String prefix : ignoreList) {
      //we check for _ in case of version and .jar in case of a prefix.*.jar we should allow
      if (name.startsWith(prefix + "-") || name.startsWith(prefix + ".jar")) {
        inIgnoreList = true;
        break;
      }
    }
    return !!!inIgnoreList;
  }

  private List<String> getIgnoreList() {
    ignoreList.add("osgi");
    ignoreList.add("org.apache.aries.application.obr.generator");
    return ignoreList;
  }
}

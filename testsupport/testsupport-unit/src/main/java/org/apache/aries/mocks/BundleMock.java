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
package org.apache.aries.mocks;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.MethodCallHandler;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.unittest.mocks.annotations.Singleton;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

@Singleton
public class BundleMock
{
  private final String symbolicName;
  private final Dictionary<?, ?> headers;
  private final BundleContext bc;
  private String location;
  private BundleClassLoader cl;
  
  private class BundleClassLoader extends URLClassLoader implements BundleReference
  {
    List<Bundle> otherBundlesToCheck = new ArrayList<Bundle>();
    
    public BundleClassLoader(URL[] urls)
    {
      super(urls);
    }

    public BundleClassLoader(URL[] urls, ClassLoader parent)
    {
      super(urls, parent);
    }
    
    public void addBundle(Bundle ... otherBundles)
    {
      otherBundlesToCheck.addAll(Arrays.asList(otherBundles));
    }
    
    public Bundle[] getBundles()
    {
      return otherBundlesToCheck.toArray(new Bundle[otherBundlesToCheck.size()]);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
      Class<?> result = null;
      for (Bundle b : otherBundlesToCheck) {
        try {
          result = b.loadClass(name);
        } catch (ClassNotFoundException e) {
          // do nothing here.
        }
        
        if (result != null) return result;
      }
      
      return super.findClass(name);
    }

    public Bundle getBundle()
    {
      return Skeleton.newMock(BundleMock.this, Bundle.class);
    }
  }
  
  public BundleMock(String name, Dictionary<?, ?> bundleHeaders)
  {
    symbolicName = name;
    headers = bundleHeaders;
    bc = Skeleton.newMock(new BundleContextMock(Skeleton.newMock(this, Bundle.class)), BundleContext.class);
    
    cl = AccessController.doPrivileged(new PrivilegedAction<BundleClassLoader>() {

      public BundleClassLoader run()
      {
        return new BundleClassLoader(new URL[0], this.getClass().getClassLoader());
      }
    });
  }
  
  public BundleMock(String name, Dictionary<?,?> bundleHeaders, boolean dummy)
  {
    this(name, bundleHeaders);
    
    cl = null;
  }
  
  public BundleMock(String name, Dictionary<?, ?> bundleHeaders, String location)
  {
    this(name, bundleHeaders);
    this.location = location;
        
    if (location != null) {
      String cp = (String)bundleHeaders.get(Constants.BUNDLE_CLASSPATH);
      
      if (cp == null) cp = ".";
      
      String[] cpEntries = cp.split(",");
      
      final List<URL> urls = new ArrayList<URL>();
      
      try {
        for (String cpEntry : cpEntries) {
          if (".".equals(cpEntry.trim())) {
            urls.add(new URL(location));
          } else {
            urls.add(new URL(location + "/" + cpEntry));
          }
        }
        
        cl = AccessController.doPrivileged(new PrivilegedAction<BundleClassLoader>() {
          public BundleClassLoader run()
          {
            return new BundleClassLoader(urls.toArray(new URL[urls.size()]));
          }
        });
      } catch (MalformedURLException e) {
        Error err = new AssertionFailedError("The location was not a valid url");
        err.initCause(e);
        throw err;
      }
    }
  }
  
  private static class PrivateDataFileHandler implements MethodCallHandler
  {
    private final File location;
    
    public PrivateDataFileHandler(File f)
    {
      this.location = f;
    }
    
    public Object handle(MethodCall call, Skeleton parent)
    {
      File privateStorage = new File(location.getAbsolutePath(), "_private");
      if (!!!privateStorage.exists())
        privateStorage.mkdirs();
      
      return new File(privateStorage, (String) call.getArguments()[0]);
    }
  }
  
  public BundleMock(String name, Dictionary<?, ?> properties, File location) throws Exception
  {
    this(name,properties,location.toURL().toExternalForm());
    
    Skeleton bcSkel = Skeleton.getSkeleton(bc);
    bcSkel.registerMethodCallHandler(
        new MethodCall(BundleContext.class,"getDataFile", new Object[] { String.class }),
        new PrivateDataFileHandler(location)
    );
  }
  
  public String getSymbolicName()
  {
    return symbolicName;
  }
  
  public Dictionary<?, ?> getHeaders()
  {
    return headers;
  }
  
  public Enumeration<URL> findEntries(String baseDir, String matchRule, boolean recurse)
  {
    System.err.println("findEntries: " + baseDir + ", " + matchRule + ", " + recurse);
    File base;
    try {
      base = new File(new File(new URL(location.replaceAll(" ", "%20")).toURI()), baseDir);
      System.err.println("Base dir: " + base);
    } catch (Exception e) {
      Error err = new AssertionFailedError("Unable to findEntries from " + location);
      err.initCause(e);
      throw err;
    }
    
    if (matchRule.equals("*.xml")) matchRule = ".*\\.xml";
    else matchRule = matchRule.replaceAll("\\*", ".*");
    
    System.err.println("matchrule: " + matchRule);
    
    final Pattern p = Pattern.compile(matchRule);
    
    File[] files = base.listFiles(new FileFilter(){
      public boolean accept(File pathname)
      {
        return pathname.isFile() &&
               p.matcher(pathname.getName()).matches();
      }
    });
    
    Vector<URL> v = new Vector<URL>();
    
    if (files != null) {
      for (File f : files) {
        try {
          v.add(f.toURL());
        } catch (MalformedURLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    } else {
      System.err.println("no matching files");
    }
    
    if (v.isEmpty()) {
      return null;
    } else {
      System.err.println(v);
      return v.elements();
    }
  }
  
  public URL getResource(String name)
  {
    if (cl != null) return cl.getResource(name);
    
    try {
      File f = new File(name);
      if(f.exists() || "Entities.jar".equals(name)) return f.toURL();
      else return null;
    } catch (MalformedURLException e) {
      Error err = new AssertionFailedError("The resource " + name + " could not be found.");
      err.initCause(e);
      throw err;
    }
  }
  
  public Enumeration<URL> getResources(String name)
  {
    if (cl != null)
    {
      try {
        return cl.getResources(name);
      } catch (IOException e) {
      // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    else {
      final URL resource = getResource(name);
      
      if(resource != null) {
        return new Enumeration<URL>() {

          boolean hasMore = true;
          public boolean hasMoreElements()
          {
            return hasMore;
          }

          public URL nextElement()
          {
            hasMore = false;
            return resource;
          }
          
        };
      }
    }
    return new Enumeration<URL>(){
      public URL nextElement()
      {
        return null;
      }

      public boolean hasMoreElements()
      {
        return false;
      }
    };
    
  }
  
  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    if (cl != null) return Class.forName(name, false, cl);
    
    throw new ClassNotFoundException("Argh, things went horribly wrong trying to load " + name);
  }
  
  public String getLocation()
  {
    try {
      return (location == null) ? new File(symbolicName + ".jar").toURL().toString() : location;
    } catch (MalformedURLException e) {
      Error err = new AssertionFailedError("We could not generate a valid url for the bundle");
      err.initCause(e);
      throw err;
    }
  }
  
  public BundleContext getBundleContext()
  {
    return bc;
  }

  public Version getVersion()
  {
    String res = (String) headers.get("Bundle-Version");
    if (res != null)
      return new Version(res);
    else
      return new Version("0.0.0");
  }
  
  public int getState()
  {
    return Bundle.ACTIVE;
  }

  public void addToClassPath(URL ... urls)
  {
    if (cl != null) {
      URL[] existingURLs = cl.getURLs();
      final URL[] mergedURLs = new URL[urls.length + existingURLs.length];
      int i = 0;
      for (; i < existingURLs.length; i++) {
        mergedURLs[i] = existingURLs[i];
      }
      
      for (int j = 0; j < urls.length; j++, i++) {
        mergedURLs[i] = urls[j];
      }
      
      BundleClassLoader newCl = AccessController.doPrivileged(new PrivilegedAction<BundleClassLoader>() {

        public BundleClassLoader run()
        {
          return new BundleClassLoader(mergedURLs, cl.getParent());
        }
        
      });
      newCl.addBundle(cl.getBundles());
      cl = newCl;
    }
  }
    
  public void addBundleToClassPath(Bundle ... bundles) {
    if (cl != null) {
      cl.addBundle(bundles);
    }
  }

  public ClassLoader getClassLoader()
  {
    return cl;
  }
  
  // This is good enough for Mocks' needs in unit test, but isn't how it works in the real world!
  public ServiceReference[] getRegisteredServices() 
  {
    ServiceReference[] result = null;
    try { 
      result = bc.getServiceReferences((String) null, null);
    } catch (InvalidSyntaxException isx) { 
      // no-op: Swallow exception
    }
    return result;
  }
}
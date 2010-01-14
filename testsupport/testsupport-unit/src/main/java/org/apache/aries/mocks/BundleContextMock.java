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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import junit.framework.AssertionFailedError;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.apache.aries.unittest.mocks.Skeleton;

/**
 *
 */
/**
 * This class is a partial implementation of BundleContext. Its main function
 * is to provide a service registry implementation
 */
public class BundleContextMock
{
  /** The service registry */
  private static Map<String, List<ServiceData>> registry = new HashMap<String, List<ServiceData>>();
  /** A list of bundles installed into the runtime */
  private static List<Bundle> bundles = new ArrayList<Bundle>();
  /** A list of service listeners */
  private static List<ServiceListener> listeners = new ArrayList<ServiceListener>();
  /** The next service id to be assigned */
  private static long nextId = 0;

  private static class MockServiceFactory implements ServiceFactory
  {
    private final Object service;
    
    public MockServiceFactory(Object obj)
    {
      service = obj;
    }
    
    public Object getService(Bundle arg0, ServiceRegistration arg1)
    {
      return service;
    }

    public void ungetService(Bundle arg0, ServiceRegistration arg1, Object arg2)
    {
    }
  }
  
  private static class FilteredServiceListener implements ServiceListener
  {
    private Filter filter;
    private final ServiceListener listener;
    
    public FilteredServiceListener(String f, ServiceListener l)
    {
      listener = l;
      
      if (f != null) {
        try {
          filter = FrameworkUtil.createFilter(f);
        } catch (InvalidSyntaxException e) {
          AssertionFailedError err = new AssertionFailedError("The filter " + f + " is invalid");
          err.initCause(e);
          
          throw err;
        }
      }
    }

    public void serviceChanged(ServiceEvent arg0)
    {
      if (matches(arg0)) listener.serviceChanged(arg0);
    }

    private boolean matches(ServiceEvent arg0)
    {
      if (filter == null) return true;
      
      ServiceReference ref = arg0.getServiceReference();
      
      if (Skeleton.isSkeleton(ref)) {
        Object template = Skeleton.getSkeleton(ref).getTemplateObject();
        
        if (template instanceof ServiceData) {
          return filter.match(((ServiceData)template).getProperties());
        }
      }
      
      return filter.match(ref);
    }
    
    @Override
    public boolean equals(Object obj)
    {
      if (obj == null) return false;
      else if (obj instanceof FilteredServiceListener) {
        return listener.equals(((FilteredServiceListener)obj).listener);
      }
      
      return false;
    }
    
    @Override
    public int hashCode()
    {
      return listener.hashCode();
    }
  }
  
  /**
   * This class represents the information registered about a service. It also
   * implements part of the ServiceRegistration and ServiceReference interfaces.
   */
  private class ServiceData implements Comparable<ServiceReference>
  {
    /** The service that was registered */
    private ServiceFactory serviceImpl;
    /** the service properties */
    @SuppressWarnings("unused")
    private final Hashtable<String, Object> serviceProps = new Hashtable<String, Object>();
    /** The interfaces the service publishes with */
    private String[] interfaceNames;

    /**
     * This method unregisters the service from the registry.
     */
    public void unregister()
    {
      for (String interfaceName : interfaceNames) {
        List<ServiceData> list = registry.get(interfaceName);
        if (list != null) {
          list.remove(this);
          if (list.isEmpty()) {
            registry.remove(interfaceName);
          }
        }
      }
      notifyAllListeners(ServiceEvent.UNREGISTERING);
    }

    /**
     * This method is used to register the service data in the registry
     */
    public void register()
    {
      for (String interfaceName : interfaceNames) {
        List<ServiceData> list = registry.get(interfaceName);
        if (list == null) {
          list = new ArrayList<ServiceData>();
          registry.put(interfaceName, list);
        }
        list.add(this);
      }
      notifyAllListeners(ServiceEvent.REGISTERED);
    }
    
    private void notifyAllListeners(int eventType) {
      List<ServiceListener> copy = new ArrayList<ServiceListener>(listeners.size());
      copy.addAll(listeners);
      for(ServiceListener listener : copy) {
        listener.serviceChanged(new ServiceEvent(eventType, Skeleton.newMock(this, ServiceReference.class)));
      }
    }
    
    /**
     * Change the service properties
     * @param newProps
     */
    public void setProperties(Dictionary<String,Object> newProps)
    {
      // make sure we don't overwrite framework properties
      newProps.put(Constants.OBJECTCLASS, serviceProps.get(Constants.OBJECTCLASS));
      newProps.put(Constants.SERVICE_ID, serviceProps.get(Constants.SERVICE_ID));

      Enumeration<String> keys = newProps.keys();
      
      serviceProps.clear();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        serviceProps.put(key, newProps.get(key));
      }
      
      notifyAllListeners(ServiceEvent.MODIFIED);
    }
    
    /**
     * This implements the isAssignableTo method from ServiceReference.
     * 
     * @param b
     * @param className
     * @return true if the referenced service can be assigned to the requested
     *              class name.
     */
    public boolean isAssignableTo(Bundle b, String className)
    {
      boolean result = false;
      
      for (String iName : interfaceNames)
      {
        result = iName.equals(className);
        
        if (result) break;
      }
      
      return result;
    }
    
    /**
     * Returns the requested service property.
     * @param key the property to return.
     * @return the property value.
     */
    public Object getProperty(String key)
    {
      return serviceProps.get(key);
    }
    
    @Override
    public boolean equals(Object o) {
      if(o == null) return false;
      
      if(o == this) return true;
      
      if (o instanceof ServiceData) {
        ServiceData other = (ServiceData) o;
        return serviceImpl == other.serviceImpl;
      }
      
      return false;
    }
    
    @Override
    public int hashCode()
    {
      return serviceImpl.hashCode();
    }
    
    /**
     * @return the keys of all the service properties.
     */
    public String[] getPropertyKeys()
    {
      Enumeration<String> e = serviceProps.keys();
      
      String[] toReturn = new String[serviceProps.size()];
      
      for(int i = 0 ; i < serviceProps.size(); i++)
        toReturn[i] = e.nextElement();
      
      return toReturn;
    }
    
    /**
     * @return the bundle this service reference was registered against.
     */
    public Bundle getBundle()
    {
      return bundle;
    }
    
    /**
     * @return a service reference for this service registration.
     */
    public ServiceReference getReference()
    {
      return Skeleton.newMock(this, ServiceReference.class);
    }
    
    public Hashtable<String, Object> getProperties()
    {
      return new Hashtable<String, Object>(serviceProps);
    }

    /**
     * Implement the standard behaviour of the registry
     */
    public int compareTo(ServiceReference o) {
      Integer rank = (Integer) serviceProps.get(Constants.SERVICE_RANKING);
      if(rank == null)
        rank = 0;
      
      Integer otherRank = (Integer) o.getProperty(Constants.SERVICE_RANKING);
      if(otherRank == null)
        otherRank = 0;
      //Higher rank = higher order
      int result = rank.compareTo(otherRank);
      
      if(result == 0) {
        Long id = (Long) serviceProps.get(Constants.SERVICE_ID);
        Long otherId = (Long) o.getProperty(Constants.SERVICE_ID);
        //higher id = lower order
        return otherId.compareTo(id);
      }
      return result;
    }
  }

  /** The bundle associated with this bundle context */
  private Bundle bundle;

  /**
   * Default constructor, widely used in the tests.
   */
  public BundleContextMock()
  {
    
  }
  
  /**
   * Constructor used by BundleMock, it ensures the bundle and its context are wired together correctly.
   * 
   * TODO We have to many Bundle mocks objects for a single OSGi bundle, we need to update this.
   * 
   * @param b
   */
  public BundleContextMock(Bundle b)
  {
    bundle = b;
  }
  
  /**
   * This checks that we have at least one service with this interface name.
   * 
   * @param interfaceName the name of the interface.
   */
  public static void assertServiceExists(String interfaceName)
  {
    assertTrue("No service registered with interface " + interfaceName + ". Services found: " + registry.keySet(), registry.containsKey(interfaceName));
  }
  
  /**
   * This checks that we have at no services with this interface name.
   * 
   * @param interfaceName the name of the interface.
   */
  public static void assertNoServiceExists(String interfaceName)
  {
    assertFalse("Services registered with interface " + interfaceName + ". Services found: " + registry.keySet(), registry.containsKey(interfaceName));
  }
  
  /**
   * This implements the registerService method from BundleContext.
   * 
   * @param interFace
   * @param service
   * @param properties
   * @return the ServiceRegistration object for this service.
   */
  public ServiceRegistration registerService(String interFace, final Object service, Dictionary<String, Object> properties)
  {
    // validate that the service implements interFace
    try {
      Class<?> clazz = Class.forName(interFace, false, service.getClass().getClassLoader());
      
      if (!!!clazz.isInstance(service) && !!!(service instanceof ServiceFactory)) {
        throw new AssertionFailedError("The service " + service + " does not implement " + interFace);
      }
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    ServiceFactory factory = new MockServiceFactory(service);
    return registerService(new String[] {interFace}, factory, properties);
  }
  
  /**
   * This implements the registerService method from BundleContext.
   * 
   * @param interfaces
   * @param service
   * @param properties
   * @return the ServiceRegistration object for this service.
   */
  public ServiceRegistration registerService(String[] interfaces, Object service, Dictionary<String, Object> properties)
  {
    if (properties == null) properties = new Hashtable<String, Object>();
    
    ServiceData data = new ServiceData();
    // cast the service to a service factory because in our framework we only ever register
    // a service factory. If we every put a non-service factory object in that is a failure.
    properties.put(Constants.OBJECTCLASS, interfaces);
    properties.put(Constants.SERVICE_ID, nextId++);
    if (service instanceof ServiceFactory) {
      data.serviceImpl = (ServiceFactory)service;
    } else {
      data.serviceImpl = new MockServiceFactory(service);
    }
    data.interfaceNames = interfaces;
    
    Enumeration<String> keys = properties.keys();
    
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      data.serviceProps.put(key, properties.get(key));
    }
    
    data.register();
    
    return Skeleton.newMock(data, ServiceRegistration.class);
  }

  /**
   * This helper method is used to get the service from the registry with the
   * given interface name.
   * 
   * <p>This should really return multiple services.
   * </p>
   * 
   * @param interfaceName the interface name.
   * @param bundle        the bundle name.
   * @return the registered service.
   */
  public static Object getService(String interfaceName, Bundle bundle)
  {
    List<ServiceData> datum = registry.get(interfaceName);
    
    if (datum == null) return null;
    else if (datum.isEmpty()) return null;
    // this is safe for now, but may not be when we do other scoped components.
    else {
      ServiceRegistration reg = Skeleton.newMock(ServiceRegistration.class);
      return datum.iterator().next().serviceImpl.getService(bundle, reg);
    }
  }
  
  /**
   * A mock implementation of the getServiceReferences method. It does not currently
   * process the filter, this is probably a bit hard, so we might cheat when we do.
   * 
   * <p>Note this does not check that the service classes are visible to the
   *   caller as OSGi does. It is equivalent to getAllServiceReferences.
   * </p>
   * 
   * @param className the name of the class the lookup is for.
   * @param filter
   * @return an array of matching service references.
   * @throws InvalidSyntaxException
   */
  public ServiceReference[] getServiceReferences(String className, String filter) throws InvalidSyntaxException
  {
    List<ServiceData> data = new ArrayList<ServiceData>();
    
    if (className != null) {
      List<ServiceData> tmpData = registry.get(className);
      if (tmpData != null) data.addAll(tmpData);
    } else {
      data = new ArrayList<ServiceData>();
      for (List<ServiceData> value : registry.values())
      data.addAll(value);
    }
    
    ServiceReference[] refs;

    if (data == null) {
      refs = null;
    } else {
      
      if (filter != null) {
        Filter f = FrameworkUtil.createFilter(filter);
        
        Iterator<ServiceData> it = data.iterator();
        
        while (it.hasNext()) {
          ServiceData sd = it.next();
          
          if (!!!f.match(sd.getProperties())) it.remove();
        }
      }
      
      if (data.isEmpty()) return null;
      
      refs = new ServiceReference[data.size()];
      for (int i = 0; i < refs.length; i++) {
        refs[i] = Skeleton.newMock(data.get(i), ServiceReference.class);
      }
    }
    
    return refs;
  }
  
  /**
   * Gets the first matching service reference.
   * 
   * @param className the class name wanted.
   * @return the matchine service, or null if one cannot be found.
   */
  public ServiceReference getServiceReference(String className)
  {
    ServiceReference[] refs;
    try {
      refs = getServiceReferences(className, null);
      if (refs != null) return refs[0];
      
      return null;
    } catch (InvalidSyntaxException e) {
      // should never happen.
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * This method finds all the service references in the registry with the
   * matching class name and filter.
   * 
   * @param className
   * @param filter
   * @return the matching service references.
   * @throws InvalidSyntaxException
   */
  public ServiceReference[] getAllServiceReferences(String className, String filter) throws InvalidSyntaxException
  {
    return getServiceReferences(className, filter);
  }
  
  /**
   * Retrieve a service from the registry.
   * @param ref the service reference.
   * @return    the returned service.
   */
  public Object getService(ServiceReference ref)
  {
    ServiceData data = (ServiceData)Skeleton.getSkeleton(ref).getTemplateObject();
    
    return data.serviceImpl.getService(getBundle(), Skeleton.newMock(data, ServiceRegistration.class));
  }
  
  /**
   * This method implements the installBundle method from BundleContext. It
   * makes use of the java.util.jar package to parse the manifest from the input
   * stream.
   * 
   * @param location the location of the bundle.
   * @param is       the input stream to read from.
   * @return         the created bundle.
   * @throws BundleException
   */
  public Bundle installBundle(String location, InputStream is) throws BundleException
  {
    Bundle b;
    JarInputStream jis;
    try {
      jis = new JarInputStream(is);

      Manifest man = jis.getManifest();
      
      b = createBundle(man, null);
      
    } catch (IOException e) {
      throw new BundleException(e.getMessage(), e);
    }
    
    return b;
  }

  /**
   * Create a mock bundle correctly configured using the supplied manifest and
   * location.
   * 
   * @param man      the manifest to load.
   * @param location the location on disk.
   * @return the created bundle
   * @throws MalformedURLException
   */
  private Bundle createBundle(Manifest man, String location) throws MalformedURLException
  {
    Attributes attribs = man.getMainAttributes();
    String symbolicName = attribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
    
    Hashtable<Object, Object> attribMap = new Hashtable<Object, Object>();
    
    for (Map.Entry<Object, Object> entry : attribs.entrySet()) {
      Attributes.Name name = (Attributes.Name)entry.getKey();
      attribMap.put(name.toString(), entry.getValue());
    }
    
    BundleMock mock = new BundleMock(symbolicName, attribMap, location);

    mock.addToClassPath(new File("build/unittest/classes").toURL());

    Bundle b = Skeleton.newMock(mock, Bundle.class);
    
    bundles.add(b);

    return b;
  }
  
  /**
   * Asks to install an OSGi bundle from the given location.
   * 
   * @param location the location of the bundle on the file system.
   * @return the installed bundle.
   * @throws BundleException
   */
  public Bundle installBundle(String location) throws BundleException
  {
    try {
      URI uri = new URI(location.replaceAll(" ", "%20"));

      File baseDir = new File(uri);
      Manifest man = null;
      //check if it is a directory
      if (baseDir.isDirectory()){
      man = new Manifest(new FileInputStream(new File(baseDir, "META-INF/MANIFEST.MF")));
      }
      //if it isn't assume it is a jar file
      else{
        InputStream is = new FileInputStream(baseDir);
        JarInputStream jis = new JarInputStream(is);
        man = jis.getManifest();
        jis.close();
        if (man == null){
          throw new BundleException("Null manifest");
        }
      }
      
      return createBundle(man, location);
    } catch (IOException e) {
      throw new BundleException(e.getMessage(), e);
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      throw new BundleException(e.getMessage(), e);
    }
  }
  
  /**
   * @return all the bundles in the system
   */
    public Bundle[] getBundles()
  {
    return bundles.toArray(new Bundle[bundles.size()]);
  }
  
  /**
   * Add a service listener.
   * 
   * @param listener
   * @param filter
   */
  public void addServiceListener(ServiceListener listener, String filter)
  {
    listeners.add(new FilteredServiceListener(filter, listener));
  }

  /**
   * Add a service listener.
   * 
   * @param listener
   */
  public void addServiceListener(ServiceListener listener) 
  {
    listeners.add(listener);
  }
  
  /**
   * Remove a service listener
   * @param listener
   */
  public void removeServiceListener(ServiceListener listener)
  {
    listeners.remove(new FilteredServiceListener(null, listener));
  }
  
  public String getProperty(String name)
  {
    if (Constants.FRAMEWORK_VERSION.equals(name)) {
      return "4.1";
    }
    /*added System.getProperty so that tests can set a system property
     * but it is retrieved via the BundleContext.
     * This allows tests to emulate different properties being set on the
     * context, helpful for the feature pack launcher/kernel relationship
     */
    else if (System.getProperty(name) != null){
      return System.getProperty(name);
    }
    
    return "";
  }
  
  /**
   * @return the bundle associated with this bundle context (if we created one).
   */
  public Bundle getBundle()
  {
    return bundle;
  }
  
  /**
   * This method clears the service registry.
   */
  public static void clear()
  {
    registry.clear();
    bundles.clear();
    listeners.clear();
    nextId = 0;
  }
  
  public static List<ServiceListener> getServiceListeners()
  {
    return listeners;
  }

  public void addBundle(Bundle b)
  {
    bundles.add(b);
  }
}
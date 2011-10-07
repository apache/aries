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
package org.apache.aries.ejb.openejb.extender;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.NamingException;

import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.aries.util.tracker.RecursiveBundleTracker;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.EjbJarInfo;
import org.apache.openejb.assembler.classic.ProxyFactoryInfo;
import org.apache.openejb.assembler.classic.SecurityServiceInfo;
import org.apache.openejb.assembler.classic.TransactionServiceInfo;
import org.apache.openejb.assembler.dynamic.PassthroughFactory;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.config.EjbModule;
import org.apache.openejb.config.ValidationContext;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;
import org.apache.openejb.ri.sp.PseudoSecurityService;
import org.apache.openejb.util.OpenEjbVersion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class EJBExtender implements BundleActivator, BundleTrackerCustomizer {

  private static final int STARTABLE = Bundle.STARTING | Bundle.ACTIVE;
  
  private static final Object PROCESSING_OBJECT = new Object();
  private static final Object REMOVING_OBJECT = new Object();
  
  private RecursiveBundleTracker tracker;
  
  private final ConcurrentMap<Bundle, RunningApplication> runningApps = 
       new ConcurrentHashMap<Bundle, RunningApplication>();
  
  private final ConcurrentMap<Bundle, Object> processingMap = 
       new ConcurrentHashMap<Bundle, Object>();
  
  public void start(BundleContext context) throws Exception {

    //Internal setup
    OSGiTransactionManager.init(context);
    AriesProxyService.init(context);
    try {
      AriesPersistenceContextIntegration.init(context);
    } catch (NoClassDefFoundError ncdfe) {
      //TODO log that no JPA Context integration is available
    }
    
    //Setup OpenEJB with our own extensions
    setupOpenEJB();
    
    tracker = new RecursiveBundleTracker(context, Bundle.INSTALLED | Bundle.RESOLVED | 
        Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING, this);

    tracker.open();
  }

  private void setupOpenEJB() throws OpenEJBException {
    //Avoid a ClassLoader problem 
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(OpenEjbVersion.class.getClassLoader());
      OpenEjbVersion.get();
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
    
    Assembler a = new Assembler();
    TransactionServiceInfo tsi = new TransactionServiceInfo();
    tsi.service = "TransactionManager";
    tsi.id = "OSGi Transaction Manager";
    PassthroughFactory.add(tsi, OSGiTransactionManager.get());
    //Avoid another ClassLoader problem
    try {
      Thread.currentThread().setContextClassLoader(PassthroughFactory.class.getClassLoader());
      a.createTransactionManager(tsi);
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
    
    try {
      //Overwrite existing, default JPA integration with an Aries JPA integrated one
      Assembler.getContext().put(JtaEntityManagerRegistry.class.getName(), 
          AriesPersistenceContextIntegration.get());
      SystemInstance.get().setComponent(JtaEntityManagerRegistry.class, 
          AriesPersistenceContextIntegration.get());
    } catch (NoClassDefFoundError ncdfe) {
      //TODO log that no JPA Context integration is available
    }
    
    SecurityServiceInfo ssi = new SecurityServiceInfo();
    ssi.service = "SecurityService";
    ssi.id = "Pseudo Security Service";
    PassthroughFactory.add(ssi, new PseudoSecurityService());
    //Avoid another ClassLoader problem
    try {
      Thread.currentThread().setContextClassLoader(PassthroughFactory.class.getClassLoader());
      a.createSecurityService(ssi);
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
    
    
    ProxyFactoryInfo proxyFactoryInfo = new ProxyFactoryInfo();
    proxyFactoryInfo.id = "Aries ProxyFactory";
    proxyFactoryInfo.service = "ProxyFactory";
    proxyFactoryInfo.properties = new Properties();
    PassthroughFactory.add(proxyFactoryInfo, AriesProxyService.get());
    try {
      Thread.currentThread().setContextClassLoader(PassthroughFactory.class.getClassLoader());
      a.createProxyFactory(proxyFactoryInfo);
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  public void stop(BundleContext context) throws Exception {
    tracker.close();
    AriesProxyService.get().destroy();
    OSGiTransactionManager.get().destroy();
    try {
      AriesPersistenceContextIntegration.get().destroy();
    } catch (NoClassDefFoundError ncdfe) {
      //TODO log that no JPA Context integration is available
    }
  }

  public Object addingBundle(Bundle bundle, BundleEvent event) {
    
    if(mightContainEJBs(bundle)) {
      if((bundle.getState() & STARTABLE) != 0) {
        startEJBs(bundle);
      }
      return bundle;
    }
    return null;
  }


  private boolean mightContainEJBs(Bundle bundle) {
    Dictionary<String, String> headers = bundle.getHeaders();
    return (headers.get("Export-EJB") != null) || (headers.get("Web-ContextPath") != null);
  }

  public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
    if((bundle.getState() & STARTABLE) != 0) {
      startEJBs(bundle);
    } else if (bundle.getState() == Bundle.STOPPING) {
      stopEJBs(bundle);
    }
  }

  private void startEJBs(final Bundle bundle) {

    try {
      //If there is another thread adding or removing then stop here
      Object o = processingMap.put(bundle, PROCESSING_OBJECT);
      if(o == REMOVING_OBJECT || o == PROCESSING_OBJECT) {
        return;
      }
      //If already running then avoid
      if(runningApps.get(bundle) != null)
        return;
      
      //Broken validation for persistence :(
      EjbModule ejbModule = new EjbModule(AriesFrameworkUtil.getClassLoaderForced(bundle), null, null, null);
      try {
        Field f = EjbModule.class.getDeclaredField("validation");
        f.setAccessible(true);
        f.set(ejbModule, new ValidationProofValidationContext(ejbModule));
      } catch (Exception e) {
        // Hmmm
      }
      addAltDDs(ejbModule, bundle);
      //We build our own because we can't trust anyone to get the classpath right otherwise!
      ejbModule.setFinder(new OSGiFinder(bundle));
      
      ConfigurationFactory configurationFactory = new ConfigurationFactory();
      
      EjbJarInfo ejbInfo = null;
      //Avoid yet another ClassLoading problem
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader(new ClassLoader(OpenEjbVersion.class.getClassLoader()) {
          protected Class<?> findClass(String name) throws ClassNotFoundException {
            for(Bundle b : bundle.getBundleContext().getBundles()) {
              if(b.getSymbolicName().contains("jaxb-impl"))
                return b.loadClass(name);
            
            }
            throw new ClassNotFoundException(name);
          }
        });
        
        ejbInfo = configurationFactory.configureApplication(ejbModule);
        //Another oddity here
        ejbInfo.validationInfo = null;
      } finally {
        Thread.currentThread().setContextClassLoader(cl);
      }
      
      
      Assembler assembler = (Assembler) SystemInstance.get().getComponent(Assembler.class);
      RunningApplication app = null;
      try {
        SystemInstance.get().setProperty("openejb.geronimo", "true");
        cl = Thread.currentThread().getContextClassLoader();
        try {
          Thread.currentThread().setContextClassLoader(OpenEjbVersion.class.getClassLoader());
          app = new RunningApplication(assembler.createApplication(ejbInfo, 
              new AppClassLoader(ejbModule.getClassLoader())), bundle, ejbInfo.enterpriseBeans);
        } finally {
          Thread.currentThread().setContextClassLoader(cl);
        }
      } finally {
        SystemInstance.get().getProperties().remove("openejb.geronimo");
      }
      runningApps.put(bundle, app);
      
      app.init();
      
      
    } catch (OpenEJBException oee) {
      // TODO Auto-generated catch block
      oee.printStackTrace();
    } catch (NamingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if(processingMap.remove(bundle) == REMOVING_OBJECT) {
        stopEJBs(bundle);
      }
    }
  }

  private void addAltDDs(EjbModule ejbModule, Bundle bundle) {
    
    Map<String, Object> altDDs = ejbModule.getAltDDs(); 
    
    String folder = (bundle.getHeaders().get("Web-ContextPath") == null) ? 
        "META-INF" : "WEB-INF";
    
    Enumeration<URL> e = bundle.findEntries(folder, "*.xml", false);
    if(e == null)
      return;
    
    for(URL u : Collections.list(e)) {
      
      String urlString = u.toExternalForm();
      urlString = urlString.substring(urlString.lastIndexOf('/') + 1);
        
      altDDs.put(urlString, u);
    }
    //Persistence descriptors are handled by Aries JPA, but OpenEJB fails validation
    //if we hide them. As a result we switch it off.
    //altDDs.remove("persistence.xml");
  }

  private void stopEJBs(Bundle bundle) {
    if(processingMap.put(bundle, REMOVING_OBJECT) == PROCESSING_OBJECT)
      return;
    else {
      try {
        RunningApplication app = runningApps.remove(bundle);
        if(app != null) {
          app.destroy();
          Assembler assembler = (Assembler) SystemInstance.get().getComponent(Assembler.class);
          assembler.destroyApplication(app.getCtx());
        }
      } catch (OpenEJBException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        if(processingMap.remove(bundle) == PROCESSING_OBJECT)
          startEJBs(bundle);
      }
    }
  }
  
  public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
    if (bundle.getState() == Bundle.STOPPING) {
      stopEJBs(bundle);
    }
  }
  
  private static final class ValidationProofValidationContext extends ValidationContext {
    private ValidationProofValidationContext(EjbModule mod) {
      super(mod);
    }

    @Override
    public boolean hasErrors() {
      return false;
    }

    @Override
    public boolean hasFailures() {
      return false;
    }

    @Override
    public boolean hasWarnings() {
      return false;
    }

    
  }

  private static final class AppClassLoader extends ClassLoader {
    private AppClassLoader(ClassLoader parentLoader) {
      super(parentLoader);
    }

    @Override
    protected Class<?> findClass(String className)
        throws ClassNotFoundException {
      return Class.forName(className, false, OpenEjbVersion.class.getClassLoader());
    }
  }
}

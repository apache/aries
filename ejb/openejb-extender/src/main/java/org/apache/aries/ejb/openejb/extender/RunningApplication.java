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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.util.AriesFrameworkUtil;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.openejb.AppContext;
import org.apache.openejb.BeanContext;
import org.apache.openejb.ContainerType;
import org.apache.openejb.assembler.classic.EnterpriseBeanInfo;
import org.apache.openejb.assembler.classic.PersistenceContextReferenceInfo;
import org.apache.openejb.assembler.classic.PersistenceUnitReferenceInfo;
import org.apache.openejb.assembler.classic.ProxyInterfaceResolver;
import org.apache.openejb.assembler.classic.ReferenceLocationInfo;
import org.apache.openejb.jee.EnterpriseBean;
import org.apache.openejb.persistence.JtaEntityManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class RunningApplication implements ServiceTrackerCustomizer {

  private static final String NONE = "NONE";
  private static final String ALL = "ALL";
  
  private final AppContext ctx;
  private final Bundle bundle;
  private final Collection<ServiceRegistration<?>> regs = 
    new ArrayList<ServiceRegistration<?>>();
  
  private ServiceTracker tracker;
  
  private final ConcurrentMap<String, ConcurrentMap<Context, PersistenceUnitReferenceInfo>> 
    unitRegistrations = new ConcurrentHashMap<String, ConcurrentMap<Context, PersistenceUnitReferenceInfo>>();
  
  private final ConcurrentMap<String, ConcurrentMap<Context, PersistenceContextReferenceInfo>> 
    contextRegistrations = new ConcurrentHashMap<String, ConcurrentMap<Context, PersistenceContextReferenceInfo>>();
  
  public RunningApplication(AppContext context, Bundle bundle, List<EnterpriseBeanInfo> enterpriseBeans) {
    this.ctx = context;
    this.bundle = bundle;
    
    for(EnterpriseBeanInfo bean : enterpriseBeans) {
      for(PersistenceUnitReferenceInfo pui : bean.jndiEnc.persistenceUnitRefs) {
        ConcurrentMap<Context, PersistenceUnitReferenceInfo> map = unitRegistrations.
           get(pui.persistenceUnitName);
        
        if(map == null) {
          map = new ConcurrentHashMap<Context, PersistenceUnitReferenceInfo>();
          unitRegistrations.put(pui.persistenceUnitName, map);
        }
        
        for(BeanContext eb : ctx.getBeanContexts()) {
          if(eb.getEjbName().equals(bean.ejbName)){
            map.put(eb.getJndiContext(), pui);
            continue;
          }
        }
      }
      
      for(PersistenceContextReferenceInfo pci : bean.jndiEnc.persistenceContextRefs) {
        ConcurrentMap<Context, PersistenceContextReferenceInfo> map = contextRegistrations.
            get(pci.persistenceUnitName);
        
        if(map == null) {
          map = new ConcurrentHashMap<Context, PersistenceContextReferenceInfo>();
          contextRegistrations.put(pci.persistenceUnitName, map);
        }
        
        for(BeanContext eb : ctx.getBeanContexts()) {
          if(eb.getEjbName().equals(bean.ejbName)){
            map.put(eb.getJndiContext(), pci);
            continue;
          }
        }
      }
    }
  }

  public AppContext getCtx() {
    return ctx;
  }

  public void init() {
    
    tracker = new ServiceTracker(bundle.getBundleContext(), 
        EntityManagerFactory.class.getName(), this);
    tracker.open();
    
    registerEJBs();
  }
  
  public void destroy() {
    tracker.close();
    for(ServiceRegistration<?> reg : regs) {
      AriesFrameworkUtil.safeUnregisterService(reg);
    }
  }
  

  
  private void registerEJBs() {
    
    Collection<String> names = new HashSet<String>();
    
    Dictionary<String, String> d = bundle.getHeaders();
    String valueOfExportEJBHeader = d.get("Export-EJB");
    
    if((valueOfExportEJBHeader == null)||(valueOfExportEJBHeader.equals(""))){
      return;
    }
    
    List<NameValuePair> contentsOfExportEJBHeader = ManifestHeaderProcessor.parseExportString(valueOfExportEJBHeader);
    for(NameValuePair nvp:contentsOfExportEJBHeader){
      names.add(nvp.getName());
    }
    
    if(names.contains(NONE)){
      return;
    }
    
    if(names.contains(ALL)){
      names = new AllCollection<String>();
    }
    
    //Register our session beans
    for (BeanContext beanContext : ctx.getDeployments()) {
      String ejbName = beanContext.getEjbName();
      //Skip if not a Singleton or stateless bean
      ContainerType type = beanContext.getContainer().getContainerType();
      boolean register = type == ContainerType.SINGLETON || type == ContainerType.STATELESS;
      
      //Skip if not allowed name
      register &= names.contains(ejbName);
      
      if(!register) {
        continue;
      }
      
      if (beanContext.isLocalbean()) {

        BeanContext.BusinessLocalBeanHome home = beanContext.getBusinessLocalBeanHome();
    
        Dictionary<String, Object> props = new Hashtable<String, Object>(); 
        
        props.put("ejb.name", ejbName);
        props.put("ejb.type", getCasedType(type));
        regs.add(bundle.getBundleContext().registerService(beanContext.getBeanClass().getName(), 
            new EJBServiceFactory(home), props));
      }

  
      for (Class<?> interfce : beanContext.getBusinessLocalInterfaces()) {

        BeanContext.BusinessLocalHome home = beanContext.getBusinessLocalHome(interfce);
        
        Dictionary<String, Object> props = new Hashtable<String, Object>(); 
        
        props.put("ejb.name", ejbName);
        props.put("ejb.type", getCasedType(type));
        regs.add(bundle.getBundleContext().registerService(interfce.getName(), 
            new EJBServiceFactory(home), props));
      }
      
      for (Class<?> interfce : beanContext.getBusinessRemoteInterfaces()) {

        List<Class> interfaces = ProxyInterfaceResolver.getInterfaces(beanContext.getBeanClass(), 
            interfce, beanContext.getBusinessRemoteInterfaces());
        BeanContext.BusinessRemoteHome home = beanContext.getBusinessRemoteHome(interfaces, interfce);
        
        Dictionary<String, Object> props = new Hashtable<String, Object>(); 
        
        props.put("sevice.exported.interfaces", interfce.getName());
        props.put("ejb.name", ejbName);
        props.put("ejb.type", getCasedType(type));
        regs.add(bundle.getBundleContext().registerService(interfce.getName(), 
            new EJBServiceFactory(home), props));
      }
    }
  }
  
  private String getCasedType(ContainerType type) {
    String s = type.toString().substring(0,1).toUpperCase();
    s += type.toString().substring(1).toLowerCase();
    return s;
  }

  public Object addingService(ServiceReference reference) {
    
    if(isTrue(reference, PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT) &&
       !!!isTrue(reference, PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE)) {
      
      Map<Context, PersistenceUnitReferenceInfo> pUnitRefs = unitRegistrations.
             get(reference.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME));
      Map<Context, PersistenceContextReferenceInfo> pCtxRefs = contextRegistrations.
             get(reference.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME));
      
      if(pUnitRefs == null) {
        pUnitRefs = new HashMap<Context, PersistenceUnitReferenceInfo>();
      }
      if(pCtxRefs == null) {
        pCtxRefs = new HashMap<Context, PersistenceContextReferenceInfo>();
      }      
      
      if(pUnitRefs.size() > 0 || pCtxRefs.size() > 0) {
      
        EntityManagerFactory emf = (EntityManagerFactory)bundle.getBundleContext().getService(reference);
        
        for(Entry<Context, PersistenceUnitReferenceInfo> e : pUnitRefs.entrySet()) {
          try {
            e.getKey().bind(e.getValue().referenceName, emf);
          } catch (NamingException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
          }
        }
        
        for(Entry<Context, PersistenceContextReferenceInfo> e : pCtxRefs.entrySet()) {
          PersistenceContextReferenceInfo pci = e.getValue();
          try {
            e.getKey().bind(pci.referenceName, new JtaEntityManager((String)reference.getProperty(
                PersistenceUnitConstants.OSGI_UNIT_NAME), AriesPersistenceContextIntegration.get(),
                emf, pci.properties, pci.extended));
          } catch (NamingException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
          }
        }
        return emf;
      }
    }
    return null;
  }

  private boolean isTrue(ServiceReference reference,
      String key) {
    return Boolean.parseBoolean(String.valueOf(reference.getProperty(key)));
  }

  public void modifiedService(ServiceReference reference, Object service) {
    //No op
  }

  public void removedService(ServiceReference reference, Object service) {
    
    Map<Context, PersistenceUnitReferenceInfo> pUnitRefs = unitRegistrations.
        get(reference.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME));
    Map<Context, PersistenceContextReferenceInfo> pCtxRefs = contextRegistrations.
        get(reference.getProperty(PersistenceUnitConstants.OSGI_UNIT_NAME));

    if(pUnitRefs == null) {
      pUnitRefs = new HashMap<Context, PersistenceUnitReferenceInfo>();
    }
    if(pCtxRefs == null) {
      pCtxRefs = new HashMap<Context, PersistenceContextReferenceInfo>();
    }      
    
    if(pUnitRefs.size() > 0 || pCtxRefs.size() > 0) {
    
      for(Entry<Context, PersistenceUnitReferenceInfo> e : pUnitRefs.entrySet()) {
        try {
          e.getKey().unbind(e.getValue().referenceName);
        } catch (NamingException ex) {
          // TODO Auto-generated catch block
          ex.printStackTrace();
        }
      }
    
      for(Entry<Context, PersistenceContextReferenceInfo> e : pCtxRefs.entrySet()) {
        PersistenceContextReferenceInfo pci = e.getValue();
        try {
          e.getKey().unbind(pci.referenceName);
        } catch (NamingException ex) {
          // TODO Auto-generated catch block
          ex.printStackTrace();
        }
      }
    }
  }
}

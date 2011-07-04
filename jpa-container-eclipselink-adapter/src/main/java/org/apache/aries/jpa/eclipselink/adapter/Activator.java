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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.eclipselink.adapter;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.persistence.spi.ProviderUtil;
import javax.sql.DataSource;

import org.apache.aries.jpa.eclipselink.adapter.platform.OSGiTSServer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This exception is thrown if an {@link EntityManagerFactoryManager} has
 * entered an invalid state and needs to be destroyed
 */
public class Activator implements BundleActivator, BundleListener {
    private static final String ECLIPSELINK_JPA_PROVIDER_BUNDLE_SYMBOLIC_NAME = "org.eclipse.persistence.jpa";
    private static final String ECLIPSELINK_JPA_PROVIDER_CLASS_NAME = "org.eclipse.persistence.jpa.PersistenceProvider";
    private final ConcurrentMap<Bundle, ServiceRegistration> registeredProviders = new ConcurrentHashMap<Bundle, ServiceRegistration>();
    
    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    
    private ServiceTracker tracker;
    private BundleContext context;
    
    private static class PersistenceUnitProxyWithTargetServer implements PersistenceUnitInfo {
        private final PersistenceUnitInfo delegate;
        
        public PersistenceUnitProxyWithTargetServer(PersistenceUnitInfo info) {
            delegate = info;
        }

        public void addTransformer(ClassTransformer arg0) {
            delegate.addTransformer(arg0);
        }

        public boolean excludeUnlistedClasses() {
            return delegate.excludeUnlistedClasses();
        }

        public ClassLoader getClassLoader() {
            ClassLoader unionClassLoader = new ClassLoader(delegate.getClassLoader()) {
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    return Activator.class.getClassLoader().loadClass(name);
                }
            };
            
            return unionClassLoader;
        }

        public List<URL> getJarFileUrls() {
            return delegate.getJarFileUrls();
        }

        public DataSource getJtaDataSource() {
            return delegate.getJtaDataSource();
        }

        public List<String> getManagedClassNames() {
            return delegate.getManagedClassNames();
        }

        public List<String> getMappingFileNames() {
            return delegate.getMappingFileNames();
        }

        public ClassLoader getNewTempClassLoader() {
            return delegate.getNewTempClassLoader();
        }

        public DataSource getNonJtaDataSource() {
            return delegate.getNonJtaDataSource();
        }

        public String getPersistenceProviderClassName() {
            return delegate.getPersistenceProviderClassName();
        }

        public String getPersistenceUnitName() {
            return delegate.getPersistenceUnitName();
        }

        public URL getPersistenceUnitRootUrl() {
            return delegate.getPersistenceUnitRootUrl();
        }

        public String getPersistenceXMLSchemaVersion() {
            return delegate.getPersistenceXMLSchemaVersion();
        }

        public Properties getProperties() {
            Properties props = delegate.getProperties();
            if (!!!props.containsKey("eclipselink.target-server")) {
                props.put("eclipselink.target-server", OSGiTSServer.class.getName());
            }
            
            return props;
        }

        public SharedCacheMode getSharedCacheMode() {
            return delegate.getSharedCacheMode();
        }

        public PersistenceUnitTransactionType getTransactionType() {
            return delegate.getTransactionType();
        }

        public ValidationMode getValidationMode() {
            return delegate.getValidationMode();
        }
    }
  
    private static class EclipseLinkProviderService implements ServiceFactory {
        private final Bundle eclipseLinkJpaBundle;
        
        public EclipseLinkProviderService(Bundle b) {
            eclipseLinkJpaBundle = b;
        }
        
        public Object getService(Bundle bundle, ServiceRegistration registration) {
            logger.debug("Requested EclipseLink Provider service");
            
            try {
                Class<? extends PersistenceProvider> providerClass = eclipseLinkJpaBundle.loadClass(ECLIPSELINK_JPA_PROVIDER_CLASS_NAME);
                Constructor<? extends PersistenceProvider> con = providerClass.getConstructor();
                final PersistenceProvider provider = con.newInstance();
                
                return new PersistenceProvider() {
                    public ProviderUtil getProviderUtil() {
                        return provider.getProviderUtil();
                    }
                    
                    public EntityManagerFactory createEntityManagerFactory(String arg0, Map arg1) {
                        return provider.createEntityManagerFactory(arg0, arg1);
                    }
                    
                    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo punit, Map props) {
                        return provider.createContainerEntityManagerFactory(new PersistenceUnitProxyWithTargetServer(punit), props);
                    }
                };
                
            } catch (Exception e) {
                logger.error("Got exception trying to instantiate the EclipseLink provider", e);
                return null;                
            }
        }

        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {}
    }
    
    public void start(BundleContext ctx) {
        logger.debug("Starting EclipseLink adapter");
        
        context = ctx;
        
        tracker = new ServiceTracker(ctx, PackageAdmin.class.getName(), null);
        tracker.open();
        
        ctx.addBundleListener(this);
        
        for (Bundle b : ctx.getBundles()) {
            if ((b.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) 
                handlePotentialEclipseLink(b);
        }
    }
  
    public void stop(BundleContext ctx) {
        logger.debug("Stopping EclipseLink adapter");
        
        tracker.close();
        
        for (ServiceRegistration reg : registeredProviders.values()) {
            reg.unregister();
        }
    }

    public void bundleChanged(BundleEvent event) {
        if ((event.getType() & (BundleEvent.STARTED | BundleEvent.STARTING | BundleEvent.LAZY_ACTIVATION)) != 0) {
            handlePotentialEclipseLink(event.getBundle());
        } else if (event.getType() == BundleEvent.STOPPING) {
            ServiceRegistration reg = registeredProviders.remove(event.getBundle());
            if (reg != null) {
                reg.unregister();
            }
        }
    }
    
    private void handlePotentialEclipseLink(Bundle b) {
        if (b.getSymbolicName().equals(ECLIPSELINK_JPA_PROVIDER_BUNDLE_SYMBOLIC_NAME)) {
            logger.debug("Found EclipseLink bundle {}", b);
            
            try {
                b.loadClass(ECLIPSELINK_JPA_PROVIDER_CLASS_NAME);
            } catch (ClassNotFoundException cnfe) {
                logger.debug("Did not find provider class, exiting");
                // not one we can handle
                return;
            }
            
            if (!!!registeredProviders.containsKey(b)) {
                logger.debug("Adding new EclipseLink provider for bundle {}", b);
                
                ServiceFactory factory = new EclipseLinkProviderService(b);
                
                Hashtable<String, Object> props = new Hashtable<String, Object>();
                props.put("org.apache.aries.jpa.container.weaving.packages", getJPAPackages(b));
                props.put("javax.persistence.provider", ECLIPSELINK_JPA_PROVIDER_CLASS_NAME);
                            
                ServiceRegistration reg = b.getBundleContext().registerService(
                        PersistenceProvider.class.getName(), factory, props);
                
                ServiceRegistration old = registeredProviders.putIfAbsent(b, reg);
                if (old != null) {
                    reg.unregister();
                }
            }
        }
    }
    
    /**
     * Get all the relevant packages that the EclipseLink JPA provider exports or persistence packages it uses itself
     * @param jpaBundle
     * @return
     */
    private String[] getJPAPackages(Bundle jpaBundle) {
        Set<String> result = new HashSet<String>();
        
        PackageAdmin admin = (PackageAdmin) tracker.getService();
        for (Bundle b : context.getBundles()) {
            for (ExportedPackage ep : nullSafe(admin.getExportedPackages(b))) {
                boolean add = true;
                if (b.equals(jpaBundle)) {
                    add = true;
                } else if (ep.getName().startsWith("org.eclipse.persistence")) {
                    inner: for (Bundle b2 : nullSafe(ep.getImportingBundles())) {
                        if (b2.equals(jpaBundle)) {
                            add = true;
                            break inner;
                        }
                    }
                }
                
                if (add) {
                    String suffix = ";" + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=" + b.getSymbolicName() + ";" + Constants.BUNDLE_VERSION_ATTRIBUTE  + "=" + b.getVersion();                    
                    result.add(ep.getName()+suffix);
                }                
            }
        }
        
        result.add("org.apache.aries.jpa.eclipselink.adapter.platform;" + 
                Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=" + context.getBundle().getSymbolicName() + ";" + 
                Constants.BUNDLE_VERSION_ATTRIBUTE  + "=" + context.getBundle().getVersion());        
        
        logger.debug("Found JPA packages {}", result);
        
        return result.toArray(new String[0]);
    }
    
    private<T> List<T> nullSafe(T[] array) {
        if (array == null) return Collections.emptyList();
        else return Arrays.asList(array);
    }
}

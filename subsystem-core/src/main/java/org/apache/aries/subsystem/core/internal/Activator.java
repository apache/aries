/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bundle activator for the this bundle.
 * When the bundle is starting, this activator will create
 * and register the SubsystemAdmin service.
 */
public class Activator implements BundleActivator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);
    
    private static BundleContext context;
    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    private static SubsystemEventDispatcher eventDispatcher;
    private static SubsystemAdminFactory adminFactory;
    
    public void start(BundleContext context) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subsystem activator starting");
        }
        Activator.context = context;
        Activator.eventDispatcher = new SubsystemEventDispatcher(context);
        adminFactory = new SubsystemAdminFactory();
        register(SubsystemAdmin.class, adminFactory, null);
        register(ResourceResolver.class,
                 new NoOpResolver(),
                 DictionaryBuilder.build(Constants.SERVICE_RANKING, Integer.MIN_VALUE));
        register(ResourceResolver.class,
                new ResourceResolverImpl(this.context),
                DictionaryBuilder.build(Constants.SERVICE_RANKING, 0));
        register(ResourceProcessor.class,
                new BundleResourceProcessor(),
                DictionaryBuilder.build(SubsystemConstants.SERVICE_RESOURCE_TYPE, SubsystemConstants.RESOURCE_TYPE_BUNDLE));
        register(ResourceProcessor.class,
                new SubsystemResourceProcessor(),
                DictionaryBuilder.build(SubsystemConstants.SERVICE_RESOURCE_TYPE, SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM));
    }

    protected <T> void register(Class<T> clazz, T service, Dictionary props) {
         registrations.add(context.registerService(clazz.getName(), service, props));
    }

    protected <T> void register(Class<T> clazz, ServiceFactory factory, Dictionary props) {
         registrations.add(context.registerService(clazz.getName(), factory, props));
    }

    public void stop(BundleContext context) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subsystem activator stopping");
        }
        for (ServiceRegistration r : registrations) {
            try {
                r.unregister();
            } catch (Exception e) {
                LOGGER.warn("Subsystem Activator shut down", e);
            }
        }
        eventDispatcher.destroy();
        if (adminFactory!= null) {
            adminFactory.destroy();
        }
        
    }
    
    public static BundleContext getBundleContext() {
        return context;
    }
    
    public static SubsystemEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }


    public static class SubsystemAdminFactory implements ServiceFactory {
        //private final List<ScopeAdmin> scopeAdmins = new ArrayList<ScopeAdmin>();
        private final List<SubsystemAdmin> admins = new ArrayList<SubsystemAdmin>();
        private final Map<SubsystemAdmin, Long> references = new HashMap<SubsystemAdmin, Long>();
        private ScopeAdmin scopeAdmin; // scope admin for the root scope.
        private static ServiceTracker serviceTracker;
        private SubsystemAdmin defaultAdmin;
        private ServiceRegistration rootAdminReg;
        
        public SubsystemAdminFactory() throws InvalidSyntaxException  {
            context = Activator.getBundleContext();
            
            ServiceReference[] reference = Activator.getBundleContext().getServiceReferences(ScopeAdmin.class.getName(), 
            "(&(ScopeName=root))");
            if (reference != null && reference.length == 1) {
                ScopeAdmin scopeAdmin = (ScopeAdmin)Activator.getBundleContext().getService(reference[0]);
                Subsystem subsystem = new SubsystemImpl(scopeAdmin.getScope(), new HashMap<String, String>());
                defaultAdmin = new SubsystemAdminImpl(scopeAdmin, subsystem, null);
                rootAdminReg = context.registerService(SubsystemAdmin.class.getName(), 
                        defaultAdmin, 
                        DictionaryBuilder.build("Subsystem", subsystem.getSubsystemId(), "SubsystemParentId", 0));
                admins.add(defaultAdmin);
            } else {
                throw new RuntimeException("Unable to locate service reference for the root scope admin");
            }
            
            Filter filter = FrameworkUtil.createFilter("(&("
                    + Constants.OBJECTCLASS + "=" + SubsystemAdmin.class.getName() + "))");
            serviceTracker = new ServiceTracker(context, filter,
                    new ServiceTrackerCustomizer() {

                        public Object addingService(ServiceReference reference) {
                            // adding new service, update admins map
                            SubsystemAdmin sa = (SubsystemAdmin) context
                                    .getService(reference);
                            admins.add(sa);

                            return sa;
                        }

                        public void modifiedService(ServiceReference reference,
                                Object service) {
                            // TODO Auto-generated method stub

                        }

                        public void removedService(ServiceReference reference,
                                Object service) {
                            SubsystemAdmin sa = (SubsystemAdmin) service;
                            admins.remove(sa);
                        }

                    });
        }
        
        public void destroy() {
            serviceTracker.close();
        }
        
        private SubsystemAdmin getSubsystemAdmin(Bundle bundle) {
            // first check if it is in root framework
            Bundle[] bundles = Activator.getBundleContext().getBundles();
            for (Bundle b : bundles) {
                if (b == bundle) {
                    return defaultAdmin;
                }
            }
            // check if they are bundles in the 
            for (SubsystemAdmin admin : admins) {
                Collection<Subsystem> subsystems = admin.getSubsystems();
                for (Subsystem subsystem : subsystems) {
                    Collection<Bundle> subsystemBundles = subsystem.getBundles();
                    for (Bundle b : subsystemBundles) {
                        if (b == bundle) {
                            return admin;
                        }
                    }
                }
            }
            
            return null;
        }
        public synchronized Object getService(Bundle bundle, ServiceRegistration registration) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Get SubsystemAdmin service from bundle symbolic name {} version {}", bundle.getSymbolicName(), bundle.getVersion());
            }
            
            long ref = 0;
            
            // figure out the subsystemAdmin for the bundle           
            SubsystemAdmin admin = getSubsystemAdmin(bundle);
            
            if (admin == null) {
                throw new SubsystemException("Unable to locate the Subsystem admin for the bundle " + bundle.toString());
            }

            if (references.get(admin) == null) {
                ref = 0;
            }
            
            references.put(admin, ref + 1);
            return admin;
        }

        public synchronized void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unget SubsystemAdmin service {} from bundle symbolic name {} version {}", new Object[] {service, bundle.getSymbolicName(), bundle.getVersion()});
            }
            SubsystemAdminImpl admin = (SubsystemAdminImpl) service;
            long ref = references.get(admin) - 1;
            if (ref == 0) {
                admin.dispose();
                admins.remove(admin.context);
                references.remove(admin);
            } else {
                references.put(admin, ref);
            }
        }

    }

}

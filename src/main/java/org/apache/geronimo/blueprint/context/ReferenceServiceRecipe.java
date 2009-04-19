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
package org.apache.geronimo.blueprint.context;

import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.geronimo.blueprint.LifeCycle;
import org.apache.geronimo.blueprint.BlueprintConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.UnaryServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.context.ServiceUnavailableException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import net.sf.cglib.proxy.ProxyRefDispatcher;
import net.sf.cglib.proxy.Enhancer;

/**
 * A recipe to create an OSGi service reference
 */
public class ReferenceServiceRecipe extends AbstractRecipe implements ProxyRefDispatcher, LifeCycle, ServiceTrackerCustomizer {

    private final BundleContext bundleContext;
    private final UnaryServiceReferenceComponentMetadata metadata;
    private ServiceTracker tracker;
    private Class type;
    private String filter;

    public ReferenceServiceRecipe(BundleContext bundleContext, UnaryServiceReferenceComponentMetadata metadata) {
        this.bundleContext = bundleContext;
        this.metadata = metadata;
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        // TODO: bindingListeners
        // TODO: serviceAvailabilitySpecification
        try {
            ClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle());
            Enhancer e = new Enhancer();
            e.setClassLoader(classLoader);
            type = getTargetClass(classLoader);
            e.setSuperclass(type);
            e.setInterfaces(getInterfaces(classLoader));
            e.setInterceptDuringConstruction(false);
            e.setCallback(this);
            e.setUseFactory(false);
            Object obj = e.create();

            filter = getOsgiFilter();
            tracker = new ServiceTracker(bundleContext, FrameworkUtil.createFilter(filter), this);
            tracker.open();

            return obj;
        } catch (Exception e) {
            throw new ConstructionException(e);
        }
    }

    private String getOsgiFilter() {
        List<String> members = new ArrayList<String>();
        // Handle filter
        String filter = metadata.getFilter();
        if (filter != null && filter.length() > 0) {
            if (!filter.startsWith("(")) {
                filter = "(" + filter + ")";
            }
            members.add(filter);
        }
        // Handle interfaces
        Set<String> interfaces = (Set<String>) metadata.getInterfaceNames();
        if (interfaces != null && !interfaces.isEmpty()) {
            for (String itf : interfaces) {
                members.add("(" + Constants.OBJECTCLASS + "=" + itf + ")");
            }
        }
        // Handle component name
        String componentName = metadata.getComponentName();
        if (componentName != null && componentName.length() > 0) {
            members.add("(" + BlueprintConstants.COMPONENT_NAME_PROPERTY + "=" + componentName + ")");
        }
        // Create filter
        if (members.isEmpty()) {
            throw new IllegalStateException("No constraints were specified on the service reference");
        }
        StringBuilder sb = new StringBuilder("(&");
        for (String member : members) {
            sb.append(member);
        }
        sb.append(")");
        return sb.toString();
    }

    private Class[] getInterfaces(ClassLoader classLoader) throws ClassNotFoundException {
        List<Class> interfaces = new ArrayList<Class>();
        for (String name : (Set<String>) metadata.getInterfaceNames()) {
            Class clazz = classLoader.loadClass(name);
            if (clazz.isInterface()) {
                interfaces.add(clazz);
            }
        }
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    private Class getTargetClass(ClassLoader classLoader) throws ClassNotFoundException {
        Class root = Object.class;
        for (String name : (Set<String>) metadata.getInterfaceNames()) {
            Class clazz = classLoader.loadClass(name);
            if (!clazz.isInterface()) {
                // Check that all classes are in the same hierarchy
                for (Class p = clazz; p != Object.class; p = p.getSuperclass()) {
                    if (p == root) {
                        root = clazz;
                        continue;
                    }
                }
                for (Class p = root; p != Object.class; p = p.getSuperclass()) {
                    if (p == clazz) {
                        continue;
                    }
                }
                throw new ConstructionException("Classes " + root.getClass().getName() + " and " + clazz.getName() + " are not in the same hierarchy");
            }
        }
        return root;
    }

    public boolean canCreate(Type type) {
        return true;
    }

    public Object loadObject(Object proxy) throws Exception {
        Object svc = tracker.getService();
        if (svc == null && metadata.getTimeout() > 0) {
            // TODO: send WAIT event
            svc = tracker.waitForService(metadata.getTimeout());
        }
        if (svc == null) {
            throw new ServiceUnavailableException("Timeout expired when waiting for OSGi service", type, filter);
        }
        return svc;
    }

    public void init() throws Exception {
    }

    public void destroy() throws Exception {
        ServiceTracker t = tracker;
        tracker = null;
        if (t != null) {
            t.close();
        }
    }

    public Object addingService(ServiceReference reference) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modifiedService(ServiceReference reference, Object service) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removedService(ServiceReference reference, Object service) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

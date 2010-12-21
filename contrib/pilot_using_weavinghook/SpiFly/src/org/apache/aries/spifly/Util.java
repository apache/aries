/**
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
package org.apache.aries.spifly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;

/** 
 * Methods used from ASM-generated code. They store, change and reset the thread context classloader.
 * The methods are static to make it easy to access them from generated code.
 */
public class Util {
    static ThreadLocal<ClassLoader> storedClassLoaders = new ThreadLocal<ClassLoader>();
    
    public static void storeContextClassloader() {
        storedClassLoaders.set(Thread.currentThread().getContextClassLoader());
    }
    
    public static void restoreContextClassloader() {
        Thread.currentThread().setContextClassLoader(storedClassLoaders.get());
        storedClassLoaders.set(null);
    }
        
    public static void fixContextClassloader(String cls, String method, Class<?> clsArg, ClassLoader bundleLoader) {
        if (!(bundleLoader instanceof BundleReference)) {
            Activator.activator.log(LogService.LOG_WARNING, "Classloader of consuming bundle doesn't implement BundleReference: " + bundleLoader);
            return;
        }

        BundleReference br = ((BundleReference) bundleLoader);
        System.out.println("~~~ cls: " + cls + " method: " + method + " clarg:" + clsArg + " cl:" + bundleLoader + " clientBundle: " + br.getBundle().getSymbolicName());        
        
        ClassLoader cl = findContextClassloader(clsArg, br.getBundle());
        if (cl != null) {
            Activator.activator.log(LogService.LOG_INFO, "Temporarily setting Thread Context Classloader to: " + cl);
            Thread.currentThread().setContextClassLoader(cl);
        } else {
            Activator.activator.log(LogService.LOG_WARNING, "No classloader found for " + cls + ":" + method + "(" + clsArg + ")");
        }
    }
    
    private static ClassLoader findContextClassloader(Class<?> cls, Bundle consumerBundle) {
        Activator activator = Activator.activator;
        
        Collection<Bundle> bundles = new ArrayList<Bundle>(activator.findProviderBundles(cls.getName()));
        activator.log(LogService.LOG_DEBUG, "Found bundles providing " + cls + ": " + bundles);
                
        Collection<Bundle> allowedBundles = activator.findConsumerRestrictions(consumerBundle, 0, cls.getName());
        if (allowedBundles != null) {
            for (Iterator<Bundle> it = bundles.iterator(); it.hasNext(); ) {
                if (!allowedBundles.contains(it.next())) {
                    it.remove();
                }
            }
        }
        
        switch (bundles.size()) {
        case 0:
            return null;
        case 1:
            Bundle bundle = bundles.iterator().next();
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            return wiring.getClassLoader();            
        default:
            List<ClassLoader> loaders = new ArrayList<ClassLoader>();
            for (Bundle b : bundles) {
                BundleWiring bw = b.adapt(BundleWiring.class);
                loaders.add(bw.getClassLoader());
            }
            return new MultiDelegationClassloader(loaders.toArray(new ClassLoader[loaders.size()]));
        }
    }
}

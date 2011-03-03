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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.aries.spifly.api.SpiFlyConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Listens for new bundles being installed and registers them as service providers if applicable.
 */
public class ProviderBundleTrackerCustomizer implements BundleTrackerCustomizer<List<ServiceRegistration<?>>> {
    final Activator activator;
    final Bundle spiBundle;

    public ProviderBundleTrackerCustomizer(Activator a, Bundle b) {
        activator = a;
        spiBundle = b;
                
        // TODO handle pre-existing bundles.
    }

    public List<ServiceRegistration<?>> addingBundle(Bundle bundle, BundleEvent event) {
        log(LogService.LOG_INFO, "Bundle Considered for SPI providers: "
                + bundle.getSymbolicName());

        if (bundle.equals(spiBundle)) {
            return null;
        }

        if (bundle.getHeaders().get(SpiFlyConstants.SPI_PROVIDER_HEADER) == null) {
            log(LogService.LOG_INFO, "No '"
                    + SpiFlyConstants.SPI_PROVIDER_HEADER
                    + "' Manifest header. Skipping bundle: "
                    + bundle.getSymbolicName());
            return null;
        } else {
            log(LogService.LOG_INFO, "Examining bundle for SPI provider: "
                    + bundle.getSymbolicName());
        }

        Enumeration<URL> entries = bundle.findEntries("META-INF/services", "*", false);
        if (entries == null) {
            return null;
        }

        List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
        while (entries.hasMoreElements()) {
            URL url = entries.nextElement();
            log(LogService.LOG_INFO, "Found SPI resource: " + url);

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream()));
                String className = reader.readLine();
                // TODO need to read more than one class name!

                Class<?> cls = bundle.loadClass(className);
                Object o = cls.newInstance();
                log(LogService.LOG_INFO, "Instantiated SPI provider: " + o);

                Hashtable<String, Object> props = new Hashtable<String, Object>();
                props.put(SpiFlyConstants.SPI_PROVIDER_URL, url);

                String s = url.toExternalForm();
                int idx = s.lastIndexOf('/');
                String registrationClassName = className;
                if (s.length() > idx) {
                    registrationClassName = s.substring(idx + 1);
                }

                ServiceRegistration<?> reg = bundle.getBundleContext()
                        .registerService(registrationClassName, o, props);
                registrations.add(reg);

                activator.registerProviderBundle(registrationClassName, bundle);
                log(LogService.LOG_INFO, "Registered service: " + reg);                
            } catch (Exception e) {
                log(LogService.LOG_WARNING,
                        "Could not load SPI implementation referred from " + url, e);
            }
        }
        
        return registrations;
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, List<ServiceRegistration<?>> registrations) {
        // should really be doing something here...
    }

    public void removedBundle(Bundle bundle, BundleEvent event, List<ServiceRegistration<?>> registrations) {
        if (registrations == null)
            return;
        
        for (ServiceRegistration<?> reg : registrations) {
            reg.unregister();
            log(LogService.LOG_INFO, "Unregistered: " + reg);            
        }
    }

    private void log(int level, String message) {
        activator.log(level, message);
    }

    private void log(int level, String message, Throwable th) {
        activator.log(level, message, th);
    }
}

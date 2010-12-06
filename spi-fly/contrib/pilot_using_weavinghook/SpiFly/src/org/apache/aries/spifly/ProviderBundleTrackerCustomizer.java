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

        Enumeration<?> entries = bundle.findEntries("META-INF/services", "*", false);
        if (entries == null) {
            return null;
        }

        List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
        while (entries.hasMoreElements()) {
            URL url = (URL) entries.nextElement();
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

                activator.registerSPIProviderBundle(registrationClassName, bundle);
                log(LogService.LOG_INFO, "Registered service: " + reg);                
            } catch (Exception e) {
                log(LogService.LOG_WARNING,
                        "Could not load SPI implementation referred from " + url, e);
            }
        }
        

        /*
        // the new approach - services are being registered based on contents of
        // the SPI-Provider header
        if (bundleClassloader == null) {
            log(LogService.LOG_INFO, "Unable to identify classloader. "
                    + "Skipping SPIClassloaderAdviceService(s) registeration.");
        } else {

            // Register SPIClassloaderAdviceService services for APIs mentioned
            // in the header
            String spiProviderHeader = (String) bundle.getHeaders().get(
                    SpiFlyConstants.SPI_PROVIDER_HEADER);
            List<PathElement> parsedHeader = HeaderParser
                    .parseHeader(spiProviderHeader);
            for (PathElement pe : parsedHeader) {

                // Format of each path element:
                // api1Name;provider-name="myimpl2";service-ids="myserviceId"

                // An example below.
                // Please note:
                // 1. The service-ids attribute holds a list of ids that will be
                // used when searching META-INF/services/. In other words
                // this will be the name of the class that will be passed to
                // ServiceLoader.load().
                // 2. A single bundle can provide implementations for many APIs
                // - there might be many api names in a single SPI-Provider
                // header.

                // Sample:
                // jaxb;provider-name="xerces123";service-ids="javax.xml.bind.JAXBContext"

                // the clause begins with a name of the API for which this
                // bundle provides an impl
                String apiName = pe.getName();
                // unique name of the provider
                String providerName = pe
                        .getAttribute(SpiFlyConstants.PROVIDER_NAME_ATTRIBUTE);
                providerName = trimQuotes(providerName);

                // 
                String serviceIds = pe
                        .getAttribute(SpiFlyConstants.SERVICE_IDS_ATTRIBUTE);
                serviceIds = trimQuotes(serviceIds);

                if (apiName == null || providerName == null
                        || serviceIds == null) {
                    log(LogService.LOG_INFO, "Skipping: " + apiName + " "
                            + providerName + " " + serviceIds
                            + ". Null values are not allowed.");
                }

                StringTokenizer tokenizer = new StringTokenizer(serviceIds, ",");
                while (tokenizer.hasMoreTokens()) {
                    String serviceId = tokenizer.nextToken().trim();
                    SPIClassloaderAdviceServiceImpl service = new SPIClassloaderAdviceServiceImpl(
                            bundleClassloader);
                    Hashtable<String, Object> props = new Hashtable<String, Object>();
                    props.put(SpiFlyConstants.API_NAME_SERVICE_ATTRIBUTE,
                            apiName);
                    props.put(SpiFlyConstants.PROVIDER_NAME_SERVICE_ATTRIBUTE,
                            providerName);
                    props.put(SpiFlyConstants.SERVICE_ID_SERVICE_ATTRIBUTE,
                            serviceId);
                    ServiceRegistration reg = bundle
                            .getBundleContext()
                            .registerService(
                                    SPIClassloaderAdviceService.class.getName(),
                                    service, props);
                    registrations.add(reg);
                    log(LogService.LOG_INFO, "Registered service: " + reg);
                }
            }
            
        } */

        return registrations;
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, List<ServiceRegistration<?>> registrations) {
        // nothing to do here
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

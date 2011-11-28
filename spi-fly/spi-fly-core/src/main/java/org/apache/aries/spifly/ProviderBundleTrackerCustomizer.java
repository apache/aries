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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.aries.spifly.api.SpiFlyConstants;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Listens for new bundles being installed and registers them as service providers if applicable.
 */
public class ProviderBundleTrackerCustomizer implements BundleTrackerCustomizer {
    private static final String METAINF_SERVICES = "META-INF/services";

    final BaseActivator activator;
    final Bundle spiBundle;

    public ProviderBundleTrackerCustomizer(BaseActivator activator, Bundle spiBundle) {
        this.activator = activator;
        this.spiBundle = spiBundle;
    }

    public List<ServiceRegistration> addingBundle(Bundle bundle, BundleEvent event) {
        log(LogService.LOG_INFO, "Bundle Considered for SPI providers: "
                + bundle.getSymbolicName());

        if (bundle.equals(spiBundle)) {
            return null;
        }

        List<String> providedServices = null;
        Map<String, Object> customAttributes = new HashMap<String, Object>();
        Map<String, String> directives = new HashMap<String, String>();
        if (bundle.getHeaders().get("Provide-Capability") != null) {
            providedServices = readProvideCapability(bundle.getHeaders(), directives, customAttributes);

            if (!"active".equals(directives.get(SpiFlyConstants.EFFECTIVE_DIRECTIVE))) {
                log(LogService.LOG_INFO, "Effective is not equal to 'active'. Not processing bundle " + bundle.getSymbolicName());
                return null;
            }
        }

        if (providedServices == null && bundle.getHeaders().get(SpiFlyConstants.SPI_PROVIDER_HEADER) != null) {
            providedServices = new ArrayList<String>();
        }

        if (providedServices == null) {
            log(LogService.LOG_INFO, "No '"
                    + SpiFlyConstants.SPI_PROVIDER_HEADER
                    + "' Manifest header. Skipping bundle: "
                    + bundle.getSymbolicName());
            return null;
        } else {
            log(LogService.LOG_INFO, "Examining bundle for SPI provider: "
                    + bundle.getSymbolicName());
        }

        URL servicesDir = bundle.getResource("/" + METAINF_SERVICES);
        if (servicesDir == null)
            return null;

        List<URL> serviceFiles = new ArrayList<URL>();

        @SuppressWarnings("unchecked")
        Enumeration<URL> entries = bundle.findEntries(METAINF_SERVICES, "*", false);
        if (entries != null) {
            serviceFiles.addAll(Collections.list(entries));
        }

        Object bcp = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if (bcp instanceof String) {
            for (String entry : ((String) bcp).split(",")) {
                entry = entry.trim();
                if (entry.equals("."))
                    continue;

                URL url = bundle.getResource(entry);
                if (url != null) {
                    serviceFiles.addAll(getMetaInfServiceURLsFromJar(url));
                }
            }
        }

        List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
        for (URL serviceFile : serviceFiles) {
            log(LogService.LOG_INFO, "Found SPI resource: " + serviceFile);

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serviceFile.openStream()));
                String className = null;
                while((className = reader.readLine()) != null) {
                    try {
                        if (className.startsWith("#"))
                            continue; // a comment

                        String s = serviceFile.toExternalForm();
                        int idx = s.lastIndexOf('/');
                        String registrationClassName = className;
                        if (s.length() > idx) {
                            registrationClassName = s.substring(idx + 1);
                        }
                        if (providedServices.size() > 0 && !providedServices.contains(registrationClassName))
                            continue;

                        Class<?> cls = bundle.loadClass(className);
                        Object o = cls.newInstance();
                        log(LogService.LOG_INFO, "Instantiated SPI provider: " + o);

                        Hashtable<String, Object> props = new Hashtable<String, Object>();
                        props.put(SpiFlyConstants.SPI_PROVIDER_URL, serviceFile);
                        props.putAll(customAttributes);

                        if (!"false".equalsIgnoreCase(directives.get(SpiFlyConstants.SERVICE_REGISTRY_DIRECTIVE))) {
                            ServiceRegistration reg = bundle.getBundleContext()
                                    .registerService(registrationClassName, o, props);
                            registrations.add(reg);
                            log(LogService.LOG_INFO, "Registered service: " + reg);
                        }

                        activator.registerProviderBundle(registrationClassName, bundle, customAttributes);
                        log(LogService.LOG_INFO, "Registered provider: " + registrationClassName + " in bundle " + bundle.getSymbolicName());
                    } catch (Exception e) {
                        log(LogService.LOG_WARNING,
                                "Could not load SPI implementation referred from " + serviceFile, e);
                    }
                }
            } catch (IOException e) {
                log(LogService.LOG_WARNING, "Could not read SPI metadata from " + serviceFile, e);
            }
        }

        return registrations;
    }

    // An empty list returned means 'all SPIs'
    // A return value of null means no SPIs
    // A populated list means: only these SPIs
    @SuppressWarnings("unchecked")
    private List<String> readProvideCapability(Dictionary<?,?> headers, Map<String, String> directives, Map<String, Object> customAttributes) {
        Object capabilityHeader = headers.get(SpiFlyConstants.PROVIDE_CAPABILITY);
        if (capabilityHeader == null)
            return null;

        List<GenericMetadata> capabilities = ManifestHeaderProcessor.parseCapabilityString(capabilityHeader.toString());
        for (GenericMetadata cap : capabilities) {
            if (!SpiFlyConstants.SPI_CAPABILITY_NAMESPACE.equals(cap.getNamespace()))
                continue;

            List<String> serviceNames = new ArrayList<String>();
            for (Map.Entry<String, Object> entry : cap.getAttributes().entrySet()) {
                if (SpiFlyConstants.SERVICE_ATTRIBUTE.equals(entry.getKey())) {
                    if (entry.getValue() instanceof String) {
                        serviceNames.add((String) entry.getValue());
                    } else if (entry.getValue() instanceof String []) {
                        serviceNames.addAll(Arrays.asList((String []) entry.getValue()));
                    } else if (entry.getValue() instanceof List) {
                        serviceNames.addAll((List<String>) entry.getValue());
                    }
                } else {
                    customAttributes.put(entry.getKey(), entry.getValue());
                }
            }

            directives.putAll(cap.getDirectives());
            return serviceNames;
        }
        return null;
    }

    private List<URL> getMetaInfServiceURLsFromJar(URL url) {
        List<URL> urls = new ArrayList<URL>();
        try {
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(url.openStream());

                JarEntry je = null;
                while((je = jis.getNextJarEntry()) != null) {
                    if (je.getName().startsWith(METAINF_SERVICES) &&
                        je.getName().length() > (METAINF_SERVICES.length() + 1)) {
                        urls.add(new URL("jar:" + url + "!/" + je.getName()));
                    }
                }
            } finally {
                if (jis != null) {
                    jis.close();
                }
            }
        } catch (IOException e) {
            log(LogService.LOG_ERROR, "Problem opening embedded jar file: " + url, e);
        }
        return urls;
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        // should really be doing something here...
    }

    @SuppressWarnings("unchecked")
    public void removedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        if (registrations == null)
            return;

        for (ServiceRegistration reg : (List<ServiceRegistration>) registrations) {
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

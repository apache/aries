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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;
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

    public List<ServiceRegistration> addingBundle(final Bundle bundle, BundleEvent event) {
        log(LogService.LOG_INFO, "Bundle Considered for SPI providers: "
                + bundle.getSymbolicName());

        if (bundle.equals(spiBundle))
            return null; // don't process the SPI bundle itself

        List<String> providedServices = null;
        Map<String, Object> customAttributes = new HashMap<String, Object>();
        if (bundle.getHeaders().get(SpiFlyConstants.REQUIRE_CAPABILITY) != null) {
            try {
                providedServices = readServiceLoaderMediatorCapabilityMetadata(bundle.getHeaders(), customAttributes);
            } catch (InvalidSyntaxException e) {
                log(LogService.LOG_ERROR, "Unable to read capabilities from bundle " + bundle, e);
            }
        }

        boolean fromSPIProviderHeader = false;
        if (providedServices == null && bundle.getHeaders().get(SpiFlyConstants.SPI_PROVIDER_HEADER) != null) {
            String header = bundle.getHeaders().get(SpiFlyConstants.SPI_PROVIDER_HEADER).toString().trim();
            if ("*".equals(header)) {
                providedServices = new ArrayList<String>();
            } else {
                providedServices = Arrays.asList(header.split(","));
            }
            fromSPIProviderHeader = true;
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

        for (String svc : providedServices) {
            // Eagerly register any services that are explicitly listed, as they may not be found in META-INF/services
            activator.registerProviderBundle(svc, bundle, customAttributes);
        }

        URL servicesDir = bundle.getResource("/" + METAINF_SERVICES);
        if (servicesDir == null)
            return null;

        List<URL> serviceFileURLs = new ArrayList<URL>();

        @SuppressWarnings("unchecked")
        Enumeration<URL> entries = bundle.findEntries(METAINF_SERVICES, "*", false);
        if (entries != null) {
            serviceFileURLs.addAll(Collections.list(entries));
        }

        Object bcp = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if (bcp instanceof String) {
            for (String entry : ((String) bcp).split(",")) {
                entry = entry.trim();
                if (entry.equals("."))
                    continue;

                URL url = bundle.getResource(entry);
                if (url != null) {
                    serviceFileURLs.addAll(getMetaInfServiceURLsFromJar(url));
                }
            }
        }

        final List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
        for (URL serviceFileURL : serviceFileURLs) {
            log(LogService.LOG_INFO, "Found SPI resource: " + serviceFileURL);

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serviceFileURL.openStream()));
                String className = null;
                while((className = reader.readLine()) != null) {
                    try {
                        className = className.trim();

                        if (className.length() == 0)
                            continue; // empty line

                        if (className.startsWith("#"))
                            continue; // a comment

                        String serviceFile = serviceFileURL.toExternalForm();
                        int idx = serviceFile.lastIndexOf('/');
                        String registrationClassName = className;
                        if (serviceFile.length() > idx) {
                            registrationClassName = serviceFile.substring(idx + 1);
                        }

                        if (providedServices.size() > 0 && !providedServices.contains(registrationClassName))
                            continue;

                        final Class<?> cls = bundle.loadClass(className);
                        log(LogService.LOG_INFO, "Loaded SPI provider: " + cls);

                        final Hashtable<String, Object> properties;
                        if (fromSPIProviderHeader)
                            properties = new Hashtable<String, Object>();
                        else
                            properties = findServiceRegistrationProperties(bundle.getHeaders(), registrationClassName, className);

                        if (properties != null) {
                            properties.put(SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY, spiBundle.getBundleId());
                            properties.put(SpiFlyConstants.PROVIDER_IMPLCLASS_PROPERTY, cls.getName());

                            ServiceRegistration reg = null;
                            SecurityManager sm = System.getSecurityManager();
                            if (sm != null) {
                                if (bundle.hasPermission(new ServicePermission(registrationClassName, ServicePermission.REGISTER))) {
                                    System.err.println("*** Found security manager and bundle has permission to register: " + bundle);
                                    reg = bundle.getBundleContext().registerService(
                                            registrationClassName, new ProviderServiceFactory(cls), properties);
                                } else {
                                    System.err.println("*** Found security manager and bundle has NO permission to register: " + bundle);
                                    log(LogService.LOG_INFO, "Bundle " + bundle + " does not have the permission to register services of type: " + registrationClassName);
                                }
                            } else {
                                reg = bundle.getBundleContext().registerService(
                                        registrationClassName, new ProviderServiceFactory(cls), properties);
                            }

                            if (reg != null) {
                                registrations.add(reg);
                                log(LogService.LOG_INFO, "Registered service: " + reg);
                            }
                        }

                        activator.registerProviderBundle(registrationClassName, bundle, customAttributes);
                        log(LogService.LOG_INFO, "Registered provider: " + registrationClassName + " in bundle " + bundle.getSymbolicName());
                    } catch (Exception e) {
                        log(LogService.LOG_WARNING,
                                "Could not load SPI implementation referred from " + serviceFileURL, e);
                    }
                }
            } catch (IOException e) {
                log(LogService.LOG_WARNING, "Could not read SPI metadata from " + serviceFileURL, e);
            }
        }

        return registrations;
    }

    // An empty list returned means 'all SPIs'
    // A return value of null means no SPIs
    // A populated list means: only these SPIs
    private List<String> readServiceLoaderMediatorCapabilityMetadata(Dictionary<?,?> headers, Map<String, Object> customAttributes) throws InvalidSyntaxException {
        Object requirementHeader = headers.get(SpiFlyConstants.REQUIRE_CAPABILITY);
        if (requirementHeader == null)
            return null;

        List<GenericMetadata> requirements = ManifestHeaderProcessor.parseRequirementString(requirementHeader.toString());
        GenericMetadata extenderRequirement = findRequirement(requirements, SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE, SpiFlyConstants.REGISTRAR_EXTENDER_NAME);
        if (extenderRequirement == null)
            return null;

        List<GenericMetadata> capabilities;
        Object capabilityHeader = headers.get(SpiFlyConstants.PROVIDE_CAPABILITY);
        if (capabilityHeader == null) {
            capabilities = Collections.emptyList();
        } else {
            capabilities = ManifestHeaderProcessor.parseCapabilityString(capabilityHeader.toString());
        }

        List<String> serviceNames = new ArrayList<String>();
        for (GenericMetadata serviceLoaderCapability : findAllMetadata(capabilities, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE)) {
            for (Map.Entry<String, Object> entry : serviceLoaderCapability.getAttributes().entrySet()) {
                if (SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(entry.getKey())) {
                    serviceNames.add(entry.getValue().toString());
                    continue;
                }

                customAttributes.put(entry.getKey(), entry.getValue());
            }
        }
        return serviceNames;
    }

    // null means don't register,
    // otherwise the return value should be taken as the service registration properties
    private Hashtable<String, Object> findServiceRegistrationProperties(Dictionary<?,?> headers, String spiName, String implName) {
        Object capabilityHeader = headers.get(SpiFlyConstants.PROVIDE_CAPABILITY);
        if (capabilityHeader == null)
            return null;

        List<GenericMetadata> capabilities = ManifestHeaderProcessor.parseCapabilityString(capabilityHeader.toString());
        GenericMetadata cap = findCapability(capabilities, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE, spiName);

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        if (cap != null) {
            for (Map.Entry<String, Object> entry : cap.getAttributes().entrySet()) {
                if (SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(entry.getKey()))
                    continue;

                if (!entry.getKey().startsWith("."))
                    properties.put(entry.getKey(), entry.getValue());
            }
        }

        String registerDirective = cap.getDirectives().get(SpiFlyConstants.REGISTER_DIRECTIVE);
        if (registerDirective == null) {
            return properties;
        } else {
            if ("".equals(registerDirective.trim()))
                return null;

            if (implName.equals(registerDirective.trim()))
                return properties;
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

    private GenericMetadata findCapability(List<GenericMetadata> capabilities, String namespace, String spiName) {
        for (GenericMetadata cap : capabilities) {
            if (namespace.equals(cap.getNamespace())) {
                if (spiName.equals(cap.getAttributes().get(namespace))) {
                    return cap;
                }
            }
        }
        return null;
    }

    private static Collection<GenericMetadata> findAllMetadata(List<GenericMetadata> requirementsOrCapabilities, String namespace) {
        List<GenericMetadata> reqsCaps = new ArrayList<ManifestHeaderProcessor.GenericMetadata>();
        for (GenericMetadata reqCap : requirementsOrCapabilities) {
            if (namespace.equals(reqCap.getNamespace())) {
                reqsCaps.add(reqCap);
            }
        }
        return reqsCaps;
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        // should really be doing something here...
    }

    @SuppressWarnings("unchecked")
    public void removedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        activator.unregisterProviderBundle(bundle);

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

    private static GenericMetadata findRequirement(List<GenericMetadata> requirements, String namespace, String type) throws InvalidSyntaxException {
        Dictionary<String, String> nsAttr = new Hashtable<String, String>();
        nsAttr.put(namespace, type);

        for (GenericMetadata req : requirements) {
            if (namespace.equals(req.getNamespace())) {
                String filterString = req.getDirectives().get(SpiFlyConstants.FILTER_DIRECTIVE);
                if (filterString != null) {
                    Filter filter = FrameworkUtil.createFilter(filterString);
                    if (filter.match(nsAttr)) {
                        return req;
                    }
                }
            }
        }
        return null;
    }
}

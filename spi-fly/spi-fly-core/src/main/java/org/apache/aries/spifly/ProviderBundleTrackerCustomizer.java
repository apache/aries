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
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

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

    @Override
    public List<ServiceRegistration> addingBundle(final Bundle bundle, BundleEvent event) {
        log(Level.FINE, "Bundle Considered for SPI providers: "
                + bundle.getSymbolicName());

        if (bundle.equals(spiBundle))
            return null; // don't process the SPI bundle itself

        List<String> providedServices = null;
        Map<String, Object> customAttributes = new HashMap<String, Object>();
        if (bundle.getHeaders().get(SpiFlyConstants.REQUIRE_CAPABILITY) != null) {
            try {
                providedServices = readServiceLoaderMediatorCapabilityMetadata(bundle, customAttributes);
            } catch (InvalidSyntaxException e) {
                log(Level.SEVERE, "Unable to read capabilities from bundle " + bundle, e);
            }
        }

        boolean fromSPIProviderHeader = false;
        String spiProviderHeader = getHeaderFromBundleOrFragment(bundle, SpiFlyConstants.SPI_PROVIDER_HEADER);
        if (providedServices == null && spiProviderHeader != null) {
            String header = spiProviderHeader.trim();
            if ("*".equals(header)) {
                providedServices = new ArrayList<String>();
            } else {
                providedServices = Arrays.asList(header.split(","));
            }
            fromSPIProviderHeader = true;
        }

        if (providedServices == null) {
            log(Level.FINE, "No '"
                    + SpiFlyConstants.SPI_PROVIDER_HEADER
                    + "' Manifest header. Skipping bundle: "
                    + bundle.getSymbolicName());
            return null;
        } else {
            log(Level.INFO, "Examining bundle for SPI provider: "
                    + bundle.getSymbolicName());
        }

        for (String svc : providedServices) {
            // Eagerly register any services that are explicitly listed, as they may not be found in META-INF/services
            activator.registerProviderBundle(svc, bundle, customAttributes);
        }

        List<URL> serviceFileURLs = new ArrayList<URL>();

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
            log(Level.INFO, "Found SPI resource: " + serviceFileURL);

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
                        log(Level.INFO, "Loaded SPI provider: " + cls);

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
                                    reg = bundle.getBundleContext().registerService(
                                            registrationClassName, new ProviderServiceFactory(cls), properties);
                                } else {
                                    log(Level.INFO, "Bundle " + bundle + " does not have the permission to register services of type: " + registrationClassName);
                                }
                            } else {
                                reg = bundle.getBundleContext().registerService(
                                        registrationClassName, new ProviderServiceFactory(cls), properties);
                            }

                            if (reg != null) {
                                registrations.add(reg);
                                log(Level.INFO, "Registered service: " + reg);
                            }
                        }

                        activator.registerProviderBundle(registrationClassName, bundle, customAttributes);
                        log(Level.INFO, "Registered provider: " + registrationClassName + " in bundle " + bundle.getSymbolicName());
                    } catch (Exception e) {
                        log(Level.WARNING,
                                "Could not load SPI implementation referred from " + serviceFileURL, e);
                    }
                }
            } catch (IOException e) {
                log(Level.WARNING, "Could not read SPI metadata from " + serviceFileURL, e);
            }
        }

        return registrations;
    }

    private String getHeaderFromBundleOrFragment(Bundle bundle, String headerName) {
        return getHeaderFromBundleOrFragment(bundle, headerName, null);
    }

    private String getHeaderFromBundleOrFragment(Bundle bundle, String headerName, String matchString) {
        String val = bundle.getHeaders().get(headerName);
        if (matches(val, matchString))
            return val;

        BundleRevision rev = bundle.adapt(BundleRevision.class);
        if (rev != null) {
            BundleWiring wiring = rev.getWiring();
            if (wiring != null) {
                for (BundleWire wire : wiring.getProvidedWires("osgi.wiring.host")) {
                    Bundle fragment = wire.getRequirement().getRevision().getBundle();
                    val = fragment.getHeaders().get(headerName);
                    if (matches(val, matchString)) {
                        return val;
                    }
                }
            }
        }

        return null;
    }

    private boolean matches(String val, String matchString) {
        if (val == null)
            return false;

        if (matchString == null)
            return true;

        int idx = val.indexOf(matchString);
        return idx >= 0;
    }

    // An empty list returned means 'all SPIs'
    // A return value of null means no SPIs
    // A populated list means: only these SPIs
    private List<String> readServiceLoaderMediatorCapabilityMetadata(Bundle bundle, Map<String, Object> customAttributes) throws InvalidSyntaxException {
        String requirementHeader = getHeaderFromBundleOrFragment(bundle, SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);
        if (requirementHeader == null)
            return null;

        Parameters requirements = OSGiHeader.parseHeader(requirementHeader);
        Entry<String, Attrs> extenderRequirement = ConsumerHeaderProcessor.findRequirement(requirements, SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE, SpiFlyConstants.REGISTRAR_EXTENDER_NAME);
        if (extenderRequirement == null)
            return null;

        Parameters capabilities;
        String capabilityHeader = getHeaderFromBundleOrFragment(bundle, SpiFlyConstants.PROVIDE_CAPABILITY, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);
        if (capabilityHeader == null) {
            capabilities = new Parameters();
        } else {
            capabilities = OSGiHeader.parseHeader(capabilityHeader);
        }

        List<String> serviceNames = new ArrayList<String>();
        for (Entry<String, Attrs> serviceLoaderCapability : ConsumerHeaderProcessor.findAllMetadata(capabilities, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE)) {
            for (Entry<String, String> entry : serviceLoaderCapability.getValue().entrySet()) {
                if (SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(entry.getKey())) {
                    serviceNames.add(entry.getValue().toString());
                    continue;
                }
                if (SpiFlyConstants.REGISTER_DIRECTIVE.equals(entry.getKey()) && entry.getValue().equals("")) {
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

        Parameters capabilities = OSGiHeader.parseHeader(capabilityHeader.toString());
        Entry<String, Attrs> cap = ConsumerHeaderProcessor.findCapability(capabilities, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE, spiName);

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        if (cap != null) {
            for (Map.Entry<String, String> entry : cap.getValue().entrySet()) {
                String key = ConsumerHeaderProcessor.removeDuplicateMarker(entry.getKey());
                if (SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(key))
                    continue;

                if (!key.startsWith("."))
                    properties.put(entry.getKey(), entry.getValue());
            }
        }

        String registerDirective = cap.getValue().get(SpiFlyConstants.REGISTER_DIRECTIVE);
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
            log(Level.SEVERE, "Problem opening embedded jar file: " + url, e);
        }
        return urls;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        // should really be doing something here...
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        activator.unregisterProviderBundle(bundle);

        if (registrations == null)
            return;

        for (ServiceRegistration reg : (List<ServiceRegistration>) registrations) {
            reg.unregister();
            log(Level.INFO, "Unregistered: " + reg);
        }
    }

    private void log(Level level, String message) {
        activator.log(level, message);
    }

    private void log(Level level, String message, Throwable th) {
        activator.log(level, message, th);
    }
}

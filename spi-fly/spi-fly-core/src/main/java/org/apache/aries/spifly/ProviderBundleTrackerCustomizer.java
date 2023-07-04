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

import static java.util.stream.Collectors.toList;
import static org.osgi.framework.wiring.BundleRevision.TYPE_FRAGMENT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.stream.Stream;

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
import aQute.bnd.stream.MapStream;
import aQute.libg.glob.Glob;

/**
 * Listens for new bundles being installed and registers them as service providers if applicable.
 */
@SuppressWarnings("rawtypes")
public class ProviderBundleTrackerCustomizer implements BundleTrackerCustomizer {
    private static final String METAINF_SERVICES = "META-INF/services";
    private static final List<String> MERGE_HEADERS = Arrays.asList(
        Constants.IMPORT_PACKAGE, Constants.REQUIRE_BUNDLE, Constants.EXPORT_PACKAGE,
        Constants.PROVIDE_CAPABILITY, Constants.REQUIRE_CAPABILITY);

    final BaseActivator activator;
    final Bundle spiBundle;

    public ProviderBundleTrackerCustomizer(BaseActivator activator, Bundle spiBundle) {
        this.activator = activator;
        this.spiBundle = spiBundle;
    }

    @Override
    public List<ServiceRegistration> addingBundle(final Bundle bundle, BundleEvent event) {
        BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
        if (bundle.equals(spiBundle) || ((bundleRevision != null) && ((bundleRevision.getTypes() & TYPE_FRAGMENT) == TYPE_FRAGMENT)))
            return null; // don't process the SPI bundle itself

        log(Level.FINE, "Bundle Considered for SPI providers: "
            + bundle.getSymbolicName());

        DiscoveryMode discoveryMode = DiscoveryMode.SERVICELOADER_CAPABILITIES;
        List<String> providedServices = null;
        Map<String, Object> customAttributes = new HashMap<String, Object>();
        if (bundle.getHeaders().get(SpiFlyConstants.REQUIRE_CAPABILITY) != null) {
            try {
                providedServices = readServiceLoaderMediatorCapabilityMetadata(bundle, customAttributes);
            } catch (InvalidSyntaxException e) {
                log(Level.FINE, "Unable to read capabilities from bundle " + bundle, e);
            }
        }

        String spiProviderHeader = getHeaderFromBundleOrFragment(bundle, SpiFlyConstants.SPI_PROVIDER_HEADER);
        if (providedServices == null && spiProviderHeader != null) {
            String header = spiProviderHeader.trim();
            if ("*".equals(header)) {
                providedServices = new ArrayList<String>();
            } else {
                providedServices = Stream.of(header.split(",")).map(String::trim).collect(toList());
            }
            discoveryMode = DiscoveryMode.SPI_PROVIDER_HEADER;
        }

        List<URL> serviceFileURLs = null;
        if (providedServices == null) {
            Entry<List<String>, List<URL>> autoServices = getFromAutoProviderProperty(bundle, customAttributes);

            providedServices = autoServices.getKey();
            serviceFileURLs = autoServices.getValue();
            discoveryMode = DiscoveryMode.AUTO_PROVIDERS_PROPERTY;
        }

        if (providedServices == null) {
            log(Level.FINE, "No provided SPI services. Skipping bundle: "
                    + bundle.getSymbolicName());
            return null;
        } else {
            log(Level.FINE, "Examining bundle for SPI provider: "
                    + bundle.getSymbolicName());
        }

        for (String serviceType : providedServices) {
            // Eagerly register any services that are explicitly listed, as they may not be found in META-INF/services
            activator.registerProviderBundle(serviceType, bundle, customAttributes);
        }

        if (serviceFileURLs == null) {
            serviceFileURLs = getServiceFileUrls(bundle);
        }

        final List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
        for (ServiceDetails details : collectServiceDetails(bundle, serviceFileURLs, discoveryMode)) {
            if (providedServices.size() > 0 && !providedServices.contains(details.serviceType))
                continue;

            try {
                final Class<?> cls = bundle.loadClass(details.instanceType);
                log(Level.FINE, "Loaded SPI provider: " + cls);

                if (details.properties != null) {
                    ServiceRegistration reg = null;
                    Object instance =
                        (details.properties.containsKey("service.scope") &&
                        "prototype".equalsIgnoreCase(String.valueOf(details.properties.get("service.scope")))) ?
                            new ProviderPrototypeServiceFactory(cls) :
                            new ProviderServiceFactory(cls);

                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        if (bundle.hasPermission(new ServicePermission(details.serviceType, ServicePermission.REGISTER))) {
                            reg = bundle.getBundleContext().registerService(
                                    details.serviceType, instance, details.properties);
                        } else {
                            log(Level.FINE, "Bundle " + bundle + " does not have the permission to register services of type: " + details.serviceType);
                        }
                    } else {
                        reg = bundle.getBundleContext().registerService(
                            details.serviceType, instance, details.properties);
                    }

                    if (reg != null) {
                        registrations.add(reg);
                        log(Level.FINE, "Registered service: " + reg);
                    }
                }

                activator.registerProviderBundle(details.serviceType, bundle, details.properties);
                log(Level.INFO, "Registered provider " + details.instanceType + " of service " + details.serviceType + " in bundle " + bundle.getSymbolicName());
            } catch (Exception | NoClassDefFoundError e) {
                log(Level.FINE,
                    "Could not load provider " + details.instanceType + " of service " + details.serviceType, e);
            }
        }

        return registrations;
    }

    private List<ServiceDetails> collectServiceDetails(Bundle bundle, List<URL> serviceFileURLs, DiscoveryMode discoveryMode) {
        List<ServiceDetails> serviceDetails = new ArrayList<>();

        for (URL serviceFileURL : serviceFileURLs) {
            log(Level.FINE, "Found SPI resource: " + serviceFileURL);

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

                        final Hashtable<String, Object> properties;
                        if (discoveryMode == DiscoveryMode.SPI_PROVIDER_HEADER) {
                            properties = new Hashtable<String, Object>();
                        }
                        else if (discoveryMode == DiscoveryMode.AUTO_PROVIDERS_PROPERTY) {
                            properties = activator.getAutoProviderInstructions().map(
                                Parameters::stream
                            ).orElseGet(MapStream::empty).filterKey(
                                i -> Glob.toPattern(i).asPredicate().test(bundle.getSymbolicName())
                            ).values().findFirst().map(
                                Hashtable<String, Object>::new
                            ).orElseGet(() -> new Hashtable<String, Object>());
                        }
                        else {
                            properties = findServiceRegistrationProperties(bundle, registrationClassName, className);
                        }

                        if (properties != null) {
                            properties.put(SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY, spiBundle.getBundleId());
                            properties.put(SpiFlyConstants.PROVIDER_IMPLCLASS_PROPERTY, className);
                            properties.put(SpiFlyConstants.PROVIDER_DISCOVERY_MODE, discoveryMode.toString());
                        }

                        serviceDetails.add(new ServiceDetails(registrationClassName, className, properties));
                    } catch (Exception e) {
                        log(Level.FINE,
                                "Could not load SPI implementation referred from " + serviceFileURL, e);
                    }
                }
            } catch (IOException e) {
                log(Level.FINE, "Could not read SPI metadata from " + serviceFileURL, e);
            }
        }

        return serviceDetails;
    }
    private Entry<List<String>, List<URL>> getFromAutoProviderProperty(Bundle bundle, Map<String, Object> customAttributes) {
        return activator.getAutoProviderInstructions().map(
            Parameters::stream
        ).orElseGet(MapStream::empty).filterKey(
            i ->
                Glob.toPattern(i).asPredicate().test(bundle.getSymbolicName())
        ).values().findFirst().map(
            un -> {
                List<URL> serviceFileURLs = getServiceFileUrls(bundle);

                List<ServiceDetails> collectServiceDetails = collectServiceDetails(bundle, serviceFileURLs, DiscoveryMode.AUTO_PROVIDERS_PROPERTY);

                collectServiceDetails.stream().map(ServiceDetails::getProperties).filter(Objects::nonNull).forEach(
                    hashtable -> hashtable.forEach(customAttributes::put)
                );

                List<String> providedServices = collectServiceDetails.stream().map(ServiceDetails::getServiceType).collect(toList());

                return new AbstractMap.SimpleImmutableEntry<>(providedServices, serviceFileURLs);
            }
        ).orElseGet(() -> new AbstractMap.SimpleImmutableEntry<>(null, null));
    }

    private List<URL> getServiceFileUrls(Bundle bundle) {
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

        return serviceFileURLs;
    }

    private String getHeaderFromBundleOrFragment(Bundle bundle, String headerName) {
        return getHeaderFromBundleOrFragment(bundle, headerName, null);
    }

    private String getHeaderFromBundleOrFragment(Bundle bundle, String headerName, String matchString) {
        Parameters headerParameters = new Parameters(bundle.getHeaders().get(headerName));
        if (matches(headerParameters.toString(), matchString) && !MERGE_HEADERS.contains(headerName)) {
            return headerParameters.isEmpty() ? null : headerParameters.toString();
        }

        BundleRevision rev = bundle.adapt(BundleRevision.class);
        if (rev != null) {
            BundleWiring wiring = rev.getWiring();
            if (wiring != null) {
                for (BundleWire wire : wiring.getProvidedWires("osgi.wiring.host")) {
                    Bundle fragment = wire.getRequirement().getRevision().getBundle();
                    Parameters fragmentParameters = new Parameters(fragment.getHeaders().get(headerName));
                    if (MERGE_HEADERS.contains(headerName)) {
                        headerParameters.mergeWith(fragmentParameters, false);
                    }
                    else {
                        headerParameters = fragmentParameters;
                    }

                    if (matches(headerParameters.toString(), matchString)) {
                        return headerParameters.toString();
                    }
                }
            }
        }

        return headerParameters.isEmpty() ? null : headerParameters.toString();
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
        Entry<String, ? extends Map<String, String>> extenderRequirement = ConsumerHeaderProcessor.findRequirement(requirements, SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE, SpiFlyConstants.REGISTRAR_EXTENDER_NAME);
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
        for (Entry<String, ? extends Map<String, String>> serviceLoaderCapability : ConsumerHeaderProcessor.findAllMetadata(capabilities, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE)) {
            for (Entry<String, String> entry : serviceLoaderCapability.getValue().entrySet()) {
                if (SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(entry.getKey())) {
                    serviceNames.add(entry.getValue().trim());
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
    private Hashtable<String, Object> findServiceRegistrationProperties(Bundle bundle, String spiName, String implName) {
        Object capabilityHeader = getHeaderFromBundleOrFragment(bundle, SpiFlyConstants.PROVIDE_CAPABILITY);
        if (capabilityHeader == null)
            return null;

        Parameters capabilities = OSGiHeader.parseHeader(capabilityHeader.toString());

        for (Map.Entry<String, Attrs> entry : capabilities.entrySet()) {
            String key = ConsumerHeaderProcessor.removeDuplicateMarker(entry.getKey());
            Attrs attrs = entry.getValue();

            if (!SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(key))
                continue;

            if (!attrs.containsKey(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE) ||
                    !attrs.get(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE).equals(spiName))
                continue;

            if (attrs.containsKey(SpiFlyConstants.REGISTER_DIRECTIVE) &&
                    !attrs.get(SpiFlyConstants.REGISTER_DIRECTIVE).equals(implName))
                continue;

            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            for (Map.Entry<String, String> prop : attrs.entrySet()) {
                if (SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(prop.getKey()) ||
                        SpiFlyConstants.REGISTER_DIRECTIVE.equals(prop.getKey()) ||
                        key.startsWith("."))
                    continue;

                properties.put(prop.getKey(), prop.getValue());
            }
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
            log(Level.FINE, "Problem opening embedded jar file: " + url, e);
        }
        return urls;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        // implementation is unnecessary for this use case
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        activator.unregisterProviderBundle(bundle);

        if (registrations == null)
            return;

        for (ServiceRegistration reg : (List<ServiceRegistration>) registrations) {
            try {
                reg.unregister();
                log(Level.FINE, "Unregistered: " + reg);
            } catch (IllegalStateException ise) {
                // Ignore the exception but do not remove the try/catch.
                // There are some bundle context races on cleanup which
                // are safe to ignore but unsafe not to perform our own
                // cleanup. In an ideal world ServiceRegistration.unregister()
                // would have been idempotent and never throw an exception.
            }
        }
    }

    private void log(Level level, String message) {
        activator.log(level, message);
    }

    private void log(Level level, String message, Throwable th) {
        activator.log(level, message, th);
    }

    enum DiscoveryMode {
        SPI_PROVIDER_HEADER,
        AUTO_PROVIDERS_PROPERTY,
        SERVICELOADER_CAPABILITIES
    }

    class ServiceDetails {
        public ServiceDetails(String serviceType, String instanceType, Hashtable<String, Object> properties) {
            this.serviceType = serviceType;
            this.instanceType = instanceType;
            this.properties = properties;
        }
        public String getInstanceType() {
            return instanceType;
        }
        public Hashtable<String, Object> getProperties() {
            return properties;
        }
        public String getServiceType() {
            return serviceType;
        }
        @Override
        public String toString() {
            return String.format(
                "ServiceDetails [serviceType=\"%s\", instanceType=\"%s\", properties=%s]",
                getServiceType(), getInstanceType(), getProperties());
        }
        private final String instanceType;
        private final Hashtable<String, Object> properties;
        private final String serviceType;
    }
}

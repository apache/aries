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
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.stream.MapStream;
import aQute.libg.glob.Glob;

/**
 * Listens for new bundles being installed and registers them as service providers if applicable.
 */
@SuppressWarnings("rawtypes")
public class ProviderBundleTrackerCustomizer implements BundleTrackerCustomizer {
    private static final String METAINF_SERVICES = "META-INF/services";
    private static final String REGISTER_DIRECTIVE_NAME = "register";
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
        List<BundleCapability> serviceLoaderCapabilities = Collections.emptyList();
        boolean registerServiceLoaderServices = false;
        Map<String, Object> customAttributes = new HashMap<String, Object>();
        BundleWiring wiring = WiringUtils.getWiring(bundle);
        if (wiring != null) {
            serviceLoaderCapabilities = wiring.getCapabilities(
                    SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);
            if (!serviceLoaderCapabilities.isEmpty()) {
                providedServices = new ArrayList<String>();
                for (BundleCapability capability : serviceLoaderCapabilities) {
                    Object serviceType = capability.getAttributes().get(
                            SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);
                    if (serviceType instanceof String) {
                        providedServices.add(((String) serviceType).trim());
                    }
                }
                registerServiceLoaderServices = WiringUtils.isWiredToExtender(
                        wiring, spiBundle, SpiFlyConstants.REGISTRAR_EXTENDER_NAME);
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
            // Keep active hosts tracked so a fragment attached later can add provider
            // capabilities and configuration resources to them.
            return new ArrayList<ServiceRegistration>();
        } else {
            log(Level.FINE, "Examining bundle for SPI provider: "
                    + bundle.getSymbolicName());
        }

        for (String serviceType : providedServices) {
            // Eagerly register any services that are explicitly listed, as they may not be found in META-INF/services
            // Keep every eligible bundle indexed so Conditional Permission Admin grants and
            // revocations can be observed by the lazy ServiceLoader view without reprocessing.
            activator.registerProviderBundle(serviceType, bundle, customAttributes);
        }

        if (serviceFileURLs == null) {
            serviceFileURLs = getServiceFileUrls(bundle,
                    discoveryMode == DiscoveryMode.SERVICELOADER_CAPABILITIES
                            ? providedServices : null);
        }

        final List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
        for (ServiceDetails details : collectServiceDetails(bundle, serviceFileURLs, discoveryMode,
                serviceLoaderCapabilities, registerServiceLoaderServices)) {
            if ((discoveryMode == DiscoveryMode.SERVICELOADER_CAPABILITIES
                    || providedServices.size() > 0)
                    && !providedServices.contains(details.serviceType))
                continue;

            try {
                final Class<?> cls = bundle.loadClass(details.instanceType);
                log(Level.FINE, "Loaded SPI provider: " + cls);

                if (details.properties != null
                        && hasRegisterPermission(bundle, details.serviceType)) {
                    ServiceRegistration reg = null;
                    Object instance = new ProviderServiceFactory(
                            cls, bundle, details.serviceType);

                    reg = bundle.getBundleContext().registerService(
                            details.serviceType, instance, details.properties);

                    if (reg != null) {
                        registrations.add(reg);
                        log(Level.FINE, "Registered service: " + reg);
                    }
                }

                activator.registerProviderBundle(details.serviceType,
                        details.instanceType, bundle,
                        details.properties == null
                                ? Collections.<String, Object>emptyMap() : details.properties);
                log(Level.INFO, "Registered provider " + details.instanceType + " of service " + details.serviceType + " in bundle " + bundle.getSymbolicName());
            } catch (Exception | NoClassDefFoundError e) {
                log(Level.FINE,
                    "Could not load provider " + details.instanceType + " of service " + details.serviceType, e);
            }
        }

        return registrations;
    }

    private boolean hasRegisterPermission(Bundle bundle, String serviceType) {
        boolean permitted = bundle.hasPermission(
                new ServicePermission(serviceType, ServicePermission.REGISTER));
        if (!permitted) {
            log(Level.FINE, "Bundle " + bundle
                    + " does not have permission to provide services of type: " + serviceType);
        }
        return permitted;
    }

    private List<ServiceDetails> collectServiceDetails(Bundle bundle, List<URL> serviceFileURLs,
            DiscoveryMode discoveryMode, List<BundleCapability> serviceLoaderCapabilities,
            boolean registerServiceLoaderServices) {
        List<ServiceDetails> serviceDetails = new ArrayList<>();

        for (Entry<String, List<String>> providerFile : readServiceProviderFiles(serviceFileURLs).entrySet()) {
            String registrationClassName = providerFile.getKey();
            for (String className : providerFile.getValue()) {
                try {
                    final List<Hashtable<String, Object>> registrations;
                    if (discoveryMode == DiscoveryMode.SPI_PROVIDER_HEADER) {
                        registrations = Collections.singletonList(new Hashtable<String, Object>());
                    }
                    else if (discoveryMode == DiscoveryMode.AUTO_PROVIDERS_PROPERTY) {
                        Hashtable<String, Object> properties = activator.getAutoProviderInstructions().map(
                            Parameters::stream
                        ).orElseGet(MapStream::empty).filterKey(
                            i -> Glob.toPattern(i).asPredicate().test(bundle.getSymbolicName())
                        ).values().findFirst().map(
                            Hashtable<String, Object>::new
                        ).orElseGet(() -> new Hashtable<String, Object>());
                        registrations = Collections.singletonList(properties);
                    }
                    else if (registerServiceLoaderServices) {
                        registrations = findServiceRegistrationProperties(
                                serviceLoaderCapabilities, registrationClassName, className);
                    }
                    else {
                        registrations = Collections.emptyList();
                    }

                    if (registrations.isEmpty()) {
                        serviceDetails.add(new ServiceDetails(
                                registrationClassName, className, null));
                    }
                    for (Hashtable<String, Object> properties : registrations) {
                        properties.put(SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY, spiBundle.getBundleId());
                        properties.put(SpiFlyConstants.PROVIDER_IMPLCLASS_PROPERTY, className);
                        properties.put(SpiFlyConstants.PROVIDER_DISCOVERY_MODE, discoveryMode.toString());
                        serviceDetails.add(new ServiceDetails(
                                registrationClassName, className, properties));
                    }
                } catch (Exception e) {
                    log(Level.FINE,
                            "Could not process SPI implementation " + className + " for " + registrationClassName, e);
                }
            }
        }

        return serviceDetails;
    }

    Map<String, List<String>> readServiceProviderFiles(List<URL> serviceFileURLs) {
        Map<String, Set<String>> providers = new LinkedHashMap<String, Set<String>>();

        for (URL serviceFileURL : serviceFileURLs) {
            log(Level.FINE, "Found SPI resource: " + serviceFileURL);

            String serviceFile = serviceFileURL.toExternalForm();
            int idx = serviceFile.lastIndexOf('/');
            String serviceType = serviceFile.substring(idx + 1);
            Set<String> serviceProviders = providers.computeIfAbsent(
                    serviceType, key -> new LinkedHashSet<String>());

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serviceFileURL.openStream(), StandardCharsets.UTF_8))) {
                String className;
                while ((className = reader.readLine()) != null) {
                    int comment = className.indexOf('#');
                    if (comment >= 0) {
                        className = className.substring(0, comment);
                    }

                    className = className.trim();
                    if (!className.isEmpty()) {
                        serviceProviders.add(className);
                    }
                }
            } catch (IOException e) {
                log(Level.FINE, "Could not read SPI metadata from " + serviceFileURL, e);
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        for (Entry<String, Set<String>> entry : providers.entrySet()) {
            result.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        return result;
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

                List<ServiceDetails> collectServiceDetails = collectServiceDetails(bundle,
                        serviceFileURLs, DiscoveryMode.AUTO_PROVIDERS_PROPERTY,
                        Collections.<BundleCapability>emptyList(), false);

                collectServiceDetails.stream().map(ServiceDetails::getProperties).filter(Objects::nonNull).forEach(
                    hashtable -> hashtable.forEach(customAttributes::put)
                );

                List<String> providedServices = collectServiceDetails.stream().map(ServiceDetails::getServiceType).collect(toList());

                return new AbstractMap.SimpleImmutableEntry<>(providedServices, serviceFileURLs);
            }
        ).orElseGet(() -> new AbstractMap.SimpleImmutableEntry<>(null, null));
    }

    private List<URL> getServiceFileUrls(Bundle bundle) {
        return getServiceFileUrls(bundle, null);
    }

    List<URL> getServiceFileUrls(Bundle bundle, List<String> serviceTypes) {
        if (serviceTypes != null) {
            BundleWiring wiring = WiringUtils.getWiring(bundle);
            if (wiring == null) {
                return Collections.emptyList();
            }

            Set<String> requestedTypes = new LinkedHashSet<String>(serviceTypes);
            Set<URL> serviceFileURLs = new LinkedHashSet<URL>();
            for (String serviceType : requestedTypes) {
                List<URL> entries = wiring.findEntries(
                        METAINF_SERVICES, serviceType, 0);
                if (entries != null) {
                    serviceFileURLs.addAll(entries);
                }
            }
            if (serviceFileURLs.isEmpty()) {
                Enumeration<URL> entries = bundle.findEntries(
                        METAINF_SERVICES, "*", false);
                if (entries != null) {
                    for (URL entry : Collections.list(entries)) {
                        String path = entry.getPath();
                        int separator = path.lastIndexOf('/');
                        String serviceType = separator < 0
                                ? path : path.substring(separator + 1);
                        if (requestedTypes.contains(serviceType)) {
                            serviceFileURLs.add(entry);
                        }
                    }
                }
            }

            addBundleClassPathServiceFiles(
                    bundle, requestedTypes, serviceFileURLs);
            List<BundleWire> hostWires = wiring.getProvidedWires(
                    HostNamespace.HOST_NAMESPACE);
            if (hostWires != null) {
                for (BundleWire hostWire : hostWires) {
                    BundleRevision fragmentRevision = hostWire.getRequirement() == null
                            ? null : hostWire.getRequirement().getRevision();
                    Bundle fragment = fragmentRevision == null
                            ? null : fragmentRevision.getBundle();
                    if (fragment != null) {
                        addBundleClassPathServiceFiles(
                                fragment, requestedTypes, serviceFileURLs);
                    }
                }
            }
            return new ArrayList<URL>(serviceFileURLs);
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

                URL url = bundle.getEntry(entry);
                if (url != null) {
                    serviceFileURLs.addAll(getMetaInfServiceURLsFromJar(url));
                }
            }
        }

        return serviceFileURLs;
    }

    private void addBundleClassPathServiceFiles(Bundle bundle,
            Set<String> serviceTypes, Set<URL> serviceFileURLs) {
        Object bcp = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if (!(bcp instanceof String)) {
            return;
        }

        for (String entry : ((String) bcp).split(",")) {
            entry = entry.trim();
            if (entry.equals(".")) {
                continue;
            }
            URL url = bundle.getEntry(entry);
            if (url != null) {
                serviceFileURLs.addAll(
                        getMetaInfServiceURLsFromJar(url, serviceTypes));
            }
        }
    }

    private String getHeaderFromBundleOrFragment(Bundle bundle, String headerName) {
        return getHeaderFromBundleOrFragment(bundle, headerName, null);
    }

    private String getHeaderFromBundleOrFragment(Bundle bundle, String headerName, String matchString) {
        final boolean mergeHeader = MERGE_HEADERS.contains(headerName);
        Parameters headerParameters = new Parameters(bundle.getHeaders().get(headerName));
        if (matches(headerParameters.toString(), matchString) && !mergeHeader) {
            return headerParameters.isEmpty() ? null : headerParameters.toString();
        }

        BundleRevision rev = bundle.adapt(BundleRevision.class);
        if (rev != null) {
            BundleWiring wiring = rev.getWiring();
            if (wiring != null) {
                for (BundleWire wire : wiring.getProvidedWires("osgi.wiring.host")) {
                    Bundle fragment = wire.getRequirement().getRevision().getBundle();
                    Parameters fragmentParameters = new Parameters(fragment.getHeaders().get(headerName));
                    if (mergeHeader) {
                        // Parameters.mergeWith merges the attributes of colliding map entries.
                        // Fragment headers are parsed independently, so append every clause and
                        // let Parameters.add assign fresh duplicate markers where needed.
                        for (Entry<String, Attrs> entry : fragmentParameters.entrySet()) {
                            headerParameters.add(entry.getKey(), new Attrs(entry.getValue()));
                        }
                    }
                    else {
                        headerParameters = fragmentParameters;
                        if (matches(headerParameters.toString(), matchString)) {
                            return headerParameters.toString();
                        }
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

    private List<Hashtable<String, Object>> findServiceRegistrationProperties(
            List<BundleCapability> capabilities, String spiName, String implName) {
        List<Hashtable<String, Object>> registrations =
                new ArrayList<Hashtable<String, Object>>();
        for (BundleCapability capability : capabilities) {
            Map<String, Object> attributes = capability.getAttributes();
            if (!spiName.equals(attributes.get(
                    SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))) {
                continue;
            }

            String register = capability.getDirectives().get(REGISTER_DIRECTIVE_NAME);
            if (register != null && !register.equals(implName)) {
                continue;
            }

            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                String name = attribute.getKey();
                if (SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE.equals(name)
                        || name.startsWith(".")) {
                    continue;
                }

                properties.put(name, attribute.getValue());
            }
            registrations.add(properties);
        }
        return registrations;
    }

    private List<URL> getMetaInfServiceURLsFromJar(URL url) {
        return getMetaInfServiceURLsFromJar(url, null);
    }

    private List<URL> getMetaInfServiceURLsFromJar(
            URL url, Set<String> serviceTypes) {
        List<URL> urls = new ArrayList<URL>();
        try {
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(url.openStream());

                JarEntry je = null;
                while((je = jis.getNextJarEntry()) != null) {
                    if (je.getName().startsWith(METAINF_SERVICES + "/")
                            && je.getName().length() > (METAINF_SERVICES.length() + 1)
                            && (serviceTypes == null || serviceTypes.contains(
                                    je.getName().substring(METAINF_SERVICES.length() + 1)))) {
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

    @SuppressWarnings("unchecked")
    void reprocessBundle(Bundle bundle, Object registrations) {
        List<ServiceRegistration> current = (List<ServiceRegistration>) registrations;
        synchronized (current) {
            activator.unregisterProviderBundle(bundle);
            unregister(current);
            current.clear();

            List<ServiceRegistration> replacements = addingBundle(bundle, null);
            if (replacements != null) {
                current.addAll(replacements);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removedBundle(Bundle bundle, BundleEvent event, Object registrations) {
        activator.unregisterProviderBundle(bundle);

        if (registrations == null)
            return;

        List<ServiceRegistration> current = (List<ServiceRegistration>) registrations;
        synchronized (current) {
            unregister(current);
        }
    }

    private void unregister(List<ServiceRegistration> registrations) {
        for (ServiceRegistration reg : registrations) {
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

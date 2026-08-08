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
package org.apache.aries.spifly.dynamic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.mytest.MySPI;
import org.apache.aries.spifly.dynamic.impl1.MySPIImpl1;
import org.apache.aries.spifly.dynamic.impl2.MySPIImpl2;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.util.CheckClassAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.BundleTracker;

public class LateMediatorStartupTest {
    private static final String SERVICE_TYPE = "org.apache.aries.mytest.MySPI";
    private static final String IMPLEMENTATION =
            "org.apache.aries.spifly.dynamic.impl1.MySPIImpl1";

    @Test
    public void startingMediatorRefreshesAlreadyLoadedConsumer() throws Exception {
        Path storage = Files.createTempDirectory("spifly-late-mediator-");
        Framework framework = null;
        try {
            Map<String, String> configuration = new HashMap<String, String>();
            configuration.put(Constants.FRAMEWORK_STORAGE, storage.resolve("framework").toString());
            configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
                    Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
            framework = newFramework(configuration);
            framework.start();

            BundleContext context = framework.getBundleContext();
            installDependency(context, ClassReader.class);
            installDependency(context, AdviceAdapter.class);
            installDependency(context, ClassNode.class);
            installDependency(context, Analyzer.class);
            installDependency(context, CheckClassAdapter.class);
            installDependency(context, BundleTracker.class);

            Bundle mediator = context.installBundle(
                    bundleFromClasses(storage, "spifly-dynamic.jar").toUri().toString());
            Bundle api = context.installBundle(createApiBundle(storage).toUri().toString());
            Bundle provider = context.installBundle(createProviderBundle(storage).toUri().toString());
            Bundle consumer = context.installBundle(createConsumerBundle(storage).toUri().toString());

            FrameworkWiring frameworkWiring = framework.adapt(FrameworkWiring.class);
            assertTrue(frameworkWiring.resolveBundles(
                    java.util.Arrays.asList(mediator, api, provider, consumer)));
            assertEquals(Bundle.RESOLVED, mediator.getState());

            provider.start();
            consumer.start();
            Class<?> classBeforeMediator = consumer.loadClass(TestClient.class.getName());
            assertEquals(Collections.emptySet(), invokeConsumer(classBeforeMediator));

            CountDownLatch consumerRestarted = new CountDownLatch(1);
            BundleListener listener = event -> {
                if (event.getType() == BundleEvent.STARTED
                        && consumer.equals(event.getBundle())) {
                    consumerRestarted.countDown();
                }
            };
            context.addBundleListener(listener);
            try {
                mediator.start();
                assertTrue("The late mediator should refresh and restart the consumer",
                        consumerRestarted.await(30, TimeUnit.SECONDS));
            }
            finally {
                context.removeBundleListener(listener);
            }

            Class<?> classAfterMediator = consumer.loadClass(TestClient.class.getName());
            assertNotSame(classBeforeMediator, classAfterMediator);
            assertEquals(Collections.singleton("olleh"), invokeConsumer(classAfterMediator));
            assertEquals(new java.util.HashSet<String>(java.util.Arrays.asList(
                    "load:olleh", "loader:olleh", "installed:olleh")),
                    invokeMethodReferences(classAfterMediator));

            mediator.stop();
            assertEquals(Collections.emptySet(), invokeConsumer(classAfterMediator));
            assertEquals(Collections.emptySet(),
                    invokeMethodReferences(classAfterMediator));
        }
        finally {
            if (framework != null) {
                framework.stop();
                framework.waitForStop(30000);
            }
            deleteRecursively(storage);
        }
    }

    @Test
    public void stoppedProviderRefreshesConsumerThatLoadedItsProvider()
            throws Exception {
        Path storage = Files.createTempDirectory("spifly-provider-stop-");
        Framework framework = null;
        try {
            Map<String, String> configuration = new HashMap<String, String>();
            configuration.put(Constants.FRAMEWORK_STORAGE,
                    storage.resolve("framework").toString());
            configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
                    Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
            framework = newFramework(configuration);
            framework.start();

            BundleContext context = framework.getBundleContext();
            installDependency(context, ClassReader.class);
            installDependency(context, AdviceAdapter.class);
            installDependency(context, ClassNode.class);
            installDependency(context, Analyzer.class);
            installDependency(context, CheckClassAdapter.class);
            installDependency(context, BundleTracker.class);

            Bundle mediator = context.installBundle(
                    bundleFromClasses(storage, "spifly-dynamic.jar").toUri().toString());
            Bundle api = context.installBundle(createApiBundle(storage).toUri().toString());
            Bundle provider = context.installBundle(
                    createProviderBundle(storage).toUri().toString());
            Bundle consumer = context.installBundle(
                    createConsumerBundle(storage).toUri().toString());

            FrameworkWiring frameworkWiring = framework.adapt(FrameworkWiring.class);
            assertTrue(frameworkWiring.resolveBundles(
                    java.util.Arrays.asList(mediator, api, provider, consumer)));
            mediator.start();
            provider.start();
            consumer.start();

            Class<?> originalConsumerClass = consumer.loadClass(TestClient.class.getName());
            assertEquals(Collections.singleton("olleh"),
                    invokeConsumer(originalConsumerClass));

            CountDownLatch consumerStopped = new CountDownLatch(1);
            CountDownLatch consumerRestarted = new CountDownLatch(1);
            CountDownLatch refreshCompleted = new CountDownLatch(1);
            BundleListener listener = event -> {
                if (consumer.equals(event.getBundle())) {
                    if (event.getType() == BundleEvent.STOPPED) {
                        consumerStopped.countDown();
                    }
                    else if (event.getType() == BundleEvent.STARTED) {
                        consumerRestarted.countDown();
                    }
                }
            };
            FrameworkListener frameworkListener = event -> {
                if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                    refreshCompleted.countDown();
                }
            };
            context.addBundleListener(listener);
            context.addFrameworkListener(frameworkListener);
            try {
                provider.stop();
                assertTrue("The stale consumer should be stopped for refresh",
                        consumerStopped.await(30, TimeUnit.SECONDS));
                assertTrue("The stale consumer should restart after refresh",
                        consumerRestarted.await(30, TimeUnit.SECONDS));
                assertTrue("The consumer refresh should complete",
                        refreshCompleted.await(30, TimeUnit.SECONDS));
            }
            finally {
                context.removeBundleListener(listener);
                context.removeFrameworkListener(frameworkListener);
            }

            assertEquals(Bundle.RESOLVED, provider.getState());
            Class<?> refreshedConsumerClass = consumer.loadClass(
                    TestClient.class.getName());
            assertNotSame(originalConsumerClass, refreshedConsumerClass);
            assertEquals(Collections.emptySet(),
                    invokeConsumer(refreshedConsumerClass));
        }
        finally {
            if (framework != null) {
                framework.stop();
                framework.waitForStop(30000);
            }
            deleteRecursively(storage);
        }
    }

    @Test
    public void providerDiscoveryUsesAttachedFragmentRevisionUntilRefresh()
            throws Exception {
        Path storage = Files.createTempDirectory("spifly-fragment-revision-");
        Framework framework = null;
        try {
            Map<String, String> configuration = new HashMap<String, String>();
            configuration.put(Constants.FRAMEWORK_STORAGE,
                    storage.resolve("framework").toString());
            configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
                    Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
            framework = newFramework(configuration);
            framework.start();

            BundleContext context = framework.getBundleContext();
            installDependency(context, ClassReader.class);
            installDependency(context, AdviceAdapter.class);
            installDependency(context, ClassNode.class);
            installDependency(context, Analyzer.class);
            installDependency(context, CheckClassAdapter.class);
            installDependency(context, BundleTracker.class);

            Bundle mediator = context.installBundle(
                    bundleFromClasses(storage, "spifly-dynamic.jar").toUri().toString());
            Bundle api = context.installBundle(createApiBundle(storage).toUri().toString());
            Path fragmentF1 = createProviderFragment(
                    storage.resolve("fragment-f1.jar"), MySPIImpl1.class, "1.0.0");
            Path fragmentF2 = createProviderFragment(
                    storage.resolve("fragment-f2.jar"), MySPIImpl2.class, "2.0.0");
            Bundle fragment = context.installBundle(fragmentF1.toUri().toString());
            Bundle provider = context.installBundle(
                    createProviderHost(storage).toUri().toString());
            Bundle consumer = context.installBundle(
                    createConsumerBundle(storage).toUri().toString());

            FrameworkWiring frameworkWiring = framework.adapt(FrameworkWiring.class);
            assertTrue(frameworkWiring.resolveBundles(java.util.Arrays.asList(
                    mediator, api, fragment, provider, consumer)));
            mediator.start();
            provider.start();
            consumer.start();
            assertEquals(Collections.singleton("olleh"),
                    invokeConsumer(consumer.loadClass(TestClient.class.getName())));

            BundleWiring originalWiring = provider.adapt(BundleWiring.class);
            BundleRevision attachedF1 = originalWiring.getProvidedWires(
                    HostNamespace.HOST_NAMESPACE).get(0).getRequirement().getRevision();
            InputStream update = Files.newInputStream(fragmentF2);
            try {
                fragment.update(update);
            }
            finally {
                update.close();
            }
            assertNotSame("The host must remain attached to F1 before refresh",
                    fragment.adapt(BundleRevision.class), attachedF1);

            consumer.stop();
            provider.stop();
            provider.start();
            assertSame("A stop/start must not change the effective host wiring",
                    originalWiring, provider.adapt(BundleWiring.class));
            consumer.start();
            assertEquals(Collections.singleton("olleh"),
                    invokeConsumer(consumer.loadClass(TestClient.class.getName())));

            consumer.stop();
            CountDownLatch refreshed = new CountDownLatch(1);
            frameworkWiring.refreshBundles(
                    java.util.Arrays.asList(provider, fragment, consumer), event -> {
                        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                            refreshed.countDown();
                        }
                    });
            assertTrue("Provider, fragment, and consumer refresh did not complete",
                    refreshed.await(30, TimeUnit.SECONDS));
            BundleWiring refreshedWiring = provider.adapt(BundleWiring.class);
            assertNotSame(originalWiring, refreshedWiring);
            BundleRevision attachedF2 = refreshedWiring.getProvidedWires(
                    HostNamespace.HOST_NAMESPACE).get(0).getRequirement().getRevision();
            assertSame("The refreshed host must attach the current fragment revision",
                    fragment.adapt(BundleRevision.class), attachedF2);
            assertTrue(refreshedWiring.listResources("META-INF/services", SERVICE_TYPE,
                    BundleWiring.LISTRESOURCES_LOCAL).contains(
                            "META-INF/services/" + SERVICE_TYPE));
            assertEquals(MySPIImpl2.class.getName(),
                    provider.loadClass(MySPIImpl2.class.getName()).getName());
            consumer.start();
            assertEquals(Collections.singleton("HELLO"),
                    invokeConsumer(consumer.loadClass(TestClient.class.getName())));
        }
        finally {
            if (framework != null) {
                framework.stop();
                framework.waitForStop(30000);
            }
            deleteRecursively(storage);
        }
    }

    @Test
    public void providerDiscoveryUsesHostClassPathEntryFromFragment()
            throws Exception {
        Path storage = Files.createTempDirectory("spifly-fragment-host-entry-");
        Framework framework = null;
        try {
            Map<String, String> configuration = new HashMap<String, String>();
            configuration.put(Constants.FRAMEWORK_STORAGE,
                    storage.resolve("framework").toString());
            configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN,
                    Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
            framework = newFramework(configuration);
            framework.start();

            BundleContext context = framework.getBundleContext();
            installDependency(context, ClassReader.class);
            installDependency(context, AdviceAdapter.class);
            installDependency(context, ClassNode.class);
            installDependency(context, Analyzer.class);
            installDependency(context, CheckClassAdapter.class);
            installDependency(context, BundleTracker.class);

            Bundle mediator = context.installBundle(
                    bundleFromClasses(storage, "spifly-dynamic.jar").toUri().toString());
            Bundle api = context.installBundle(createApiBundle(storage).toUri().toString());
            Bundle fragment = context.installBundle(createProviderFragment(
                    storage.resolve("host-entry-fragment.jar"),
                    MySPIImpl1.class, "1.0.0", false).toUri().toString());
            Bundle provider = context.installBundle(
                    createProviderHost(storage, true).toUri().toString());
            Bundle consumer = context.installBundle(
                    createConsumerBundle(storage).toUri().toString());

            FrameworkWiring frameworkWiring = framework.adapt(FrameworkWiring.class);
            assertTrue(frameworkWiring.resolveBundles(java.util.Arrays.asList(
                    mediator, api, fragment, provider, consumer)));
            mediator.start();
            provider.start();
            consumer.start();

            assertEquals("The mediated ServiceLoader must see the fragment entry",
                    Collections.singleton("olleh"),
                    invokeConsumer(consumer.loadClass(TestClient.class.getName())));
            org.osgi.framework.ServiceReference<?>[] registrations =
                    context.getAllServiceReferences(SERVICE_TYPE, null);
            assertEquals("The registrar must publish the fragment provider",
                    1, registrations == null ? 0 : registrations.length);
        }
        finally {
            if (framework != null) {
                framework.stop();
                framework.waitForStop(30000);
            }
            deleteRecursively(storage);
        }
    }

    private Framework newFramework(Map<String, String> configuration) throws Exception {
        String factoryName = System.getProperty(
                "spifly.test.frameworkFactory",
                "org.apache.felix.framework.FrameworkFactory");
        FrameworkFactory factory = (FrameworkFactory) Class.forName(factoryName)
                .getDeclaredConstructor().newInstance();
        return factory.newFramework(configuration);
    }

    @SuppressWarnings("unchecked")
    private Set<String> invokeConsumer(Class<?> consumerClass) throws Exception {
        Method method = consumerClass.getMethod("test", String.class);
        return (Set<String>) method.invoke(
                consumerClass.getDeclaredConstructor().newInstance(), "hello");
    }

    @SuppressWarnings("unchecked")
    private Set<String> invokeMethodReferences(Class<?> consumerClass)
            throws Exception {
        Method method = consumerClass.getMethod("testMethodReferences",
                String.class, ClassLoader.class);
        return (Set<String>) method.invoke(
                consumerClass.getDeclaredConstructor().newInstance(), "hello",
                consumerClass.getClassLoader());
    }

    private void installDependency(BundleContext context, Class<?> type) throws Exception {
        BundleWiring systemWiring = context.getBundle(0).adapt(BundleWiring.class);
        if (systemWiring != null) {
            for (BundleCapability capability : systemWiring.getCapabilities(
                    PackageNamespace.PACKAGE_NAMESPACE)) {
                if (type.getPackage().getName().equals(capability.getAttributes().get(
                        PackageNamespace.PACKAGE_NAMESPACE))) {
                    return;
                }
            }
        }
        URI location = type.getProtectionDomain().getCodeSource().getLocation().toURI();
        context.installBundle(location.toString());
    }

    private Path createApiBundle(Path directory) throws IOException {
        Manifest manifest = bundleManifest("spifly.test.api");
        manifest.getMainAttributes().putValue(
                Constants.EXPORT_PACKAGE, "org.apache.aries.mytest;version=1.0.0");
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        addClass(entries, MySPI.class);
        return writeBundle(directory.resolve("api.jar"), manifest, entries);
    }

    private Path createProviderBundle(Path directory) throws IOException {
        Manifest manifest = bundleManifest("spifly.test.provider");
        manifest.getMainAttributes().putValue(
                Constants.IMPORT_PACKAGE, "org.apache.aries.mytest;version=\"[1,2)\"");
        manifest.getMainAttributes().putValue(Constants.REQUIRE_CAPABILITY,
                "osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.registrar)\"");
        manifest.getMainAttributes().putValue(Constants.PROVIDE_CAPABILITY,
                "osgi.serviceloader;osgi.serviceloader=\"" + SERVICE_TYPE + "\"");
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        addClass(entries, MySPIImpl1.class);
        entries.put("META-INF/services/" + SERVICE_TYPE,
                (IMPLEMENTATION + "\n").getBytes(StandardCharsets.UTF_8));
        return writeBundle(directory.resolve("provider.jar"), manifest, entries);
    }

    private Path createProviderHost(Path directory) throws IOException {
        return createProviderHost(directory, false);
    }

    private Path createProviderHost(Path directory,
            boolean fragmentSuppliesClassPathEntry) throws IOException {
        Manifest manifest = bundleManifest("spifly.test.fragment.provider");
        manifest.getMainAttributes().putValue(
                Constants.IMPORT_PACKAGE, "org.apache.aries.mytest;version=\"[1,2)\"");
        if (fragmentSuppliesClassPathEntry) {
            manifest.getMainAttributes().putValue(
                    Constants.BUNDLE_CLASSPATH, "embedded.jar");
        }
        return writeBundle(directory.resolve("provider-host.jar"), manifest,
                Collections.<String, byte[]>emptyMap());
    }

    private Path createProviderFragment(Path path, Class<?> implementation,
            String version) throws IOException {
        return createProviderFragment(path, implementation, version, true);
    }

    private Path createProviderFragment(Path path, Class<?> implementation,
            String version, boolean declaresClassPath) throws IOException {
        Manifest manifest = bundleManifest("spifly.test.fragment");
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(Constants.BUNDLE_VERSION, version);
        attributes.putValue(Constants.FRAGMENT_HOST,
                "spifly.test.fragment.provider;bundle-version=\"[1,2)\"");
        if (declaresClassPath) {
            attributes.putValue(Constants.BUNDLE_CLASSPATH, ".,embedded.jar");
        }
        attributes.putValue(Constants.REQUIRE_CAPABILITY,
                "osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.registrar)\"");
        attributes.putValue(Constants.PROVIDE_CAPABILITY,
                "osgi.serviceloader;osgi.serviceloader=\"" + SERVICE_TYPE + "\"");
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("embedded.jar", createEmbeddedProviderJar(implementation));
        return writeBundle(path, manifest, entries);
    }

    private byte[] createEmbeddedProviderJar(Class<?> implementation)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JarOutputStream output = new JarOutputStream(bytes);
        try {
            String classResource = implementation.getName().replace('.', '/') + ".class";
            output.putNextEntry(new JarEntry(classResource));
            InputStream classBytes = implementation.getClassLoader()
                    .getResourceAsStream(classResource);
            if (classBytes == null) {
                throw new IOException("Cannot find test class " + classResource);
            }
            try {
                byte[] buffer = new byte[8192];
                for (int read; (read = classBytes.read(buffer)) >= 0;) {
                    output.write(buffer, 0, read);
                }
            }
            finally {
                classBytes.close();
            }
            output.closeEntry();
            output.putNextEntry(new JarEntry("META-INF/services/" + SERVICE_TYPE));
            output.write((implementation.getName() + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        finally {
            output.close();
        }
        return bytes.toByteArray();
    }

    private Path createConsumerBundle(Path directory) throws IOException {
        Manifest manifest = bundleManifest("spifly.test.consumer");
        manifest.getMainAttributes().putValue(
                Constants.IMPORT_PACKAGE, "org.apache.aries.mytest;version=\"[1,2)\"");
        manifest.getMainAttributes().putValue(Constants.REQUIRE_CAPABILITY,
                "osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.processor)\"," +
                "osgi.serviceloader;filter:=\"(osgi.serviceloader=" + SERVICE_TYPE + ")\"");
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        addClass(entries, TestClient.class);
        return writeBundle(directory.resolve("consumer.jar"), manifest, entries);
    }

    private Manifest bundleManifest(String symbolicName) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");
        return manifest;
    }

    private void addClass(Map<String, byte[]> entries, Class<?> type) throws IOException {
        String resource = type.getName().replace('.', '/') + ".class";
        InputStream stream = type.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IOException("Cannot find test class " + resource);
        }
        try {
            entries.put(resource, readAllBytes(stream));
        }
        finally {
            stream.close();
        }
    }

    private byte[] readAllBytes(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int read; (read = stream.read(buffer)) >= 0;) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private Path writeBundle(Path path, Manifest manifest, Map<String, byte[]> entries)
            throws IOException {
        JarOutputStream output = new JarOutputStream(Files.newOutputStream(
                path, StandardOpenOption.CREATE_NEW), manifest);
        try {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new JarEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        finally {
            output.close();
        }
        return path;
    }

    private Path bundleFromClasses(Path directory, String fileName) throws IOException {
        Path testClasses;
        try {
            testClasses = Paths.get(LateMediatorStartupTest.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        }
        catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
        Path classes = testClasses.resolveSibling("classes");
        Manifest manifest;
        InputStream manifestStream = Files.newInputStream(
                classes.resolve("META-INF").resolve("MANIFEST.MF"));
        try {
            manifest = new Manifest(manifestStream);
        }
        finally {
            manifestStream.close();
        }

        Path bundle = directory.resolve(fileName);
        JarOutputStream output = new JarOutputStream(Files.newOutputStream(
                bundle, StandardOpenOption.CREATE_NEW), manifest);
        try {
            List<Path> files = new ArrayList<Path>();
            Stream<Path> stream = Files.walk(classes);
            try {
                stream.filter(Files::isRegularFile).forEach(files::add);
            }
            finally {
                stream.close();
            }
            Collections.sort(files, Comparator.comparing(path -> classes.relativize(path).toString()));
            for (Path file : files) {
                String name = classes.relativize(file).toString().replace('\\', '/');
                if ("META-INF/MANIFEST.MF".equals(name)) {
                    continue;
                }
                output.putNextEntry(new JarEntry(name));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
        finally {
            output.close();
        }
        return bundle;
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        List<Path> paths = new ArrayList<Path>();
        Stream<Path> stream = Files.walk(directory);
        try {
            stream.forEach(paths::add);
        }
        finally {
            stream.close();
        }
        Collections.sort(paths, Comparator.reverseOrder());
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}

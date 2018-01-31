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
package org.apache.aries.blueprint.container;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.aries.blueprint.BlueprintConstants;
import org.apache.aries.blueprint.ExtendedReferenceMetadata;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.RefMetadataImpl;
import org.apache.aries.blueprint.reflect.ReferenceMetadataImpl;
import org.apache.aries.proxy.ProxyManager;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

public class LifecyclePolicyTest {

    @Test
    public void testStatic() throws Exception {
        final ReferenceMetadataImpl ref = new ReferenceMetadataImpl();
        ref.setId("ref");
        ref.setRuntimeInterface(TestItf.class);
        ref.setLifecycle(ExtendedReferenceMetadata.LIFECYCLE_STATIC);

        final BeanMetadataImpl bean1 = new BeanMetadataImpl();
        bean1.setId("bean1");
        bean1.setRuntimeClass(Bean1.class);
        bean1.setInitMethod("init");
        bean1.setDestroyMethod("destroy");
        bean1.addProperty("itf", new RefMetadataImpl("ref"));

        final BeanMetadataImpl bean2 = new BeanMetadataImpl();
        bean2.setId("bean2");
        bean2.setRuntimeClass(Bean2.class);
        bean2.setInitMethod("init");
        bean2.setDestroyMethod("destroy");
        bean2.addProperty("bean1", new RefMetadataImpl("bean1"));

        Bundle bundle = EasyMock.createMock(Bundle.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle extenderBundle = EasyMock.createMock(Bundle.class);
        BundleContext extenderBundleContext = EasyMock.createMock(BundleContext.class);
        BlueprintListener eventDispatcher = EasyMock.createMock(BlueprintListener.class);
        NamespaceHandlerRegistry namespaceHandlerRegistry = EasyMock.createMock(NamespaceHandlerRegistry.class);
        ProxyManager proxyManager = EasyMock.createMock(ProxyManager.class);
        NamespaceHandlerSet namespaceHandlerSet = EasyMock.createMock(NamespaceHandlerSet.class);
        TestItf itf = EasyMock.createMock(TestItf.class);
        ServiceRegistration registration = EasyMock.createMock(ServiceRegistration.class);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        List<URL> pathList = new ArrayList<URL>();
        Set<URI> namespaces = new HashSet<URI>();

        BlueprintContainerImpl container = new BlueprintContainerImpl(
                bundle, bundleContext, extenderBundle, eventDispatcher, namespaceHandlerRegistry,
                executorService, timer, pathList, proxyManager, namespaces
        ) {
            private boolean repoCreated = false;
            @Override
            public BlueprintRepository getRepository() {
                if (!repoCreated) {
                    getComponentDefinitionRegistry().registerComponentDefinition(ref);
                    getComponentDefinitionRegistry().registerComponentDefinition(bean1);
                    getComponentDefinitionRegistry().registerComponentDefinition(bean2);
                    repoCreated = true;
                }
                return super.getRepository();
            }
        };

        ServiceReference svcRef1 = EasyMock.createMock(ServiceReference.class);

        EasyMock.expect(bundle.getSymbolicName()).andReturn("bundleSymbolicName").anyTimes();
        EasyMock.expect(bundle.getVersion()).andReturn(Version.emptyVersion).anyTimes();
        EasyMock.expect(bundle.getState()).andReturn(Bundle.ACTIVE).anyTimes();
        EasyMock.expect(bundle.getBundleContext()).andReturn(bundleContext).anyTimes();
        Hashtable<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "bundleSymbolicName;blueprint.aries.xml-validation:=false");
        EasyMock.expect(bundle.getHeaders()).andReturn(headers).anyTimes();
        eventDispatcher.blueprintEvent(EasyMock.<BlueprintEvent>anyObject());
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(namespaceHandlerRegistry.getNamespaceHandlers(namespaces, bundle))
                .andReturn(namespaceHandlerSet).anyTimes();
        EasyMock.expect(namespaceHandlerSet.getNamespaces()).andReturn(namespaces).anyTimes();
        namespaceHandlerSet.addListener(container);
        EasyMock.expectLastCall();
        EasyMock.expect(bundleContext.getProperty(BlueprintConstants.XML_VALIDATION_PROPERTY))
                .andReturn(null);
        Properties props = new Properties();
        props.put("osgi.blueprint.container.version", Version.emptyVersion);
        props.put("osgi.blueprint.container.symbolicname", "bundleSymbolicName");
        EasyMock.expect(bundleContext.registerService(
                EasyMock.aryEq(new String[] {BlueprintContainer.class.getName()}),
                EasyMock.same(container),
                EasyMock.eq((Dictionary)props))).andReturn(registration);
        bundleContext.addServiceListener(EasyMock.<org.osgi.framework.ServiceListener>anyObject(), EasyMock.<String>anyObject());
        EasyMock.expectLastCall();
        EasyMock.expect(bundleContext.getServiceReferences((String) null, "(objectClass=" + TestItf.class.getName() + ")"))
                .andReturn(new ServiceReference[] { svcRef1 });

        EasyMock.expect(bundleContext.getService(svcRef1)).andReturn(itf);
        EasyMock.expect(bundle.loadClass("java.lang.Object")).andReturn((Class) Object.class).anyTimes();

        EasyMock.replay(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);

        container.run();
        ReferenceRecipe recipe = (ReferenceRecipe) container.getRepository().getRecipe("ref");
        recipe.start(container);

        Bean2 bean2i = (Bean2) container.getRepository().create("bean2");
        Assert.assertNotNull(bean2i);
        Assert.assertEquals(1, Bean2.initialized);
        Assert.assertEquals(0, Bean2.destroyed);

        EasyMock.verify(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);

        //
        // Unregister the service
        //
        // Given the lifecycle is 'static', this should cause the Bean1 and Bean2
        // to be destroyed
        //

        EasyMock.reset(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);

        EasyMock.expect(bundle.getSymbolicName()).andReturn("bundleSymbolicName").anyTimes();
        EasyMock.expect(bundle.getVersion()).andReturn(Version.emptyVersion).anyTimes();
        EasyMock.expect(bundleContext.ungetService(svcRef1)).andReturn(false);

        EasyMock.replay(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);

        recipe.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, svcRef1));
        Assert.assertEquals(1, Bean2.initialized);
        Assert.assertEquals(1, Bean2.destroyed);

        EasyMock.verify(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);

        //
        // Re-register the service
        //
        // Given the lifecycle is 'static', this should cause the Bean1 and Bean2
        // to be recreated
        //

        EasyMock.reset(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);

        EasyMock.expect(bundle.getSymbolicName()).andReturn("bundleSymbolicName").anyTimes();
        EasyMock.expect(bundle.getVersion()).andReturn(Version.emptyVersion).anyTimes();
        EasyMock.expect(bundleContext.getService(svcRef1)).andReturn(itf);
        EasyMock.expect(bundle.loadClass("java.lang.Object")).andReturn((Class) Object.class).anyTimes();

        EasyMock.replay(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);

        recipe.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, svcRef1));
        Assert.assertEquals(2, Bean2.initialized);
        Assert.assertEquals(1, Bean2.destroyed);

        EasyMock.verify(bundle, bundleContext, extenderBundle, extenderBundleContext,
                eventDispatcher, namespaceHandlerRegistry, namespaceHandlerSet, proxyManager,
                svcRef1, registration);
    }

    public interface TestItf {

    }

    public static class Bean1 {
        private TestItf itf;
        public TestItf getItf() {
            return itf;
        }
        public void setItf(TestItf itf) {
            this.itf = itf;
        }
        public void init() {
        }
        public void destroy() {
        }
    }

    public static class Bean2 {
        private Bean1 bean1;
        static int initialized = 0;
        static int destroyed = 0;
        public Bean1 getBean1() {
            return bean1;
        }
        public void setBean1(Bean1 bean1) {
            this.bean1 = bean1;
        }
        public void init() {
            initialized++;
        }
        public void destroy() {
            destroyed++;
        }
    }
}

/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.spring.extender;

import java.util.Hashtable;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator,
        ServiceTrackerCustomizer<BlueprintExtenderService, SpringOsgiExtender> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    BundleContext bundleContext;
    ServiceTracker<BlueprintExtenderService, SpringOsgiExtender> tracker;
    ServiceRegistration<NamespaceHandler> osgiNamespaceRegistration;
    ServiceRegistration<NamespaceHandler> compendiumNamespaceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        tracker = new ServiceTracker<BlueprintExtenderService, SpringOsgiExtender>(
                bundleContext, BlueprintExtenderService.class, this
        );
        tracker.open();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("osgi.service.blueprint.namespace", "http://www.springframework.org/schema/osgi");
        osgiNamespaceRegistration = bundleContext.registerService(
                NamespaceHandler.class, new SpringOsgiNamespaceHandler(), props);
        props = new Hashtable<String, String>();
        props.put("osgi.service.blueprint.namespace", "http://www.springframework.org/schema/osgi-compendium");
        compendiumNamespaceRegistration = bundleContext.registerService(
                NamespaceHandler.class, new SpringOsgiCompendiumNamespaceHandler(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
        osgiNamespaceRegistration.unregister();
        compendiumNamespaceRegistration.unregister();
    }

    @Override
    public SpringOsgiExtender addingService(ServiceReference<BlueprintExtenderService> reference) {
        BlueprintExtenderService blueprintExtenderService = bundleContext.getService(reference);
        SpringOsgiExtender extender = new SpringOsgiExtender(blueprintExtenderService);
        try {
            extender.start(bundleContext);
        } catch (Exception e) {
            LOGGER.error("Error starting SpringOsgiExtender", e);
        }
        return extender;
    }

    @Override
    public void modifiedService(ServiceReference<BlueprintExtenderService> reference, SpringOsgiExtender service) {
    }

    @Override
    public void removedService(ServiceReference<BlueprintExtenderService> reference, SpringOsgiExtender service) {
        try {
            service.stop(bundleContext);
        } catch (Exception e) {
            LOGGER.error("Error stopping SpringOsgiExtender", e);
        }
        bundleContext.ungetService(reference);
    }
}

/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.blueprint.web;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.aries.blueprint.services.BlueprintExtenderService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * OSGI Blueprint-aware ServletContextListener which use Aties BlueprintExtenderService
 * to create Blueprint Container for the application bundle and save it as ServletContext attribute
 *  
 */
public class BlueprintContextListener implements ServletContextListener {
    public static final String CONTAINER_ATTRIBUTE = "org.apache.aries.blueprint.container";

    public static final String LOCATION_PARAM = "blueprintLocation";
    public static final String DEFAULT_LOCATION = "OSGI-INF/blueprint.xml";

    public static final String BUNDLE_CONTEXT_PARAM = "blueprintContext";
    public static final String DEFAULT_BUNDLE_CONTEXT_ATTRIBUTE = "osgi-bundlecontext";

    public void contextInitialized(ServletContextEvent event) {
        ServletContext sc = event.getServletContext();

        // Get bundle context
        BundleContext bc = getBundleContext(sc);
        if (bc == null) {
            return;
        }

        // Get BlueprintExtenderService
        BlueprintExtenderService blueprintExtender = getBlueprintExtenderService(bc);
        if (blueprintExtender == null) {
            return;
        }

        try {
            // Check if the extender has already created a container
            BlueprintContainer container = blueprintExtender.getContainer(bc.getBundle());

            if (container == null) {
                List<Object> blueprintResources = getBlueprintAppList(sc, bc.getBundle());
                if (blueprintResources.isEmpty()) {
                    // The extender is expected to scan a bundle
                    container = blueprintExtender.createContainer(bc.getBundle());
                } else {
                    // Use specified resources to create a container
                    container = blueprintExtender.createContainer(bc.getBundle(), blueprintResources);
                }
            }
            if (container == null) {
                sc.log("Failed to startup blueprint container.");
            } else {
                sc.setAttribute(CONTAINER_ATTRIBUTE, container);
            }
        } catch (Exception e) {
            sc.log("Failed to startup blueprint container. " + e, e);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        ServletContext sc = event.getServletContext();
        BlueprintContainer container = (BlueprintContainer)sc.getAttribute(CONTAINER_ATTRIBUTE);
        if (container == null) {
            return;
        }

        BundleContext bc = getBundleContext(sc);
        if (bc == null) {
            return;
        }

        BlueprintExtenderService blueprintExtender = getBlueprintExtenderService(bc);
        if (blueprintExtender == null) {
            return;
        }

        blueprintExtender.destroyContainer(bc.getBundle(), container);
    }

    private List<Object> getBlueprintAppList(ServletContext sc, Bundle applicationBundle) {
        String location = sc.getInitParameter(LOCATION_PARAM);
        if (location == null) {
            location = DEFAULT_LOCATION;
        }

        List<Object> blueprintResources = new LinkedList<Object>();
        URL entry = applicationBundle.getEntry(location);
        if (entry != null) {
            blueprintResources.add(entry);
        }

        return blueprintResources;
    }

    private BundleContext getBundleContext(ServletContext sc) {
        String bundleContextAttributeName = sc
                .getInitParameter(BUNDLE_CONTEXT_PARAM);
        if (bundleContextAttributeName == null) {
            bundleContextAttributeName = DEFAULT_BUNDLE_CONTEXT_ATTRIBUTE;
        }

        BundleContext bc = (BundleContext) sc.getAttribute(bundleContextAttributeName);
        if (bc == null) {
            sc.log("Failed to startup blueprint container: no BundleContext is available");
        }
        return bc;
    }

    private BlueprintExtenderService getBlueprintExtenderService(BundleContext bc) {
        ServiceReference sref = bc
                .getServiceReference(BlueprintExtenderService.class.getName());
        if (sref != null) {
            return (BlueprintExtenderService) bc.getService(sref);
        } else {
            return null;
        }
    }
}

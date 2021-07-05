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
package org.apache.aries.blueprint.spring;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;

/**
 * Spring extension.
 * Each spring namespace is wrapped in a blueprint namespace.
 *
 * @see BlueprintNamespaceHandler
 */
public class SpringExtension implements Extension {

    public static final String SPRING_HANDLERS = "META-INF/spring.handlers";
    public static final String SPRING_SCHEMAS = "META-INF/spring.schemas";
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringExtension.class);

    private final Bundle bundle;
    private final List<ServiceRegistration<NamespaceHandler>> registrations;
    private final Configuration blacklistedNamespaces;

    public SpringExtension(Bundle bundle, Configuration blacklistedNamespaces) {
        this.bundle = bundle;
        this.blacklistedNamespaces = blacklistedNamespaces;
        this.registrations = new ArrayList<ServiceRegistration<NamespaceHandler>>();
    }

    @Override
    public void start() throws Exception {
        Dictionary<String, Object> nsHandlerBlacklist = blacklistedNamespaces.getProperties();
        Map<String, NamespaceHandler> handlers = new HashMap<String, NamespaceHandler>();
        Properties props = loadSpringHandlers();
        Properties schemas = loadSpringSchemas();
        for (String key : props.stringPropertyNames()) {
            if (nsHandlerBlacklist != null && nsHandlerBlacklist.get(key) != null && props.get(key).equals(nsHandlerBlacklist.get(key))) {
                LOGGER.info("Ignoring namespace handler for namespace: {}={}, bundle: {}:{}", key, props.get(key), bundle.getSymbolicName(), bundle.getBundleId());
                continue;
            }

            String clazzName = props.getProperty(key);
            org.springframework.beans.factory.xml.NamespaceHandler springHandler
                    = (org.springframework.beans.factory.xml.NamespaceHandler) bundle.loadClass(clazzName).newInstance();
            NamespaceHandler wrapper = new BlueprintNamespaceHandler(bundle, schemas, springHandler);
            handlers.put(key, wrapper);
        }
        if (bundle == FrameworkUtil.getBundle(BeanFactory.class)) {
            org.springframework.beans.factory.xml.NamespaceHandler springHandler
                    = new BeansNamespaceHandler();
            NamespaceHandler wrapper = new BlueprintNamespaceHandler(bundle, schemas, springHandler);
            handlers.put(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI, wrapper);
        }
        for (Map.Entry<String, NamespaceHandler> entry : handlers.entrySet()) {
            Hashtable<String, String> svcProps = new Hashtable<String, String>();
            svcProps.put("osgi.service.blueprint.namespace", entry.getKey());
            ServiceRegistration<NamespaceHandler> reg =
                    bundle.getBundleContext().registerService(NamespaceHandler.class, entry.getValue(),
                            svcProps);
            registrations.add(reg);
        }
    }

    private Properties loadSpringHandlers() throws IOException {
        Properties props = new Properties();
        URL url = bundle.getResource(SPRING_HANDLERS);
        InputStream is = url.openStream();
        try {
            props.load(is);
        } finally {
            is.close();
        }
        return props;
    }

    private Properties loadSpringSchemas() throws IOException {
        Properties props = new Properties();
        URL url = bundle.getResource(SPRING_SCHEMAS);
        InputStream is = url.openStream();
        try {
            props.load(is);
        } finally {
            is.close();
        }
        return props;
    }

    @Override
    public void destroy() throws Exception {
        for (ServiceRegistration<NamespaceHandler> reg : registrations) {
            reg.unregister();
        }
    }

}

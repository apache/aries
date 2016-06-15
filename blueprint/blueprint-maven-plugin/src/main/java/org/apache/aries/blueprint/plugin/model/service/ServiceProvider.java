/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.model.service;

import com.google.common.collect.Lists;
import org.apache.aries.blueprint.plugin.model.Bean;
import org.apache.aries.blueprint.plugin.model.BeanRef;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceProvider {
    public final List<String> interfaces;
    public final String beanRef;
    public final Map<String, String> serviceProperties;

    public ServiceProvider(List<String> interfaces, String beanRef, Map<String, String> serviceProperties) {
        this.interfaces = interfaces;
        this.beanRef = beanRef;
        this.serviceProperties = serviceProperties;
    }

    public static ServiceProvider fromBean(Bean bean) {
        return createServiceProvider(bean.clazz, bean.id);
    }

    public static ServiceProvider fromMethod(BeanRef beanRef, Method method) {
        return createServiceProvider(method, beanRef.id);
    }

    private static ServiceProvider createServiceProvider(AnnotatedElement annotatedElement, String ref) {
        OsgiServiceProvider serviceProvider = annotatedElement.getAnnotation(OsgiServiceProvider.class);
        Properties properties = annotatedElement.getAnnotation(Properties.class);

        if (serviceProvider == null) {
            return null;
        }

        List<String> interfaceNames = extractServiceInterfaces(serviceProvider);

        Map<String, String> propertiesAsMap = extractProperties(properties);

        return new ServiceProvider(interfaceNames, ref, propertiesAsMap);
    }

    private static Map<String, String> extractProperties(Properties properties) {
        Map<String, String> propertiesAsMap = new HashMap<>();
        if (properties != null) {
            for (Property property : properties.value()) {
                propertiesAsMap.put(property.name(), property.value());
            }
        }
        return propertiesAsMap;
    }

    private static List<String> extractServiceInterfaces(OsgiServiceProvider serviceProvider) {
        List<String> interfaceNames = Lists.newArrayList();
        for (Class<?> serviceIf : serviceProvider.classes()) {
            interfaceNames.add(serviceIf.getName());
        }
        return interfaceNames;
    }
}

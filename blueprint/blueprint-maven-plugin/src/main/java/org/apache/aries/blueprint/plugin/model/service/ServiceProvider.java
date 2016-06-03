package org.apache.aries.blueprint.plugin.model.service;

import com.google.common.collect.Lists;
import org.apache.aries.blueprint.plugin.model.Bean;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

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
        OsgiServiceProvider serviceProvider = bean.clazz.getAnnotation(OsgiServiceProvider.class);
        if (serviceProvider == null) {
            return null;
        }

        List<String> interfaceNames = Lists.newArrayList();
        for (Class<?> serviceIf : serviceProvider.classes()) {
            interfaceNames.add(serviceIf.getName());
        }

        Properties properties = bean.clazz.getAnnotation(Properties.class);

        Map<String, String> propertiesAsMap = new HashMap<>();
        if (properties != null) {
            for (Property property : properties.value()) {
                propertiesAsMap.put(property.name(), property.value());
            }
        }

        return new ServiceProvider(interfaceNames, bean.id, propertiesAsMap);
    }
}

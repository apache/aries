/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.blueprint.codec;

import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;

public class BPBeanMetadata extends BPComponentMetadata implements BPTarget {

    private String className;

    private String destroyMethod;

    private String factoryMethod;

    private String initMethod;

    private String scope;

    private BPBeanArgument[] arguments;

    private BPBeanProperty[] properties;

    private BPTarget factoryComponent;

    public BPBeanMetadata(CompositeData bean) {
        super(bean);
        className = (String) bean.get(BlueprintMetadataMBean.CLASS_NAME);
        destroyMethod = (String) bean.get(BlueprintMetadataMBean.DESTROY_METHOD);
        factoryMethod = (String) bean.get(BlueprintMetadataMBean.FACTORY_METHOD);
        initMethod = (String) bean.get(BlueprintMetadataMBean.INIT_METHOD);
        scope = (String) bean.get(BlueprintMetadataMBean.SCOPE);

        Byte[] buf = (Byte[]) bean.get(BlueprintMetadataMBean.FACTORY_COMPONENT);
        factoryComponent = (BPTarget) Util.boxedBinary2BPMetadata(buf);

        CompositeData[] cd_args = (CompositeData[]) bean.get(BlueprintMetadataMBean.ARGUMENTS);
        arguments = new BPBeanArgument[cd_args.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = new BPBeanArgument(cd_args[i]);
        }

        CompositeData[] cd_props = (CompositeData[]) bean.get(BlueprintMetadataMBean.PROPERTIES);
        properties = new BPBeanProperty[cd_props.length];
        for (int i = 0; i < properties.length; i++) {
            properties[i] = new BPBeanProperty(cd_props[i]);
        }

    }

    public BPBeanMetadata(BeanMetadata bean) {
        super(bean);
        className = bean.getClassName();
        destroyMethod = bean.getDestroyMethod();
        factoryMethod = bean.getFactoryMethod();
        initMethod = bean.getInitMethod();
        scope = bean.getScope();

        factoryComponent = (BPTarget) Util.metadata2BPMetadata(bean.getFactoryComponent());

        arguments = new BPBeanArgument[bean.getArguments().size()];
        int i = 0;
        for (Object arg : bean.getArguments()) {
            arguments[i++] = new BPBeanArgument((BeanArgument) arg);
        }

        properties = new BPBeanProperty[bean.getProperties().size()];
        i = 0;
        for (Object prop : bean.getProperties()) {
            properties[i++] = new BPBeanProperty((BeanProperty) prop);
        }
    }

    protected Map<String, Object> getItemsMap() {
        Map<String, Object> items = super.getItemsMap();

        // add its fields to the map
        items.put(BlueprintMetadataMBean.CLASS_NAME, className);
        items.put(BlueprintMetadataMBean.DESTROY_METHOD, destroyMethod);
        items.put(BlueprintMetadataMBean.FACTORY_METHOD, factoryMethod);
        items.put(BlueprintMetadataMBean.INIT_METHOD, initMethod);
        items.put(BlueprintMetadataMBean.SCOPE, scope);

        items.put(BlueprintMetadataMBean.FACTORY_COMPONENT, Util.bpMetadata2BoxedBinary(factoryComponent));

        CompositeData[] cd_args = new CompositeData[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            cd_args[i] = arguments[i].asCompositeData();
        }
        items.put(BlueprintMetadataMBean.ARGUMENTS, cd_args);

        CompositeData[] cd_props = new CompositeData[properties.length];
        for (int i = 0; i < properties.length; i++) {
            cd_props[i] = properties[i].asCompositeData();
        }
        items.put(BlueprintMetadataMBean.PROPERTIES, cd_props);

        return items;
    }

    public CompositeData asCompositeData() {
        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.BEAN_METADATA_TYPE, getItemsMap());
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public BPBeanArgument[] getArguments() {
        return arguments;
    }

    public String getClassName() {
        return className;
    }

    public String getDestroyMethod() {
        return destroyMethod;
    }

    public BPTarget getFactoryComponent() {
        return factoryComponent;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public BPBeanProperty[] getProperties() {
        return properties;
    }

    public String getScope() {
        return scope;
    }
}

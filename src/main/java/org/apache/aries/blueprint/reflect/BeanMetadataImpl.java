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
package org.apache.aries.blueprint.reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.aries.blueprint.ExtendedBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.Target;

/**
 * Implementation of BeanMetadata
 *
 * @version $Rev$, $Date$
 */
public class BeanMetadataImpl extends ComponentMetadataImpl implements MutableBeanMetadata {

    private String className;
    private String initMethod;
    private String destroyMethod;
    private List<BeanArgument> arguments;
    private List<BeanProperty> properties;
    private int initialization;
    private String factoryMethod;
    private Target factoryComponent;
    private QName scope;
    private Class runtimeClass;
    private boolean processor;
    private boolean fieldInjection;
    private boolean rawConversion;
    
    public BeanMetadataImpl() {
        this.fieldInjection = false;
    }

    public BeanMetadataImpl(BeanMetadata source) {
        super(source);
        this.className = source.getClassName();
        this.initMethod = source.getInitMethod();
        this.destroyMethod = source.getDestroyMethod();
        for (BeanArgument argument : source.getArguments()) {
            addArgument(new BeanArgumentImpl(argument));
        }
        for (BeanProperty property : source.getProperties()) {
            addProperty(new BeanPropertyImpl(property));
        }
        this.initialization = source.getActivation();
        this.factoryMethod = source.getFactoryMethod();
        this.factoryComponent = MetadataUtil.cloneTarget(source.getFactoryComponent());
        this.scope = source.getScope() != null ? QName.valueOf(source.getScope()) : null;
        this.dependsOn = new ArrayList<String>(source.getDependsOn());
        if (source instanceof ExtendedBeanMetadata) {
            this.runtimeClass = ((ExtendedBeanMetadata) source).getRuntimeClass();
            this.fieldInjection = ((ExtendedBeanMetadata) source).getFieldInjection();
            this.rawConversion = ((ExtendedBeanMetadata) source).getRawConversion();
        } else {
            this.fieldInjection = false;
            this.rawConversion = false;
        }
    }
    
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(String initMethodName) {
        this.initMethod = initMethodName;
    }

    public String getDestroyMethod() {
        return destroyMethod;
    }

    public void setDestroyMethod(String destroyMethodName) {
        this.destroyMethod = destroyMethodName;
    }

    public List<BeanArgument> getArguments() {
        if (this.arguments == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(this.arguments);
        }
    }

    public void setArguments(List<BeanArgument> arguments) {
        this.arguments = arguments != null ? new ArrayList<BeanArgument>(arguments) : null;
    }

    public void addArgument(BeanArgument argument) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<BeanArgument>();
        }
        this.arguments.add(argument);
    }

    public BeanArgument addArgument(Metadata value, String valueType, int index) {
        BeanArgument arg = new BeanArgumentImpl(value, valueType, index);
        addArgument(arg);
        return arg;
    }

    public void removeArgument(BeanArgument argument) {
        if (this.arguments != null) {
            this.arguments.remove(argument);
        }
    }

    public List<BeanProperty> getProperties() {
        if (this.properties == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(this.properties);
        }
    }

    public void setProperties(List<BeanProperty> properties) {
        this.properties = properties != null ? new ArrayList<BeanProperty>(properties) : null;
    }

    public void addProperty(BeanProperty property) {
        if (this.properties == null) {
            this.properties = new ArrayList<BeanProperty>();
        }
        this.properties.add(property);
    }

    public BeanProperty addProperty(String name, Metadata value) {
        BeanProperty prop = new BeanPropertyImpl(name, value);
        addProperty(prop);
        return prop;
    }

    public void removeProperty(BeanProperty property) {
        if (this.properties != null) {
            this.properties.remove(property);
        }
    }

    public String getFactoryMethod() {
        return this.factoryMethod;
    }

    public void setFactoryMethod(String factoryMethodName) {
        this.factoryMethod = factoryMethodName;
    }

    public Target getFactoryComponent() {
        return this.factoryComponent;
    }

    public void setFactoryComponent(Target factoryComponent) {
        this.factoryComponent = factoryComponent;
    }

    public String getScope() {
        return this.scope != null ? this.scope.toString() : null;
    }

    public void setScope(String scope) {
        this.scope = scope != null ? QName.valueOf(scope) : null;
    }

    public Class getRuntimeClass() {
        return this.runtimeClass;
    }

    public void setRuntimeClass(Class runtimeClass) {
        this.runtimeClass = runtimeClass;
    }

    public boolean isProcessor() {
        return processor;
    }

    public void setProcessor(boolean processor) {
        this.processor = processor;
    }

    public boolean getFieldInjection() {
        return fieldInjection;
    }
    
    public void setFieldInjection(boolean fieldInjection) {
        this.fieldInjection = fieldInjection;
    }
    
    public boolean getRawConversion() {
        return rawConversion;
    }

    public void setRawConversion(boolean rawConversion) {
        this.rawConversion = rawConversion;
    }

    @Override
    public String toString() {
        return "BeanMetadata[" +
                "id='" + id + '\'' +
                ", initialization=" + initialization +
                ", dependsOn=" + dependsOn +
                ", className='" + className + '\'' +
                ", initMethodName='" + initMethod + '\'' +
                ", destroyMethodName='" + destroyMethod + '\'' +
                ", arguments=" + arguments +
                ", properties=" + properties +
                ", factoryMethodName='" + factoryMethod + '\'' +
                ", factoryComponent=" + factoryComponent +
                ", scope='" + scope + '\'' +
                ", runtimeClass=" + runtimeClass +
                ", fieldInjection=" + fieldInjection + 
                ']';
    }
}

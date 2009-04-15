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
package org.apache.felix.blueprint.reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.Value;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class LocalComponentMetadataImpl extends ComponentMetadataImpl implements LocalComponentMetadata {

    private String className;
    private String scope;
    private boolean isLazy;
    private String initMethodName;
    private String destroyMethodName;
    private ConstructorInjectionMetadataImpl constructorInjectionMetadata;
    private Collection<PropertyInjectionMetadata> propertyInjectionMetadata;
    private Collection<MethodInjectionMetadata> methodInjectionMetadata;
    private MethodInjectionMetadata factoryMethodMetadata;
    private Value factoryComponent;

    public LocalComponentMetadataImpl() {
        constructorInjectionMetadata = new ConstructorInjectionMetadataImpl();
        propertyInjectionMetadata = new ArrayList<PropertyInjectionMetadata>();
    }

    public LocalComponentMetadataImpl(LocalComponentMetadata source) {
        super(source);
        initMethodName = source.getInitMethodName();
        destroyMethodName = source.getDestroyMethodName();
        className = source.getClassName();
        scope = source.getScope();
        isLazy = source.isLazy();
    }
    
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getInitMethodName() {
        return initMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }

    public ConstructorInjectionMetadata getConstructorInjectionMetadata() {
        return constructorInjectionMetadata;
    }

    public Collection<PropertyInjectionMetadata> getPropertyInjectionMetadata() {
        return Collections.unmodifiableCollection(propertyInjectionMetadata);
    }

    public Collection<MethodInjectionMetadata> getMethodInjectionMetadata() {
        return Collections.unmodifiableCollection(methodInjectionMetadata);
    }

    public void setMethodInjectionMetadata(Collection<MethodInjectionMetadata> methodInjectionMetadata) {
        this.methodInjectionMetadata = methodInjectionMetadata;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    public MethodInjectionMetadata getFactoryMethodMetadata() {
        return factoryMethodMetadata;
    }

    public void setFactoryMethodMetadata(MethodInjectionMetadata factoryMethodMetadata) {
        this.factoryMethodMetadata = factoryMethodMetadata;
    }

    public Value getFactoryComponent() {
        return factoryComponent;
    }

    public void setFactoryComponent(Value factoryComponent) {
        this.factoryComponent = factoryComponent;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void addConsuctorArg(ParameterSpecification parameterSpecification) {
        constructorInjectionMetadata.addParameterSpecification(parameterSpecification);
    }

    public void addProperty(PropertyInjectionMetadataImpl propertyInjectionMetadata) {
        this.propertyInjectionMetadata.add(propertyInjectionMetadata);
    }
}

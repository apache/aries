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

import java.util.ArrayList;
import java.util.List;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.ExtendedBeanMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.spring.BlueprintBeanFactory.SpringMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;

public class SpringBeanProcessor implements BeanProcessor, ComponentDefinitionRegistryProcessor {

    private final BundleContext bundleContext;
    private final ExtendedBlueprintContainer blueprintContainer;
    private final SpringApplicationContext applicationContext;
    boolean creatingProcessor;

    public SpringBeanProcessor(
            BundleContext bundleContext,
            ExtendedBlueprintContainer blueprintContainer,
            SpringApplicationContext applicationContext) {
        this.bundleContext = bundleContext;
        this.blueprintContainer = blueprintContainer;
        this.applicationContext = applicationContext;
    }

    @Override
    public void process(ComponentDefinitionRegistry componentDefinitionRegistry) {
        applicationContext.process();
    }

    @Override
    public Object beforeInit(Object o, String s, BeanCreator beanCreator, BeanMetadata beanMetadata) {
        if (beanMetadata instanceof SpringMetadata || beanMetadata == null) {
            return o;
        }
        if (o instanceof Aware) {
            if (o instanceof BeanNameAware) {
                ((BeanNameAware) o).setBeanName(s);
            }
            if (o instanceof BeanClassLoaderAware) {
                ClassLoader cl = bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();
                ((BeanClassLoaderAware) o).setBeanClassLoader(cl);
            }
            if (o instanceof BeanFactoryAware) {
                ((BeanFactoryAware) o).setBeanFactory(applicationContext.getBeanFactory());
            }
        }
        return applicationContext.getBeanFactory().applyBeanPostProcessorsBeforeInitialization(o, s);
    }

    @Override
    public Object afterInit(Object o, String s, BeanCreator beanCreator, BeanMetadata beanMetadata) {
        return applicationContext.getBeanFactory().applyBeanPostProcessorsAfterInitialization(o, s);
    }

    @Override
    public void beforeDestroy(Object o, String s) {
        for (BeanPostProcessor processor : applicationContext.getBeanFactory().getBeanPostProcessors()) {
            if (processor instanceof DestructionAwareBeanPostProcessor) {
                ((DestructionAwareBeanPostProcessor) processor).postProcessBeforeDestruction(o, s);
            }
        }
    }

    @Override
    public void afterDestroy(Object o, String s) {
    }

    private <T> List<T> getProcessors(Class<T> type) {
        List<T> processors = new ArrayList<T>();
        if (!creatingProcessor) {
            creatingProcessor = true;
            for (BeanMetadata bean : blueprintContainer.getMetadata(BeanMetadata.class)) {
                Class clazz = null;
                if (bean instanceof ExtendedBeanMetadata) {
                    clazz = ((ExtendedBeanMetadata) bean).getRuntimeClass();
                }
                if (clazz == null && bean.getClassName() != null) {
                    try {
                        clazz = bundleContext.getBundle().loadClass(bean.getClassName());
                    } catch (ClassNotFoundException e) {
                    }
                }
                if (clazz == null) {
                    continue;
                }
                if (type.isAssignableFrom(clazz)) {
                    Object p = blueprintContainer.getComponentInstance(bean.getId());
                    processors.add(type.cast(p));
                }
            }
            creatingProcessor = false;
        }
        return processors;
    }

}

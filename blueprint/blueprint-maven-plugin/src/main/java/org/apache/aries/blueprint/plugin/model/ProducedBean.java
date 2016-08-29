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
package org.apache.aries.blueprint.plugin.model;


import org.apache.aries.blueprint.plugin.Extensions;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;

import java.lang.reflect.Method;

public class ProducedBean extends Bean {
    public String factoryMethod;
    public BeanRef factoryBean;
    private Method producingMethod;

    public ProducedBean(Class<?> clazz, BeanRef factoryBean, Method factoryMethod) {
        this(clazz, null, factoryBean, factoryMethod);
    }

    public ProducedBean(Class<?> clazz, String id, BeanRef factoryBean, Method factoryMethod) {
        super(clazz);
        if (id != null) {
            this.id = id;
        }
        this.factoryBean = factoryBean;
        this.factoryMethod = factoryMethod.getName();
        this.producingMethod = factoryMethod;
        handleCustomBeanAnnotations();
    }

    private void handleCustomBeanAnnotations() {
        for (BeanAnnotationHandler beanAnnotationHandler : Extensions.BEAN_ANNOTATION_HANDLERs) {
            Object annotation = AnnotationHelper.findAnnotation(producingMethod.getAnnotations(), beanAnnotationHandler.getAnnotation());
            if (annotation != null) {
                beanAnnotationHandler.handleBeanAnnotation(producingMethod, id, this, this);
            }
        }
    }

    public void setSingleton() {
        this.isPrototype = false;
    }

    @Override
    protected void resolveArguments(BlueprinRegister matcher) {
        resolveArguments(matcher, producingMethod.getParameterTypes(), producingMethod.getParameterAnnotations());
    }
}

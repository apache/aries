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
package org.apache.aries.blueprint.plugin.handlers.blueprint.bean;

import org.apache.aries.blueprint.annotation.bean.Activation;
import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.bean.Scope;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.BeanFinder;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.FactoryMethodFinder;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;

import java.lang.reflect.AnnotatedElement;

public class BeanHandler implements
        BeanFinder<Bean>,
        FactoryMethodFinder<Bean>,
        NamedLikeHandler<Bean>,
        BeanAnnotationHandler<Bean> {
    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Class<Bean> getAnnotation() {
        return Bean.class;
    }

    @Override
    public String getName(Class clazz, AnnotatedElement annotatedElement) {
        Bean bean = annotatedElement.getAnnotation(Bean.class);
        if ("".equals(bean.id())) {
            return null;
        }
        return bean.id();
    }

    @Override
    public String getName(Object annotation) {
        Bean bean = Bean.class.cast(annotation);
        if ("".equals(bean.id())) {
            return null;
        }
        return bean.id();
    }

    @Override
    public void handleBeanAnnotation(AnnotatedElement annotatedElement, String id,
                                     ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        Bean annotation = annotatedElement.getAnnotation(Bean.class);
        if (annotation.activation() != Activation.DEFAULT) {
            beanEnricher.addAttribute("activation", annotation.activation().name().toLowerCase());
        }
        beanEnricher.addAttribute("scope", annotation.scope() == Scope.SINGLETON ? "singleton" : "prototype");
        if (annotation.dependsOn().length > 0) {
            StringBuilder dependsOn = new StringBuilder();
            for (int i = 0; i < annotation.dependsOn().length; i++) {
                if (i > 0) {
                    dependsOn.append(" ");
                }
                dependsOn.append(annotation.dependsOn()[i]);
            }
            beanEnricher.addAttribute("depends-on", dependsOn.toString());
        }
        if (!annotation.initMethod().isEmpty()) {
            beanEnricher.addAttribute("init-method", annotation.initMethod());
        }
        if (!annotation.destroyMethod().isEmpty()) {
            beanEnricher.addAttribute("destroy-method", annotation.destroyMethod());
        }
    }
}

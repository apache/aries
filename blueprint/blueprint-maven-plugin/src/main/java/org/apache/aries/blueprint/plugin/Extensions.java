/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin;

import org.apache.aries.blueprint.plugin.spi.BeanFinder;
import org.apache.aries.blueprint.plugin.spi.BeanAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.CustomFactoryMethodAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.InjectLikeHandler;
import org.apache.aries.blueprint.plugin.spi.MethodAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;
import org.apache.aries.blueprint.plugin.spi.ValueInjectionHandler;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Extensions {
    public static final List<Class<? extends Annotation>> beanMarkingAnnotationClasses = new ArrayList<>();
    public static final List<Class<? extends Annotation>> singletons = new ArrayList<>();
    public static final List<InjectLikeHandler<? extends Annotation>> beanInjectLikeHandlers = new ArrayList<>();
    public static final List<NamedLikeHandler> namedLikeHandlers = new ArrayList<>();
    public static final List<ValueInjectionHandler<? extends Annotation>> valueInjectionHandlers = new ArrayList<>();
    public static final List<BeanAnnotationHandler<? extends Annotation>> BEAN_ANNOTATION_HANDLERs = new ArrayList<>();
    public static final List<CustomFactoryMethodAnnotationHandler<? extends Annotation>> customFactoryMethodAnnotationHandlers = new ArrayList<>();
    public static final List<CustomDependencyAnnotationHandler<? extends Annotation>> customDependencyAnnotationHandlers = new ArrayList<>();
    public static final List<MethodAnnotationHandler<? extends Annotation>> methodAnnotationHandlers = new ArrayList<>();

    static {
        for (BeanFinder beanFinder : ServiceLoader.load(BeanFinder.class)) {
            beanMarkingAnnotationClasses.add(beanFinder.beanAnnotation());
            if (beanFinder.isSingleton()) {
                singletons.add(beanFinder.beanAnnotation());
            }
        }

        for (InjectLikeHandler<? extends Annotation> injectLikeHandler : ServiceLoader.load(InjectLikeHandler.class)) {
            beanInjectLikeHandlers.add(injectLikeHandler);
        }

        for (NamedLikeHandler namedLikeHandler : ServiceLoader.load(NamedLikeHandler.class)) {
            namedLikeHandlers.add(namedLikeHandler);
        }

        for (ValueInjectionHandler<? extends Annotation> valueInjectionHandler : ServiceLoader.load(ValueInjectionHandler.class)) {
            valueInjectionHandlers.add(valueInjectionHandler);
        }

        for (BeanAnnotationHandler<? extends Annotation> beanAnnotationHandler : ServiceLoader.load(BeanAnnotationHandler.class)) {
            BEAN_ANNOTATION_HANDLERs.add(beanAnnotationHandler);
        }

        for (CustomFactoryMethodAnnotationHandler<? extends Annotation> customFactoryMethodAnnotationHandler : ServiceLoader.load(CustomFactoryMethodAnnotationHandler.class)) {
            customFactoryMethodAnnotationHandlers.add(customFactoryMethodAnnotationHandler);
        }

        for (CustomDependencyAnnotationHandler<? extends Annotation> customDependencyAnnotationHandler : ServiceLoader.load(CustomDependencyAnnotationHandler.class)) {
            customDependencyAnnotationHandlers.add(customDependencyAnnotationHandler);
        }

        for (MethodAnnotationHandler<? extends Annotation> methodAnnotationHandler : ServiceLoader.load(MethodAnnotationHandler.class)) {
            methodAnnotationHandlers.add(methodAnnotationHandler);
        }
    }
}

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
package org.apache.aries.blueprint.plugin;

import org.apache.aries.blueprint.plugin.handlers.Handlers;
import org.apache.xbean.finder.ClassFinder;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class FilteredClassFinder {

    @SuppressWarnings("unchecked")
    static Set<Class<?>> findClasses(ClassFinder finder, Collection<String> packageNames) {
        return findClasses(finder, packageNames, Handlers.BEAN_MARKING_ANNOTATION_CLASSES.toArray(new Class[Handlers.BEAN_MARKING_ANNOTATION_CLASSES.size()]));
    }

    private static Set<Class<?>> findClasses(ClassFinder finder, Collection<String> packageNames, Class<? extends Annotation>[] annotations) {
        Set<Class<?>> rawClasses = new HashSet<Class<?>>();
        for (Class<? extends Annotation> annotation : annotations) {
            rawClasses.addAll(finder.findAnnotatedClasses(annotation));
        }
        return filterByBasePackages(rawClasses, packageNames);
    }
    
    private static Set<Class<?>> filterByBasePackages(Set<Class<?>> rawClasses, Collection<String> packageNames) {
        Set<Class<?>> filteredClasses = new HashSet<Class<?>>();
        for (Class<?> clazz : rawClasses) {
            for (String packageName : packageNames) {
                if (clazz.getPackage().getName().startsWith(packageName)) {
                    filteredClasses.add(clazz);
                }
            }
        }
        return filteredClasses;
    }
}

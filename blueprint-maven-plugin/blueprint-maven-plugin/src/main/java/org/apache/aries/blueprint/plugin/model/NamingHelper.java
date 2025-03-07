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

import java.lang.reflect.AnnotatedElement;

class NamingHelper {
    static String getBeanName(Class<?> clazz) {
        return getBeanName(clazz, clazz);
    }

    private static String getBeanName(Class<?> clazz, AnnotatedElement annotatedElement) {
        String name = AnnotationHelper.findName(annotatedElement.getAnnotations());
        if (name != null) {
            return name;
        }
        return getBeanNameFromSimpleName(clazz.getSimpleName());
    }

    private static String getBeanNameFromSimpleName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
    }

}

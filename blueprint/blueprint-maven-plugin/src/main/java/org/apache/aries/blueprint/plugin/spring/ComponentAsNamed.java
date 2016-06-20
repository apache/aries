/**
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
package org.apache.aries.blueprint.plugin.spring;

import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.AnnotatedElement;

public class ComponentAsNamed implements NamedLikeHandler {
    @Override
    public Class getAnnotation() {
        return Component.class;
    }

    @Override
    public String getName(Class clazz, AnnotatedElement annotatedElement) {
        Component annotation = annotatedElement.getAnnotation(Component.class);
        if (annotation != null && annotation.value() != null && !"".equals(annotation.value())) {
            return annotation.value();
        }
        return null;
    }

    @Override
    public String getName(Object annotation) {
        Component component = Component.class.cast(annotation);
        if (component != null && component.value() != null && !"".equals(component.value())) {
            return component.value();
        }
        return null;
    }
}

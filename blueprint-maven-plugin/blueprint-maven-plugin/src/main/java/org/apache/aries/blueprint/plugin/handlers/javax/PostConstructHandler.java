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
package org.apache.aries.blueprint.plugin.handlers.javax;

import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.MethodAnnotationHandler;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.List;

public class PostConstructHandler implements MethodAnnotationHandler<PostConstruct> {
    @Override
    public Class<PostConstruct> getAnnotation() {
        return PostConstruct.class;
    }

    @Override
    public void handleMethodAnnotation(Class<?> clazz, List<Method> methods, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        if(methods.size() > 1){
            throw new IllegalArgumentException("There can be only one method annotated with @PostConstruct in bean");
        }
        beanEnricher.addAttribute("init-method", methods.get(0).getName());
    }
}

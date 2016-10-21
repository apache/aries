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
package org.apache.aries.blueprint.plugin.javax;

import org.apache.aries.blueprint.plugin.spi.BeanEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.MethodAnnotationHandler;

import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.List;

public class PreDestroyHandler implements MethodAnnotationHandler<PreDestroy> {
    @Override
    public Class<PreDestroy> getAnnotation() {
        return PreDestroy.class;
    }

    @Override
    public void handleMethodAnnotation(Class<?> clazz, List<Method> methods, ContextEnricher contextEnricher, BeanEnricher beanEnricher) {
        if(methods.size() > 1){
            throw new IllegalArgumentException("There can be only one method annotated with @PreDestroy in bean");
        }
        beanEnricher.addAttribute("destroy-method", methods.get(0).getName());
    }
}

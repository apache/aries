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
package org.apache.aries.blueprint.intercept;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class TheProcessor implements BeanProcessor {

    private BlueprintContainer blueprintContainer;

    public BlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    @Override
    public Object beforeInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        if (bean.getClass() == BeanA.class) {
            ((ExtendedBlueprintContainer) blueprintContainer)
                    .getComponentDefinitionRegistry()
                    .registerInterceptorWithComponent(beanData, new TheInterceptor());
        }
        return bean;
    }

    @Override
    public Object afterInit(Object o, String s, BeanCreator beanCreator, BeanMetadata beanMetadata) {
        return o;
    }

    @Override
    public void beforeDestroy(Object o, String s) {

    }

    @Override
    public void afterDestroy(Object o, String s) {

    }

}

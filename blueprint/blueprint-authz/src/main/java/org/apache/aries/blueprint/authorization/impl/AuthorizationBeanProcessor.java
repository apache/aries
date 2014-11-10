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
package org.apache.aries.blueprint.authorization.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class AuthorizationBeanProcessor implements BeanProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationInterceptor.class);
    public static final String AUTH_PROCESSOR_BEAN_NAME = "org_apache_aries_authz_annotations";
    private ComponentDefinitionRegistry cdr;

    public AuthorizationBeanProcessor() {
    }

    public void setCdr(ComponentDefinitionRegistry cdr) {
        this.cdr = cdr;
    }

    public void afterDestroy(Object arg0, String arg1) {
    }

    public Object afterInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        return bean;
    }

    public void beforeDestroy(Object arg0, String arg1) {
    }

    public Object beforeInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        Class<?> c = bean.getClass();
        if (new SecurityAnotationParser().isSecured(c)) {
            LOGGER.debug("Adding annotation based authorization interceptor for bean {} with class {}", beanName, c);
            cdr.registerInterceptorWithComponent(beanData, new AuthorizationInterceptor());
        }
        return bean;
    }

}

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
package org.apache.aries.blueprint.plugin.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Named;

import org.ops4j.pax.cdi.api.OsgiService;
import org.springframework.beans.factory.annotation.Qualifier;

public class Context implements Matcher {

    SortedSet<Bean> beans;
    SortedSet<OsgiServiceBean> serviceRefs;
    
    public Context(Class<?>... beanClasses) {
        this(Arrays.asList(beanClasses));
    }

    public Context(Collection<Class<?>> beanClasses) {
        this.beans = new TreeSet<Bean>();
        this.serviceRefs = new TreeSet<OsgiServiceBean>();
        addBeans(beanClasses);
    }

    private void addBeans(Collection<Class<?>> beanClasses) {
        for (Class<?> clazz : beanClasses) {
            Bean bean = new Bean(clazz);
            beans.add(bean);
            addServiceRefs(clazz);
        }
    }

    private void addServiceRefs(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            OsgiService osgiService = field.getAnnotation(OsgiService.class);
            if (osgiService != null) {
                serviceRefs.add(new OsgiServiceBean(field.getType(), osgiService));
            }
        }
    }
    
    public void resolve() {
        for (Bean bean : beans) {
            bean.resolve(this);
        }
    }
    
    public Bean getMatching(Field field) {
        String destId = getDestinationId(field);
        // TODO Replace loop by lookup
        for (Bean bean : beans) {
            if (bean.matches(field.getType(), destId)) {
                return bean;
            }
        }
        for (Bean bean : serviceRefs) {
            if (bean.matches(field.getType(), destId)) {
                return bean;
            }
        }
        return null;
    }

	private String getDestinationId(Field field) {
		Named named = field.getAnnotation(Named.class);
		if (named != null) {
			return named.value();
		}
		Qualifier qualifier = field.getAnnotation(Qualifier.class);
        if (qualifier != null) {
        	return qualifier.value();
        }
		return null;
	}

    public SortedSet<Bean> getBeans() {
        return beans;
    }
    
    public SortedSet<OsgiServiceBean> getServiceRefs() {
        return serviceRefs;
    }


}

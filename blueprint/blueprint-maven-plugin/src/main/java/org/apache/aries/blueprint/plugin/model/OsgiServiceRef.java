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

import org.ops4j.pax.cdi.api.OsgiService;

/**
 * Synthetic bean that refers to an OSGi service
 */
public class OsgiServiceRef extends BeanRef {

    public String filter;

    public OsgiServiceRef(Field field) {
        super(field);
        if (id == null) {
            id = getBeanName(clazz);
        }
        OsgiService osgiService = field.getAnnotation(OsgiService.class);
        filter = osgiService.filter();
    }

}

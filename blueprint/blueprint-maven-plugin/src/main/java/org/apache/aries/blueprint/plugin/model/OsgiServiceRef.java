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

    final public String filter;
    final public String compName;


    public OsgiServiceRef(Field field) {
        super(field);
        OsgiService osgiService = field.getAnnotation(OsgiService.class);
        String filterValue = osgiService.filter();
        if (filterValue.contains("(")) {
            filter = filterValue;
            compName = null;
        } else {
            compName = filterValue;
            filter = null;
        }
        id = getBeanName(clazz);
        if (filter != null) {
            id = id + "-" + getId(filter);
        }
    }

    public OsgiServiceRef(Class<?> clazz, OsgiService osgiService, String name) {
        super(clazz, name);
        String filterValue = osgiService.filter();
        if (filterValue.contains("(")) {
            filter = filterValue;
            compName = null;
        } else {
            compName = filterValue;
            filter = null;
        }
    }


    private String getId(String raw) {
        StringBuilder builder = new StringBuilder();
        for (int c = 0; c < raw.length(); c++) {
            char ch = raw.charAt(c);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

}

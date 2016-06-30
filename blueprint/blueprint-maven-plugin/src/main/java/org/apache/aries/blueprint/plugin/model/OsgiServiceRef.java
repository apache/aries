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

import org.apache.aries.blueprint.plugin.Extensions;
import org.apache.aries.blueprint.plugin.spi.NamedLikeHandler;
import org.ops4j.pax.cdi.api.OsgiService;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Synthetic bean that refers to an OSGi service
 */
public class OsgiServiceRef extends BeanRef {

    final public String filter;
    final public String compName;

    public OsgiServiceRef(Field field) {
        super(field);
        ServiceFilter serviceFilter = extractServiceFilter(field);
        filter = serviceFilter.filter;
        compName = serviceFilter.compName;
        id = generateReferenceId(field);
    }

    private String generateReferenceId(AnnotatedElement annotatedElement) {
        String prefix = getBeanName(clazz, annotatedElement);
        String suffix = createIdSuffix(annotatedElement);
        return prefix + suffix;
    }

    private String createIdSuffix(AnnotatedElement annotatedElement) {
        if (shouldAddSuffix(annotatedElement)) {
            if (filter != null) {
                return "-" + getId(filter);
            }
            if (compName != null) {
                return "-" + compName;
            }
        }
        return "";
    }

    private boolean shouldAddSuffix(AnnotatedElement annotatedElement) {
        for (NamedLikeHandler namedLikeHandler : Extensions.namedLikeHandlers) {
            if (annotatedElement.getAnnotation(namedLikeHandler.getAnnotation()) != null) {
                String name = namedLikeHandler.getName(clazz, annotatedElement);
                if (name != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public OsgiServiceRef(Method method) {
        super(method);
        ServiceFilter serviceFilter = extractServiceFilter(method);
        filter = serviceFilter.filter;
        compName = serviceFilter.compName;
        id = generateReferenceId(method);
    }

    private ServiceFilter extractServiceFilter(AnnotatedElement annotatedElement) {
        OsgiService osgiService = annotatedElement.getAnnotation(OsgiService.class);
        return extractServiceFilter(osgiService);
    }

    private ServiceFilter extractServiceFilter(OsgiService osgiService) {
        String filterValue = osgiService.filter();
        return new ServiceFilter(filterValue);
    }

    public OsgiServiceRef(Class<?> clazz, OsgiService osgiService, String name) {
        super(clazz, name);
        ServiceFilter serviceFilter = extractServiceFilter(osgiService);
        filter = serviceFilter.filter;
        compName = serviceFilter.compName;
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

    private static class ServiceFilter {
        final public String filter;
        final public String compName;

        public ServiceFilter(String filterValue) {
            if (filterValue == null) {
                filter = null;
                compName = null;
            } else if (filterValue.contains("(")) {
                filter = filterValue;
                compName = null;
            } else {
                filter = null;
                compName = filterValue;
            }
        }
    }
}

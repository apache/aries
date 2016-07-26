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

package org.apache.aries.proxy.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class SystemModuleClassLoader extends ClassLoader {

    private static java.lang.reflect.Method method_Class_getModule;
    private static java.lang.reflect.Method method_Module_getResourceAsStream ;
    static {
        try {
            method_Class_getModule = Class.class.getMethod("getModule");
            method_Module_getResourceAsStream = method_Class_getModule.getReturnType()
                .getMethod("getResourceAsStream", String.class);
        } catch (NoSuchMethodException e) {
            //this isn't java9 with jigsaw

        }
    }


    public SystemModuleClassLoader(ClassLoader parentLoader) {
        super(parentLoader);
    }

    public SystemModuleClassLoader() {
        super();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        if (url == null) {
            // try java9 module resource loader
            if (method_Class_getModule == null || method_Module_getResourceAsStream == null) {
                return null; // not Java 9 JIGSAW
            }
            try {
                String className = name.replace('/', '.');
                int lastDot = className.lastIndexOf('.');
                className = className.substring(0, lastDot);
                final Class<?> clazz = Class.forName(className, false, this);
                final Object module = method_Class_getModule.invoke(clazz);
                return (InputStream)method_Module_getResourceAsStream
                    .invoke(module, name);
            } catch (Exception e) {
                return null; // not found
            }
        } else {
            try {
                return url.openStream();
            } catch (IOException e) {
                return null;
            }
        }
    }
}

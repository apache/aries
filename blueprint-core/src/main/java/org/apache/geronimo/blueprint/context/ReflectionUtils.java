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
package org.apache.geronimo.blueprint.context;

import java.lang.reflect.Method;
import java.util.Set;

public class ReflectionUtils {
           
    public static Set<String> getImplementedInterfaces(Set<String> classes, Class clazz) {
        if (clazz != null && clazz != Object.class) {
            for (Class itf : clazz.getInterfaces()) {
                classes.add(itf.getName());
                getImplementedInterfaces(classes, itf);
            }
            getImplementedInterfaces(classes, clazz.getSuperclass());
        }
        return classes;
    }

    public static Set<String> getSuperClasses(Set<String> classes, Class clazz) {
        if (clazz != null && clazz != Object.class) {
            classes.add(clazz.getName());
            getSuperClasses(classes, clazz.getSuperclass());
        }
        return classes;
    }
    
    public static Method findMethod(Class clazz, String name, Class[] paramTypes) {    
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return findCompatibileMethod(clazz, name, paramTypes);
        }
    }

    public static Method findCompatibileMethod(Class clazz, String name, Class[] paramTypes) {
        Method[] methods = clazz.getMethods();
        for (Method method :  methods) {
            Class[] methodParams = method.getParameterTypes();
            if (name.equals(method.getName()) && methodParams.length == paramTypes.length) {
                boolean assignable = true;
                for (int i = 0; i < paramTypes.length && assignable; i++) {
                    assignable = methodParams[i].isAssignableFrom(paramTypes[i]);
                }
                if (assignable) {
                    return method;
                }                        
            }
        }
        return null;
    }        
}

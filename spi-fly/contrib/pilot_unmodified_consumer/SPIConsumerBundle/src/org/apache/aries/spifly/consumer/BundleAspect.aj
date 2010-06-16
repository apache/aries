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
package org.apache.aries.spifly.consumer;

import java.util.Arrays;
import java.util.ServiceLoader;

import org.apache.aries.spifly.api.SPIClassloaderAdviceService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public aspect BundleAspect {
    pointcut serviceloader(Class cls) : 
        args(cls) && call(ServiceLoader ServiceLoader.load(Class));
    
    ServiceLoader around(Class cls) : serviceloader(cls) {
        ClassLoader loader = SPIClassloaderAdviceService.class.getClassLoader();
        System.out.println("++++++++++++++++" + loader);
        if (!(loader instanceof BundleReference)) { 
            return proceed(cls);
        }
        BundleReference bref = (BundleReference) loader;
        Bundle b = bref.getBundle();
        System.out.println("Bundle: " + b);

        BundleContext ctx = b.getBundleContext();
        ClassLoader targetLoader = null;
        try {
            ServiceReference[] refs = ctx.getServiceReferences(SPIClassloaderAdviceService.class.getName(), 
                    "(AdviceClass=" + cls.getName() + ")");
            if (refs == null) {
                return proceed(cls);
            }
            
            System.out.println("Services: " + Arrays.toString(refs));
            for (int i=0 ; i < refs.length && targetLoader == null; i++) {
                Object svc = ctx.getService(refs[i]);
                if (svc instanceof SPIClassloaderAdviceService) {
                    targetLoader = ((SPIClassloaderAdviceService) svc).getServiceClassLoader(cls);
                }
            }
        } catch (InvalidSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (targetLoader == null) {
            return proceed(cls);
        }        
        
        ClassLoader prevCl = Thread.currentThread().getContextClassLoader();                
        try {
            System.out.println("Setting thread context classloader to " + targetLoader);
            Thread.currentThread().setContextClassLoader(targetLoader);
            return proceed(cls);            
        } finally {
            Thread.currentThread().setContextClassLoader(prevCl);                       
        }
    }
}

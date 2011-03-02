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
package org.apache.aries.spifly.aop.sample;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.aries.spifly.aop.sample.HeaderParser.PathElement;
import org.apache.aries.spifly.api.SPIClassloaderAdviceService;
import org.apache.aries.spifly.api.SpiFlyConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public aspect BundleAspect {

    // TODO 1. how to instrument somebody's else calls? if there's a way to do
    // it, will there be any security issues?

    // TODO remove System.out.println() invocations

    pointcut serviceloader(Class cls) : 
        args(cls) && call(static ServiceLoader ServiceLoader.load(Class));

    ServiceLoader around(Class cls) : serviceloader(cls) {

        System.out.println("Invoked for " + cls.getName());

        ClassLoader tcl = Thread.currentThread().getContextClassLoader();

        String spiConsumerHeader = null;
        Bundle callerBundle = null;

        if (tcl == null) {
            System.out
                    .println("Aspect triggered without a thread context classloader set. "
                            + "Will assign randomly chosen impl.");
        } else {
            System.out.println("Processing thread context classloader " + tcl);
            if (tcl instanceof BundleReference) {
                BundleReference callerRef = (BundleReference) tcl;
                callerBundle = callerRef.getBundle();
                System.out.println("Found caller bundle: " + callerBundle);
                Object header = callerBundle.getHeaders().get(
                        SpiFlyConstants.SPI_CONSUMER_HEADER);
                if (header == null) {
                    System.out.println("SPI-Consumer header is missing. "
                            + "Will assign randomly chosen impl.");
                } else {
                    spiConsumerHeader = (String) header;
                }

            } else {
                System.out.println("Classloader is not a bundle reference. "
                        + "Will assign randomly chosen impl.");
            }

        }

        BundleContext ctx = null;
        if (callerBundle != null) {
            ctx = callerBundle.getBundleContext();
        } else {
            // TODO if unable to get the caller bundle, try to get it from the
            // bundle that provided this aspect - right now it should be the
            // same one that is doing the invocation; if a way to run an aspect
            // on behalf of other bundle is found, this will not make much sense
            ctx = ((BundleReference) org.apache.aries.spifly.Activator.class
                    .getClassLoader()).getBundle().getBundleContext();
        }

        // if we managed to retrieve the header, we need to parse it and get the
        // preferences defined there; if there's no header, the consumer bundle
        // will not be able to influence the process of chosing the provider
        Map providerNames = new HashMap();
        if (spiConsumerHeader != null) {
            // parse the optional SPI Consumer header
            List parsedHeader = HeaderParser.parseHeader(spiConsumerHeader);

            for (int i = 0; i < parsedHeader.size(); i++) {
                PathElement pe = (PathElement) parsedHeader.get(i);

                String apiName = pe.getName();
                String providerName = pe
                        .getAttribute(SpiFlyConstants.PROVIDER_NAME_ATTRIBUTE);
                if (apiName != null && providerName != null) {
                    System.out.println("Adding mapping - provider: "
                            + providerName + " for API: " + apiName);
                    providerNames.put(apiName, providerName);
                }
            }
        }

        ClassLoader targetLoader = null;
        try {
            // Ask for SPIClassloaderAdviceService services that handle a given
            // service id (name of the class passed to the ServiceLoader.load()
            // method. For example for JAXB this will be
            // javax.xml.bind.JAXBContext as the invocation would look like this
            // ServiceLoader.load(JAXBContext.class);

            // Unfortunately, being only provided with the classname/service id
            // we don't know what's the api name, so we cannot add extra search
            // parameters here. Once we get all the providers that match a given
            // service id, we can then compare their api names and provider
            // names with the ones that are mentioned in the preferences defined
            // by the invoking consumer - in the providerNames collection.

            ServiceReference[] refs = ctx.getServiceReferences(
                    SPIClassloaderAdviceService.class.getName(), "(ServiceId="
                            + cls.getName() + ")");
            if (refs == null) {
                System.out.println("Skipping as no providers found.");
                return proceed(cls);
            }

            System.out.println("Services: " + Arrays.toString(refs));
            for (int i = 0; i < refs.length && targetLoader == null; i++) {
                Object svc = ctx.getService(refs[i]);
                if (svc instanceof SPIClassloaderAdviceService) {
                    // We weren't able to guess what's the API name being only
                    // provided with the class name. Now we can ask this given
                    // service instance for the api name. We can then check if
                    // we have an entry for this api name in our consumer
                    // header. If so, we need to check the provider name for
                    // this service is the same as the name of the one that is
                    // preferred by the calling bundle.
                    String apiName = (String) refs[i].getProperty("ApiName");
                    String requiredProviderName = (String) providerNames
                            .get(apiName);
                    if (requiredProviderName == null) {
                        // there was no criterion for this api name, we can
                        // safely return this classloader
                        targetLoader = ((SPIClassloaderAdviceService) svc)
                                .getServiceClassLoader(cls);
                        break;
                    } else {
                        if (requiredProviderName.equals(refs[i]
                                .getProperty("ProviderName"))) {
                            // The SPIClassloaderAdviceService that we are now
                            // processing has an api name attribute equal to one
                            // of the entries in the providerNames collection.
                            // Moreover, the provider name attribute for this
                            // service is the same as the value in the
                            // providerNames collection for this api name.
                            targetLoader = ((SPIClassloaderAdviceService) svc)
                                    .getServiceClassLoader(cls);
                            break;
                        }
                        // The SPIClassloaderAdviceService that we are now
                        // processing has an api name attribute equal to one
                        // of the entries in the providerNames collection.
                        // However, the provider name attribute for this
                        // service is NOT the same as the value in the
                        // providerNames collection for this api name. We need
                        // to skip this provider and continue searhing.
                    }

                }
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }

        if (targetLoader == null) {
            // no matching providers - proceed without chaning the classloader
            return proceed(cls);
        }

        ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
        try {
            System.out.println("Setting thread context classloader to "
                    + targetLoader);
            Thread.currentThread().setContextClassLoader(targetLoader);
            return proceed(cls);
        } finally {
            Thread.currentThread().setContextClassLoader(prevCl);
        }
    }
}

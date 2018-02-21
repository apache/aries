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
package org.apache.aries.jndi.tracker;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class ServiceTrackerCustomizers {

    public static final CachingServiceTracker<InitialContextFactory> ICF_CACHE = new BaseCachingServiceTracker<InitialContextFactory>() {
        public List<String> getProperty(ServiceReference<InitialContextFactory> ref) {
            String[] interfaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
            List<String> resultList = new ArrayList<String>();
            for (String interfaceName : interfaces) {
                if (!InitialContextFactory.class.getName().equals(interfaceName)) {
                    resultList.add(interfaceName);
                }
            }

            return resultList;
        }
    };
    // TODO we should probably cope with the url.scheme property changing.
    public static final CachingServiceTracker<ObjectFactory> URL_FACTORY_CACHE = new BaseCachingServiceTracker<ObjectFactory>() {
        protected List<String> getProperty(ServiceReference<ObjectFactory> reference) {
            Object scheme = reference.getProperty(JNDIConstants.JNDI_URLSCHEME);
            List<String> result;

            if (scheme instanceof String) {
                result = new ArrayList<String>();
                result.add((String) scheme);
            } else if (scheme instanceof String[]) {
                result = Arrays.asList((String[]) scheme);
            } else {
                result = Collections.emptyList();
            }

            return result;
        }
    };

    public static final <S> ServiceTrackerCustomizer<S, ServiceReference<S>> LAZY() {
        return new ServiceTrackerCustomizer<S, ServiceReference<S>>() {
            public ServiceReference<S> addingService(ServiceReference<S> reference) {
                return reference;
            }

            public void modifiedService(ServiceReference<S> reference, ServiceReference<S> service) {
            }

            public void removedService(ServiceReference<S> reference, ServiceReference<S> service) {
            }
        };
    }

    public interface CachingServiceTracker<S> extends ServiceTrackerCustomizer<S, ServiceReference<S>> {
        ServiceReference<S> find(String identifier);
    }

    private static abstract class BaseCachingServiceTracker<S> implements CachingServiceTracker<S> {
        /** The cached references */
        protected ConcurrentMap<String, ServiceReference<S>> cache = new ConcurrentHashMap<String, ServiceReference<S>>();
        /** A list of service references that are being tracked */
        protected List<ServiceReference<S>> trackedReferences = new ArrayList<ServiceReference<S>>();

        public ServiceReference<S> find(String identifier) {
            return cache.get(identifier);
        }

        public synchronized ServiceReference<S> addingService(ServiceReference<S> reference) {
            List<String> cacheKeys = getProperty(reference);

            for (String key : cacheKeys) {
                cache.putIfAbsent(key, reference);
            }

            trackedReferences.add(reference);

            return reference;
        }

        protected abstract List<String> getProperty(ServiceReference<S> reference);

        public synchronized void removedService(ServiceReference<S> reference, ServiceReference<S> service) {
            trackedReferences.remove(reference);

            List<String> keysToProcess = new ArrayList<String>(getProperty(reference));

            refLoop:
            for (ServiceReference<S> ref : trackedReferences) {
                List<String> refInt = getProperty(ref);
                for (String interfaceName : refInt) {
                    int index = keysToProcess.indexOf(interfaceName);
                    if (index >= 0) {
                        keysToProcess.remove(index);
                        if (cache.replace(interfaceName, reference, ref)) {
                            if (keysToProcess.isEmpty()) break refLoop;
                        }
                    }
                }
            }

            for (String interfaceName : keysToProcess) {
                cache.remove(interfaceName, reference);
            }
        }

        public void modifiedService(ServiceReference<S> reference, ServiceReference<S> service) {
        }
    }
}

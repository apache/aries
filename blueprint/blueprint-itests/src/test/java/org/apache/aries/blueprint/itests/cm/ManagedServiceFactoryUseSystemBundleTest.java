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
package org.apache.aries.blueprint.itests.cm;

import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.aries.blueprint.itests.Helper;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;

/**
 * Shows that the cm bundle can process config even if the events are hidden from it
 * when the property to use the system bundle context is set
 */
public class ManagedServiceFactoryUseSystemBundleTest extends ManagedServiceFactoryTest {
    private static final String CM_BUNDLE = "org.apache.aries.blueprint.cm";

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return new Option[] {
            baseOptions(),
            systemProperty("org.apache.aries.blueprint.use.system.context").value("true"),
            Helper.blueprintBundles(), //
            keepCaches(), //
            streamBundle(testBundle())
        };
    }

    ServiceRegistration<?> eventHook;
    ServiceRegistration<?> findHook;

    @Before
    public void regiserHook() throws BundleException {
        context().getBundleByName(CM_BUNDLE).stop();
        final BundleContext systemContext = context().getBundle(Constants.SYSTEM_BUNDLE_LOCATION)
            .getBundleContext();
        eventHook = context().registerService(EventListenerHook.class, new EventListenerHook() {

            @SuppressWarnings({
                "unchecked", "rawtypes"
            })
            @Override
            public void event(ServiceEvent event, Map contexts) {
                if (CM_BUNDLE.equals(event.getServiceReference().getBundle().getSymbolicName())) {
                    // hide from everything but the system bundle
                    // TODO on R6 we should be able to even try hiding from the system bundle
                    // R5 it was not clear if hooks could hide from the system bundle
                    // equinox R5 does allow hiding from system bundle
                    contexts.keySet().retainAll(Collections.singleton(systemContext));
                }
            }

        }, null);
        findHook = context().registerService(FindHook.class, new FindHook() {
            @SuppressWarnings({
                "rawtypes", "unchecked"
            })
            @Override
            public void find(BundleContext context, String arg1, String arg2, boolean arg3,
                             Collection references) {
                // hide from everything but the system bundle
                // TODO on R6 we should be able to even try hiding from the system bundle
                // R5 it was not clear if hooks could hide from the system bundle
                // equinox R5 does allow hiding from system bundle
                if (!context.equals(systemContext)) {
                    for (Iterator<ServiceReference> iReferences = references.iterator(); iReferences
                        .hasNext();) {
                        if (CM_BUNDLE.equals(iReferences.next().getBundle().getSymbolicName())) {
                            iReferences.remove();
                        }
                    }
                }
            }

        }, null);
        context().getBundleByName(CM_BUNDLE).start();
    }

    @After
    public void unregisterHook() {
        eventHook.unregister();
        findHook.unregister();
    }

}

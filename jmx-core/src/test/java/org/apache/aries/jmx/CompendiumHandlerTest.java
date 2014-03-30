/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.management.StandardMBean;

import org.apache.aries.jmx.agent.JMXAgentContext;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

public class CompendiumHandlerTest {

    protected AbstractCompendiumHandler target;

    @After
    public void tearDown(){
        target = null;
    }


    @Test
    public void testAddingServiceWillInitiateMBeanRegistration() throws Exception {
        Bundle mockSystemBundle = mock(Bundle.class);
        when(mockSystemBundle.getSymbolicName()).thenReturn("the.sytem.bundle");

        Object service = new Object();

        ServiceReference reference = mock(ServiceReference.class);
        when(reference.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        when(reference.getProperty(Constants.OBJECTCLASS)).thenReturn("the class");

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getProperty(Constants.FRAMEWORK_UUID)).thenReturn("some-uuid");
        when(bundleContext.getService(reference)).thenReturn(service);
        when(bundleContext.getBundle(0)).thenReturn(mockSystemBundle);

        Logger agentLogger = mock(Logger.class);
        JMXAgentContext agentContext = mock(JMXAgentContext.class);
        when(agentContext.getBundleContext()).thenReturn(bundleContext);
        when(agentContext.getLogger()).thenReturn(agentLogger);

        AbstractCompendiumHandler concreteHandler = new CompendiumHandler(agentContext, "org.osgi.service.Xxx");
        target = spy(concreteHandler);

        target.addingService(reference);

        //service only got once
        verify(bundleContext).getService(reference);
        //template method is invoked
        verify(target).constructInjectMBean(service);
        //registration is invoked on context
        verify(agentContext).registerMBean(target);

    }

    @Test
    public void testRemovedServiceWillUnregisterMBean() throws Exception{

        Object service = new Object();
        ServiceReference reference = mock(ServiceReference.class);
        when(reference.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
        when(reference.getProperty(Constants.OBJECTCLASS)).thenReturn("the class");

        BundleContext bundleContext = mock(BundleContext.class);
        Logger agentLogger = mock(Logger.class);
        JMXAgentContext agentContext = mock(JMXAgentContext.class);
        when(agentContext.getBundleContext()).thenReturn(bundleContext);
        when(agentContext.getLogger()).thenReturn(agentLogger);

        AbstractCompendiumHandler concreteHandler = new CompendiumHandler(agentContext, "org.osgi.service.Xxx");
        target = spy(concreteHandler);
        target.trackedId.set(1);

        String name = "osgi.compendium:service=xxx,version=1.0";
        doReturn(name).when(target).getName();

        target.removedService(reference, service);

        //service unget
        verify(bundleContext).ungetService(reference);
        //unregister is invoked on context
        verify(agentContext).unregisterMBean(target);

    }



    /*
     * Concrete implementation used for test
     */
    class CompendiumHandler extends AbstractCompendiumHandler {

        protected CompendiumHandler(JMXAgentContext agentContext, Filter filter) {
            super(agentContext, filter);
        }

        protected CompendiumHandler(JMXAgentContext agentContext, String clazz) {
            super(agentContext, clazz);
        }

        protected StandardMBean constructInjectMBean(Object targetService) {
            return null;
        }

        public String getBaseName() {
            return null;
        }

    }
}

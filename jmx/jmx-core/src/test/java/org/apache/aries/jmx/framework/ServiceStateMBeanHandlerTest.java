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
package org.apache.aries.jmx.framework;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.agent.JMXAgent;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class ServiceStateMBeanHandlerTest {


    @Test
    public void testOpen() throws Exception {

        BundleContext context = mock(BundleContext.class);
        when(context.getProperty(Constants.FRAMEWORK_UUID)).thenReturn("some-uuid");

        Logger logger = mock(Logger.class);

        Bundle mockSystemBundle = mock(Bundle.class);
        when(mockSystemBundle.getSymbolicName()).thenReturn("the.sytem.bundle");
        when(context.getBundle(0)).thenReturn(mockSystemBundle);

        JMXAgent agent = mock(JMXAgent.class);
        JMXAgentContext agentContext = new JMXAgentContext(context, agent, logger);

        ServiceStateMBeanHandler handler = new ServiceStateMBeanHandler(agentContext, new StateConfig());
        handler.open();

        assertNotNull(handler.getMbean());

    }

}

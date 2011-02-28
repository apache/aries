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
package org.apache.aries.jmx.provisioning;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import javax.management.StandardMBean;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class ProvisioningServiceMBeanHandlerTest {

    
    @Test
    public void testConstructInjectMBean() {
        
        BundleContext bundleContext = mock(BundleContext.class);
        Logger agentLogger = mock(Logger.class);   
        JMXAgentContext agentContext = new JMXAgentContext(bundleContext, null, agentLogger);
        ProvisioningService provService = mock(ProvisioningService.class);
        
        ProvisioningServiceMBeanHandler handler = new ProvisioningServiceMBeanHandler(agentContext);
        StandardMBean mbean = handler.constructInjectMBean(provService);
        assertNotNull(mbean);
        
    }


}

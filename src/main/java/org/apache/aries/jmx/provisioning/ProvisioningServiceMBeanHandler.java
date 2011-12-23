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

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.aries.jmx.AbstractCompendiumHandler;
import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.osgi.jmx.service.provisioning.ProvisioningServiceMBean;
import org.osgi.service.log.LogService;

/**
 * <p>
 * Implementation of <code>MBeanHandler</code> which manages the <code>ProvisioningServiceMBean</code> implementation
 *
 * @see MBeanHandler
 *
 * @version $Rev$ $Date$
 */
public class ProvisioningServiceMBeanHandler extends AbstractCompendiumHandler {

    /**
     * Constructs new ProvisioningServiceMBeanHandler instance
     *
     * @param agentContext
     *            JMXAgentContext instance
     */
    public ProvisioningServiceMBeanHandler(JMXAgentContext agentContext) {
        super(agentContext, "org.osgi.service.provisioning.ProvisioningService");
    }

    /**
     * @see org.apache.aries.jmx.AbstractCompendiumHandler#constructInjectMBean(java.lang.Object)
     */
    @Override
    protected StandardMBean constructInjectMBean(Object targetService) {
        ProvisioningService psMBean = new ProvisioningService(
                (org.osgi.service.provisioning.ProvisioningService) targetService);
        StandardMBean mbean = null;
        try {
            mbean = new StandardMBean(psMBean, ProvisioningServiceMBean.class);
        } catch (NotCompliantMBeanException e) {
            Logger logger = agentContext.getLogger();
            logger.log(LogService.LOG_ERROR, "Failed to instantiate MBean for "
                    + ProvisioningServiceMBean.class.getName(), e);
        }
        return mbean;
    }

    /**
     * @see org.apache.aries.jmx.AbstractCompendiumHandler#getBaseName()
     */
    protected String getBaseName() {
        return ProvisioningServiceMBean.OBJECTNAME;
    }

}

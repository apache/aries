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
package org.apache.aries.jmx.cm;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.aries.jmx.AbstractCompendiumHandler;
import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.osgi.jmx.service.cm.ConfigurationAdminMBean;
import org.osgi.service.log.LogService;

/**
 * <p>
 * Implementation of <code>MBeanHandler</code> which manages the <code>ConfigurationAdminMBean</code> implementation
 *
 * @see MBeanHandler </p>
 *
 * @version $Rev$ $Date$
 */
public class ConfigurationAdminMBeanHandler extends AbstractCompendiumHandler {

    /**
     * Constructs new ConfigurationAdminMBeanHandler instance
     *
     * @param agentContext
     *            JMXAgentContext instance
     */
    public ConfigurationAdminMBeanHandler(JMXAgentContext agentContext) {
        super(agentContext, "org.osgi.service.cm.ConfigurationAdmin");
    }

    /**
     * @see org.apache.aries.jmx.AbstractCompendiumHandler#constructInjectMBean(java.lang.Object)
     */
    @Override
    protected StandardMBean constructInjectMBean(Object targetService) {
        ConfigurationAdminMBean caMBean = new org.apache.aries.jmx.cm.ConfigurationAdmin(
                (org.osgi.service.cm.ConfigurationAdmin) targetService);
        StandardMBean mbean = null;
        try {
            mbean = new StandardMBean(caMBean, ConfigurationAdminMBean.class);
        } catch (NotCompliantMBeanException e) {
            Logger logger = agentContext.getLogger();
            logger.log(LogService.LOG_ERROR, "Failed to instantiate MBean for "
                    + ConfigurationAdminMBean.class.getName(), e);
        }
        return mbean;
    }

    /**
     * @see org.apache.aries.jmx.AbstractCompendiumHandler#getBaseName()
     */
    protected String getBaseName() {
        return ConfigurationAdminMBean.OBJECTNAME;
    }

}

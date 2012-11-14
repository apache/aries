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
package org.apache.aries.jmx.permissionadmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.aries.jmx.AbstractCompendiumHandler;
import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.osgi.jmx.service.permissionadmin.PermissionAdminMBean;
import org.osgi.service.log.LogService;

/**
 * <p>
 * <tt>PermissionAdminMBeanHandler</tt> represents MBeanHandler which
 * holding information about {@link PermissionAdminMBean}.</p>
 * @see MBeanHandler
 *
 * @version $Rev$ $Date$
 */
public class PermissionAdminMBeanHandler extends AbstractCompendiumHandler {

    /**
     * Constructs new PermissionAdminMBeanHandler.
     *
     * @param agentContext JMXAgentContext instance.
     */
    public PermissionAdminMBeanHandler(JMXAgentContext agentContext) {
        super(agentContext, "org.osgi.service.permissionadmin.PermissionAdmin");
    }

    /**
     * @see org.apache.aries.jmx.AbstractCompendiumHandler#constructInjectMBean(java.lang.Object)
     */
    @Override
    protected StandardMBean constructInjectMBean(Object targetService) {
        PermissionAdminMBean paMBean = new PermissionAdmin((org.osgi.service.permissionadmin.PermissionAdmin) targetService);
        StandardMBean mbean = null;
        try {
            mbean = new StandardMBean(paMBean, PermissionAdminMBean.class);
        } catch (NotCompliantMBeanException e) {
            Logger logger = agentContext.getLogger();
            logger.log(LogService.LOG_ERROR, "Not compliant MBean", e);
        }
        return mbean;
    }


    /**
     * @see org.apache.aries.jmx.AbstractCompendiumHandler#getBaseName()
     */
    protected String getBaseName() {
        return PermissionAdminMBean.OBJECTNAME;
    }
}
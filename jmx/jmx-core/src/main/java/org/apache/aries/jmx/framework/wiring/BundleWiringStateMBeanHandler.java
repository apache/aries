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
package org.apache.aries.jmx.framework.wiring;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.apache.aries.jmx.util.ObjectNameUtils;
import org.apache.aries.jmx.util.shared.RegistrableStandardEmitterMBean;
import org.osgi.framework.BundleContext;
import org.osgi.jmx.framework.wiring.BundleWiringStateMBean;
import org.osgi.service.log.LogService;

public class BundleWiringStateMBeanHandler implements MBeanHandler {

    private JMXAgentContext agentContext;
    private final String name;
    private final BundleContext bundleContext;
    private final Logger logger;

    private StandardMBean mbean;
    private BundleWiringState revisionsStateMBean;

    public BundleWiringStateMBeanHandler(JMXAgentContext agentContext) {
        this.agentContext = agentContext;
        this.bundleContext = agentContext.getBundleContext();
        this.logger = agentContext.getLogger();
        this.name = ObjectNameUtils.createFullObjectName(bundleContext, BundleWiringStateMBean.OBJECTNAME);
    }

    /* (non-Javadoc)
     * @see org.apache.aries.jmx.MBeanHandler#open()
     */
    public void open() {
        revisionsStateMBean = new BundleWiringState(bundleContext, logger);
        try {
            mbean = new RegistrableStandardEmitterMBean(revisionsStateMBean, BundleWiringStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            logger.log(LogService.LOG_ERROR, "Failed to instantiate MBean for " + BundleWiringStateMBean.class.getName(), e);
        }
        agentContext.registerMBean(this);
    }

    /* (non-Javadoc)
     * @see org.apache.aries.jmx.MBeanHandler#getMbean()
     */
    public StandardMBean getMbean() {
        return mbean;
    }


    /* (non-Javadoc)
     * @see org.apache.aries.jmx.MBeanHandler#close()
     */
    public void close() {
        agentContext.unregisterMBean(this);
    }

    /* (non-Javadoc)
     * @see org.apache.aries.jmx.MBeanHandler#getName()
     */
    public String getName() {
        return name;
    }
}

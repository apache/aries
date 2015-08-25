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

import static org.osgi.jmx.framework.ServiceStateMBean.OBJECTNAME;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.apache.aries.jmx.util.ObjectNameUtils;
import org.apache.aries.jmx.util.shared.RegistrableStandardEmitterMBean;
import org.osgi.framework.BundleContext;
import org.osgi.jmx.framework.ServiceStateMBean;
import org.osgi.service.log.LogService;

/**
 * <p>
 * Implementation of <code>MBeanHandler</code> which manages the <code>ServiceState</code>
 * MBean implementation
 * @see MBeanHandler
 * </p>
 *
 * @version $Rev$ $Date$
 */
public class ServiceStateMBeanHandler implements MBeanHandler {

    private JMXAgentContext agentContext;
    private StateConfig stateConfig;
    private String name;
    private StandardMBean mbean;
    private ServiceState serviceStateMBean;
    private BundleContext bundleContext;
    private Logger logger;


    public ServiceStateMBeanHandler(JMXAgentContext agentContext, StateConfig stateConfig) {
        this.agentContext = agentContext;
        this.stateConfig = stateConfig;
        this.bundleContext = agentContext.getBundleContext();
        this.logger = agentContext.getLogger();
        this.name = ObjectNameUtils.createFullObjectName(bundleContext, OBJECTNAME);
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#open()
     */
    public void open() {
        serviceStateMBean = new ServiceState(bundleContext, stateConfig, logger);
        try {
            mbean = new RegistrableStandardEmitterMBean(serviceStateMBean, ServiceStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            logger.log(LogService.LOG_ERROR, "Failed to instantiate MBean for " + ServiceStateMBean.class.getName(), e);
        }
        agentContext.registerMBean(this);
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#getMbean()
     */
    public StandardMBean getMbean() {
        return mbean;
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#close()
     */
    public void close() {
        agentContext.unregisterMBean(this);
       // ensure dispatcher is shutdown even if postDeRegister is not honored
       if (serviceStateMBean != null) {
           serviceStateMBean.shutDownDispatcher();
       }
    }



}

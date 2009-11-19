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

import org.apache.aries.jmx.agent.JMXAgent;
import org.apache.aries.jmx.agent.JMXAgentImpl;
import org.apache.aries.jmx.agent.JMXAgentContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * <p>Activator for JMX OSGi bundle.</p>
 * 
 * @version $Rev$ $Date$
 */
public class Activator implements BundleActivator {

    private JMXAgent agent;
    private Logger logger;

    /**
     * <p>Called when JMX OSGi bundle starts.
     * This method creates and starts JMX agent.</p>
     * 
     * @see org.osgi.framework.BundleActivator#start(BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        logger = new Logger(context);
        //starting logger
        logger.open();
        logger.log(LogService.LOG_DEBUG, "Starting JMX OSGi bundle");
        agent = new JMXAgentImpl(logger);
        JMXAgentContext agentContext = new JMXAgentContext(context, agent, logger);
        agent.setAgentContext(agentContext);
        agent.start();
    }

    /**
     * <p>Called when JMX OSGi bundle stops.
     * This method stops agent and logger @see {@link Logger}.</p>
     * 
     * @see org.osgi.framework.BundleActivator#stop(BundleContext)
     */
    public void stop(BundleContext bc) throws Exception {
        if (logger != null) {
            logger.log(LogService.LOG_DEBUG, "Stopping JMX OSGi bundle");
        }
        if (agent != null) {
            agent.stop();
        }
        if (logger != null) {
            logger.close();
        }
    }

}

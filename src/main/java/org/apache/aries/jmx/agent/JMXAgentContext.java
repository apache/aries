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
package org.apache.aries.jmx.agent;

import java.util.concurrent.ExecutorService;

import javax.management.MBeanServer;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.osgi.framework.BundleContext;

/**
 * <p>This class <tt>JMXAgentContext</tt> represents context of JMXAgent.
 * Delegates registration and unregistration methods to {@link JMXAgent}.</p>
 * @see JMXAgent
 *
 * @version $Rev$ $Date$
 */
public class JMXAgentContext {

    private JMXAgent agent;
    private BundleContext bundleContext;
    private Logger logger;

    /**
     * Constructs new JMXAgentContext.
     * @param bundleContext bundle context @see {@link BundleContext}.
     * @param agent {@link JMXAgent}.
     * @param log logger represents by @see {@link Logger}.
     */
    public JMXAgentContext(BundleContext bundleContext, JMXAgent agent, Logger log) {
        this.bundleContext = bundleContext;
        this.agent = agent;
        this.logger = log;
    }

    /**
     * Delegates invocation to JMX agent.
     * @see org.apache.aries.jmx.agent.JMXAgent#registerMBeans(MBeanServer)
     *
     */
    public void registerMBeans(final MBeanServer server) {
        agent.registerMBeans(server);
    }

    /**
     * Delegates invocation to JMX agent.
     * @see org.apache.aries.jmx.agent.JMXAgent#unregisterMBeans(MBeanServer)
     */
    public void unregisterMBeans(final MBeanServer server) {
        agent.unregisterMBeans(server);
    }

    /**
     * Delegates invocation to JMX agent.
     * @see org.apache.aries.jmx.agent.JMXAgent#registerMBean(MBeanHandler)
     */
    public void registerMBean(final MBeanHandler mbeanData) {
        agent.registerMBean(mbeanData);
    }

    /**
     * Delegates invocation to JMX agent.
     * @see org.apache.aries.jmx.agent.JMXAgent#unregisterMBean(MBeanHandler)
     */
    public void unregisterMBean(final MBeanHandler mBeanHandler) {
        agent.unregisterMBean(mBeanHandler);
    }

    /**
     * Delegates invocation to JMX agent.
     * @see org.apache.aries.jmx.agent.JMXAgent#unregisterMBean(String)
     * @deprecated
     */
    public void unregisterMBean(final String name) {
        agent.unregisterMBean(name);
    }

    /**
     * Gets bundle context.
     * @return bundle context.
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Gets a logger represents by @see {@link Logger}.
     * @return LogService tracker.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Delegates invocation to JMX agent.
     * @see org.apache.aries.jmx.agent.JMXAgent#getRegistrationExecutor()
     */
    public ExecutorService getRegistrationExecutor() {
        return agent.getRegistrationExecutor();
    }
}

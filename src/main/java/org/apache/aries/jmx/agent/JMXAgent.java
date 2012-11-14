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

import org.apache.aries.jmx.MBeanHandler;

/**
 * <p>This <tt>JMXAgent</tt> class represent agent for MBeanServers registered in ServiceRegistry.
 * It's responsible for registration and unregistration MBeans with available MBeanServers.
 * </p>
 *
 * @version $Rev$ $Date$
 */
public interface JMXAgent {

    /**
     * This method starts JMX agent.
     * Creates and starting all MBean Handlers and MBeanServiceTracker.
     */
    void start();

    /**
     * Registers MBeans with provided MBeanServer.
     * @param server MBeanServer with which MBeans are going to be registered
     */
    void registerMBeans(final MBeanServer server);

    /**
     * Unregisters MBeans with provided MBeanServer.
     * @param server MBeanServer with which MBeans are going to be unregistered.
     */
    void unregisterMBeans(final MBeanServer server);

    /**
     * Registers MBean with all available MBeanServers.
     * @param mBeanHandler handler which contains MBean info.
     */
    void registerMBean(final MBeanHandler mBeanHandler);

    /**
     * Unregisters MBean with all available MBeanServers.
     * @param mBeanHandler handler which contains MBean info.
     */
    void unregisterMBean(final MBeanHandler mBeanHandler);

    /**
     * Unregisters MBean with all available MBeanServers.
     * @param name of MBean to be unregistered.
     * @deprecated
     */
    void unregisterMBean(final String name);

    /**
     * Stops JMXAgent.
     * This method stops MBeanServiceTracker and all MBean handlers.
     */
    void stop();

    /**
     * Gets JMXAgentContext @see {@link JMXAgentContext}.
     * @return JMXAgentContext instance.
     */
    JMXAgentContext getAgentContext();

    /**
     * Sets JMXAgentContext for this agent.
     * @param agentContext JMXAgentContext instance created for this agent.
     */
    void setAgentContext(JMXAgentContext agentContext);

    /**
     * Gets registration {@link ExecutorService}.
     * @return registration executor.
     */
    ExecutorService getRegistrationExecutor();

}
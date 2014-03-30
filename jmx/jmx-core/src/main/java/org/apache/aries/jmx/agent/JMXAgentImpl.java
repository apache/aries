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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.aries.jmx.JMXThreadFactory;
import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.MBeanServiceTracker;
import org.apache.aries.jmx.cm.ConfigurationAdminMBeanHandler;
import org.apache.aries.jmx.framework.BundleStateMBeanHandler;
import org.apache.aries.jmx.framework.FrameworkMBeanHandler;
import org.apache.aries.jmx.framework.PackageStateMBeanHandler;
import org.apache.aries.jmx.framework.ServiceStateMBeanHandler;
import org.apache.aries.jmx.framework.wiring.BundleWiringStateMBeanHandler;
import org.apache.aries.jmx.permissionadmin.PermissionAdminMBeanHandler;
import org.apache.aries.jmx.provisioning.ProvisioningServiceMBeanHandler;
import org.apache.aries.jmx.useradmin.UserAdminMBeanHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>
 * Represent agent for MBeanServers registered in ServiceRegistry. Providing registration and unregistration methods.
 * </p>
 *
 * @see JMXAgent
 *
 * @version $Rev$ $Date$
 */
public class JMXAgentImpl implements JMXAgent {

    private ServiceTracker mbeanServiceTracker;
    /**
     * {@link MBeanHandler} store.
     */
    private List<MBeanServer> mbeanServers;
    private Map<MBeanHandler, Boolean> mbeansHandlers;
    private BundleContext context;
    private Logger logger;

    /**
     * Constructs new JMXAgent.
     *
     * @param logger @see org.apache.aries.jmx.Logger
     */
    public JMXAgentImpl(BundleContext context, Logger logger) {
        this.context = context;
        this.logger = logger;
        this.mbeanServers = new ArrayList<MBeanServer>();
        this.mbeansHandlers = new HashMap<MBeanHandler, Boolean>();
    }

    /**
     * @see org.apache.aries.jmx.agent.JMXAgent#start()
     */
    public synchronized void start() {
        logger.log(LogService.LOG_INFO, "Starting JMX OSGi agent");
        // Initialize static handlers
        // Those handlers do not track dependencies
        JMXAgentContext agentContext = new JMXAgentContext(context, this, logger);
        MBeanHandler frameworkHandler = new FrameworkMBeanHandler(agentContext);
        mbeansHandlers.put(frameworkHandler, Boolean.FALSE);
        frameworkHandler.open();
        MBeanHandler bundleStateHandler = new BundleStateMBeanHandler(agentContext);
        mbeansHandlers.put(bundleStateHandler, Boolean.FALSE);
        bundleStateHandler.open();
        MBeanHandler revisionsStateHandler = new BundleWiringStateMBeanHandler(agentContext);
        mbeansHandlers.put(revisionsStateHandler, Boolean.FALSE);
        revisionsStateHandler.open();
        MBeanHandler serviceStateHandler = new ServiceStateMBeanHandler(agentContext);
        mbeansHandlers.put(serviceStateHandler, Boolean.FALSE);
        serviceStateHandler.open();
        MBeanHandler packageStateHandler = new PackageStateMBeanHandler(agentContext);
        mbeansHandlers.put(packageStateHandler, Boolean.FALSE);
        packageStateHandler.open();
        MBeanHandler permissionAdminHandler = new PermissionAdminMBeanHandler(agentContext);
        mbeansHandlers.put(permissionAdminHandler, Boolean.FALSE);
        permissionAdminHandler.open();
        MBeanHandler userAdminHandler = new UserAdminMBeanHandler(agentContext);
        mbeansHandlers.put(userAdminHandler, Boolean.FALSE);
        userAdminHandler.open();
        MBeanHandler configAdminHandler = new ConfigurationAdminMBeanHandler(agentContext);
        mbeansHandlers.put(configAdminHandler, Boolean.FALSE);
        configAdminHandler.open();
        MBeanHandler provServiceHandler = new ProvisioningServiceMBeanHandler(agentContext);
        mbeansHandlers.put(provServiceHandler, Boolean.FALSE);
        provServiceHandler.open();
        // Track mbean servers
        mbeanServiceTracker = new MBeanServiceTracker(agentContext);
        mbeanServiceTracker.open();
    }

    /**
     * @see org.apache.aries.jmx.agent.JMXAgent#registerMBeans(javax.management.MBeanServer)
     */
    public synchronized void registerMBeans(final MBeanServer server) {
        for (MBeanHandler mbeanHandler : mbeansHandlers.keySet()) {
            if (mbeansHandlers.get(mbeanHandler) == Boolean.TRUE) {
                String name = mbeanHandler.getName();
                StandardMBean mbean = mbeanHandler.getMbean();
                if (mbean != null) {
                    try {
                        logger.log(LogService.LOG_INFO, "Registering " + mbean.getMBeanInterface().getName()
                                + " to MBeanServer " + server + " with name " + name);
                        server.registerMBean(mbean, new ObjectName(name));
                    } catch (InstanceAlreadyExistsException e) {
                        logger.log(LogService.LOG_ERROR, "MBean is already registered", e);
                    } catch (MBeanRegistrationException e) {
                        logger.log(LogService.LOG_ERROR, "Can't register MBean", e);
                    } catch (NotCompliantMBeanException e) {
                        logger.log(LogService.LOG_ERROR, "MBean is not compliant MBean", e);
                    } catch (MalformedObjectNameException e) {
                        logger.log(LogService.LOG_ERROR, "Try to register with no valid objectname", e);
                    } catch (NullPointerException e) {
                        logger.log(LogService.LOG_ERROR, "Name of objectname can't be null", e);
                    }
                }
            }
        }
        mbeanServers.add(server);
    }

    /**
     * @see org.apache.aries.jmx.agent.JMXAgent#unregisterMBeans(javax.management.MBeanServer)
     */
    public synchronized void unregisterMBeans(final MBeanServer server) {
        for (MBeanHandler mBeanHandler : mbeansHandlers.keySet()) {
            if (mbeansHandlers.get(mBeanHandler) == Boolean.TRUE) {
                try
                {
                   String name = mBeanHandler.getName();
                   StandardMBean mbean = mBeanHandler.getMbean();
                   if (mbean != null) {
                       logger.log(LogService.LOG_INFO, "Unregistering " + mbean.getMBeanInterface().getName()
                             + " to MBeanServer " + server + " with name " + name);
                       server.unregisterMBean(new ObjectName(name));
                   }
                } catch (MBeanRegistrationException e) {
                   logger.log(LogService.LOG_ERROR, "Can't unregister MBean", e);
                } catch (InstanceNotFoundException e) {
                   logger.log(LogService.LOG_ERROR, "MBean doesn't exist in the repository", e);
                } catch (MalformedObjectNameException e) {
                   logger.log(LogService.LOG_ERROR, "Try to unregister with no valid objectname", e);
                } catch (NullPointerException e) {
                   logger.log(LogService.LOG_ERROR, "Name of objectname can't be null ", e);
                } catch (Exception e) {
                   logger.log(LogService.LOG_ERROR, "Cannot unregister MBean: " + mBeanHandler, e);
                }
            }
        }
        mbeanServers.remove(server);
    }

    /**
     * @see org.apache.aries.jmx.agent.JMXAgent#registerMBean(org.apache.aries.jmx.MBeanHandler)
     */
    public synchronized void registerMBean(final MBeanHandler mBeanHandler) {
        for (MBeanServer server : mbeanServers) {
            String name = mBeanHandler.getName();
            StandardMBean mbean = mBeanHandler.getMbean();
            try {
                logger.log(LogService.LOG_INFO, "Registering " + mbean.getMBeanInterface().getName()
                        + " to MBeanServer " + server + " with name " + name);
                server.registerMBean(mbean, new ObjectName(name));

            } catch (InstanceAlreadyExistsException e) {
                logger.log(LogService.LOG_ERROR, "MBean is already registered", e);
            } catch (MBeanRegistrationException e) {
                logger.log(LogService.LOG_ERROR, "Can't register MBean", e);
            } catch (NotCompliantMBeanException e) {
                logger.log(LogService.LOG_ERROR, "MBean is not compliant MBean, Stopping registration", e);
                return;
            } catch (MalformedObjectNameException e) {
                logger.log(LogService.LOG_ERROR, "Try to register with no valid objectname, Stopping registration", e);
                return;
            } catch (NullPointerException e) {
                logger.log(LogService.LOG_ERROR, "Name of objectname can't be null, Stopping registration", e);
                return;
            }
        }
        mbeansHandlers.put(mBeanHandler, Boolean.TRUE);
    }

    /**
     * @see org.apache.aries.jmx.agent.JMXAgent#unregisterMBean(org.apache.aries.jmx.MBeanHandler)
     */
    public synchronized void unregisterMBean(final MBeanHandler mBeanHandler) {
        for (MBeanServer server : mbeanServers) {
            String name = mBeanHandler.getName();
            try {
                logger.log(LogService.LOG_INFO, "Unregistering mbean " + " to MBeanServer " + server + " with name "
                        + name);
                server.unregisterMBean(new ObjectName(name));
            } catch (MBeanRegistrationException e) {
                logger.log(LogService.LOG_ERROR, "Can't register MBean", e);
            } catch (InstanceNotFoundException e) {
                logger.log(LogService.LOG_ERROR, "MBean doesn't exist in the repository", e);
            } catch (MalformedObjectNameException e) {
                logger.log(LogService.LOG_ERROR, "Try to register with no valid objectname, Stopping registration", e);
                return;
            } catch (NullPointerException e) {
                logger.log(LogService.LOG_ERROR, "Name of objectname can't be null, Stopping registration", e);
                return;
            }
        }
        mbeansHandlers.put(mBeanHandler, Boolean.FALSE);
    }

    /**
     * @see org.apache.aries.jmx.agent.JMXAgent#stop()
     */
    public void stop() {
        logger.log(LogService.LOG_INFO, "Stopping JMX OSGi agent");
        synchronized (this) {
            mbeanServiceTracker.close();
            for (MBeanHandler mBeanHandler : mbeansHandlers.keySet()) {
                mBeanHandler.close();
            }
        }
    }

}

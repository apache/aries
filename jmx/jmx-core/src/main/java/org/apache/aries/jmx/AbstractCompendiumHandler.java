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

import java.util.concurrent.atomic.AtomicLong;

import javax.management.StandardMBean;

import org.apache.aries.jmx.agent.JMXAgentContext;
import org.apache.aries.jmx.util.ObjectNameUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>
 * Abstract implementation of {@link MBeanHandler} that provides a template with basic tracking of an optional
 * compendium service. MBeanHandler implementations that manage a {@link StandardMBean} that is backed by a single OSGi
 * compendium service should extend this class and implement the {@linkplain #constructInjectMBean(Object)} and
 * {@linkplain #getName()} methods
 * </p>
 * 
 * @see MBeanHandler
 * 
 * @version $Rev$ $Date$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractCompendiumHandler extends ServiceTracker implements MBeanHandler {

    protected final JMXAgentContext agentContext;
    protected StandardMBean mbean;
    protected final AtomicLong trackedId = new AtomicLong();
    
    /**
     * 
     * @param agentContext
     * @param filter
     */
    protected AbstractCompendiumHandler(JMXAgentContext agentContext, Filter filter) {
        super(agentContext.getBundleContext(), filter, null);
        this.agentContext = agentContext;
    }

    /**
     * 
     * @param agentContext
     * @param clazz
     */
    protected AbstractCompendiumHandler(JMXAgentContext agentContext, String clazz) {
        super(agentContext.getBundleContext(), clazz, null);
        this.agentContext = agentContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
     */
    public Object addingService(ServiceReference reference) {
        Logger logger = agentContext.getLogger();
        Object trackedService = null;
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        //API stipulates versions for compendium services with static ObjectName
        //This shouldn't happen but added as a consistency check
        if (trackedId.compareAndSet(0, serviceId)) {
            logger.log(LogService.LOG_INFO, "Registering MBean with ObjectName [" + getName() + "] for service with "
                    + Constants.SERVICE_ID + " [" + serviceId + "]");
            trackedService = context.getService(reference);
            mbean = constructInjectMBean(trackedService);
            agentContext.registerMBean(AbstractCompendiumHandler.this);
        } else {
            String serviceDescription = getServiceDescription(reference);
            logger.log(LogService.LOG_WARNING, "Detected secondary ServiceReference for [" + serviceDescription
                    + "] with " + Constants.SERVICE_ID + " [" + serviceId + "] Only 1 instance will be JMX managed");
        }
        return trackedService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference reference, Object service) {
        Logger logger = agentContext.getLogger();
        long serviceID = (Long) reference.getProperty(Constants.SERVICE_ID);
        if (trackedId.compareAndSet(serviceID, 0)) {
            logger.log(LogService.LOG_INFO, "Unregistering MBean with ObjectName [" + getName() + "] for service with "
                    + Constants.SERVICE_ID + " [" + serviceID + "]"); 
            agentContext.unregisterMBean(AbstractCompendiumHandler.this);
            context.ungetService(reference);
        } else {
            String serviceDescription = getServiceDescription(reference);
            logger.log(LogService.LOG_WARNING, "ServiceReference for [" + serviceDescription + "] with "
                    + Constants.SERVICE_ID + " [" + serviceID + "] is not currently JMX managed");
        }
    }

    private String getServiceDescription(ServiceReference reference) {
        String serviceDescription = (String) reference.getProperty(Constants.SERVICE_DESCRIPTION);
        if (serviceDescription == null) {
            Object obj = reference.getProperty(Constants.OBJECTCLASS);
            if (obj instanceof String[]) {
                StringBuilder sb = new StringBuilder();
                for (String s : (String[]) obj) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(s);
                }
                serviceDescription = sb.toString();
            } else {
                serviceDescription = obj.toString();
            }
        }
        return serviceDescription;
    }

    /**
     * Gets the <code>StandardMBean</code> managed by this handler when the backing service is available or null
     * 
     * @see org.apache.aries.jmx.MBeanHandler#getMbean()
     */
    public StandardMBean getMbean() {
        return mbean;
    }

    /**
     * Implement this method to construct an appropriate {@link StandardMBean} instance which is backed by the supplied
     * service tracked by this handler
     * 
     * @param targetService
     *            the compendium service tracked by this handler
     * @return The <code>StandardMBean</code> instance whose registration lifecycle will be managed by this handler
     */
    protected abstract StandardMBean constructInjectMBean(Object targetService);

    /**
     * The base name of the MBean. Will be expanded with the framework name and the UUID.
     * @return
     */
    protected abstract String getBaseName();

    /**
     * @see org.apache.aries.jmx.MBeanHandler#getName()
     */
    public String getName() {
        return ObjectNameUtils.createFullObjectName(context, getBaseName());
    }
}

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

import static org.osgi.jmx.framework.BundleStateMBean.OBJECTNAME;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.util.ObjectNameUtils;
import org.apache.aries.jmx.util.shared.RegistrableStandardEmitterMBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * <p>
 * Implementation of <code>MBeanHandler</code> which manages the <code>BundleState</code>
 * MBean implementation
 * @see MBeanHandler
 * </p>
 *
 * @version $Rev$ $Date$
 */
public class BundleStateMBeanHandler implements MBeanHandler {

    private Logger logger;
    private String name;
    private StandardMBean mbean;
    private BundleState bundleStateMBean;
    private BundleContext bundleContext;
    private ServiceReference packageAdminRef;
    private ServiceReference startLevelRef;


    public BundleStateMBeanHandler(BundleContext bundleContext, Logger logger) {
        this.bundleContext = bundleContext;
        this.logger = logger;
        this.name = ObjectNameUtils.createFullObjectName(bundleContext, OBJECTNAME);
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#open()
     */
    public void open() {
        packageAdminRef = bundleContext.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminRef);
        startLevelRef = bundleContext.getServiceReference(StartLevel.class.getName());
        StartLevel startLevel = (StartLevel) bundleContext.getService(startLevelRef);
        bundleStateMBean = new BundleState(bundleContext, packageAdmin, startLevel, logger);
        try {
            mbean = new RegistrableStandardEmitterMBean(bundleStateMBean, BundleStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            logger.log(LogService.LOG_ERROR, "Failed to instantiate MBean for " + BundleStateMBean.class.getName(), e);
        }
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
        if (packageAdminRef != null) {
            try {
                bundleContext.ungetService(packageAdminRef);
            } catch (RuntimeException e) {
                logger.log(LogService.LOG_WARNING, "Exception occured during cleanup", e);
            }
            packageAdminRef = null;
        }
        if (startLevelRef != null) {
            try {
                bundleContext.ungetService(startLevelRef);
            } catch (RuntimeException e) {
                logger.log(LogService.LOG_WARNING, "Exception occured during cleanup", e);
            }
            startLevelRef = null;
        }
        // ensure dispatcher is shutdown even if postDeRegister is not honored
        if (bundleStateMBean != null) {
            bundleStateMBean.shutDownDispatcher();
        }
    }





}

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

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
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
public class PermissionAdminMBeanHandler implements MBeanHandler {

    private String name;
    private StandardMBean mbean;
    private BundleContext context;
    private Logger logger;

    /**
     * Constructs new PermissionAdminMBeanHandler.
     * 
     * @param context
     *            bundle context of JMX bundle.
     * @param logger
     *            @see {@link Logger}.
     */
    public PermissionAdminMBeanHandler(BundleContext context, Logger logger) {
        this.context = context;
        this.name = PermissionAdminMBean.OBJECTNAME;
        this.logger = logger;
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#getMbean()
     */
    public StandardMBean getMbean() {
        return mbean;
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#open()
     */
    public void open() {
        ServiceReference adminRef = context.getServiceReference(org.osgi.service.permissionadmin.PermissionAdmin.class
                .getCanonicalName());
        org.osgi.service.permissionadmin.PermissionAdmin permissionAdmin = (org.osgi.service.permissionadmin.PermissionAdmin) context
                .getService(adminRef);
        PermissionAdminMBean paMBean = new PermissionAdmin(permissionAdmin);
        try {
            mbean = new StandardMBean(paMBean, PermissionAdminMBean.class);
        } catch (NotCompliantMBeanException e) {
            logger.log(LogService.LOG_ERROR, "Not compliant MBean", e);
        }
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#close()
     */
    public void close() {
        // not used
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#getName()
     */
    public String getName() {
        return name;
    }
}
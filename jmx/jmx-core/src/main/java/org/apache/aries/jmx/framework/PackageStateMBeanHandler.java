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

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.MBeanHandler;
import org.apache.aries.jmx.util.ObjectNameUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.framework.PackageStateMBean;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * <p>
 * <tt>PackageStateMBeanHandler</tt> represents MBeanHandler which
 * holding information about {@link PackageStateMBean}.</p>
 *
 * @see MBeanHandler
 *
 * @version $Rev$ $Date$
 */
public class PackageStateMBeanHandler implements MBeanHandler {

    private String name;
    private StandardMBean mbean;
    private BundleContext context;
    private Logger logger;

    /**
     * Constructs new PackageStateMBeanHandler.
S     *
     * @param context bundle context of JMX bundle.
     * @param logger @see {@link Logger}.
     */
    public PackageStateMBeanHandler(BundleContext context, Logger logger) {
        this.context = context;
        this.name = ObjectNameUtils.createFullObjectName(context, PackageStateMBean.OBJECTNAME);
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
        ServiceReference adminRef = context.getServiceReference(PackageAdmin.class.getCanonicalName());
        PackageAdmin packageAdmin = (PackageAdmin) context.getService(adminRef);
        PackageStateMBean packageState = new PackageState(context, packageAdmin);
        try {
            mbean = new StandardMBean(packageState, PackageStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            logger.log(LogService.LOG_ERROR, "Not compliant MBean", e);
        }
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#close()
     */
    public void close() {
        //not used
    }

    /**
     * @see org.apache.aries.jmx.MBeanHandler#getName()
     */
    public String getName() {
        return name;
    }


}

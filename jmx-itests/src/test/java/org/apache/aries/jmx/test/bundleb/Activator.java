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
package org.apache.aries.jmx.test.bundleb;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.jmx.test.bundleb.api.InterfaceB;
import org.apache.aries.jmx.test.bundleb.api.MSF;
import org.apache.aries.jmx.test.bundleb.impl.B;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class Activator implements BundleActivator {

    ServiceRegistration plainRegistration;
    ServiceRegistration msfRegistration;
    
    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.aries.jmx.test.ServiceB");
        plainRegistration = context.registerService(InterfaceB.class.getName(), new B(), props);
        Dictionary<String, Object> fprops = new Hashtable<String, Object>();
        fprops.put(Constants.SERVICE_PID, "jmx.test.B.factory");
        msfRegistration = context.registerService(ManagedServiceFactory.class.getName(), new MSF(), fprops);
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plainRegistration.unregister();
        msfRegistration.unregister();
    }

}

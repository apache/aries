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
package org.apache.aries.jmx.test.bundlea;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.jmx.test.bundlea.api.InterfaceA;
import org.apache.aries.jmx.test.bundlea.impl.A;
import org.apache.aries.jmx.test.bundleb.api.InterfaceB;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class Activator implements BundleActivator {

    ServiceTracker tracker;
    
    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        
        tracker = new ServiceTracker(context, InterfaceB.class.getName(), null);
        tracker.open();
        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.aries.jmx.test.ServiceA");
        String[] interfaces = new String[] { InterfaceA.class.getName(), ManagedService.class.getName() };
		context.registerService(interfaces, new A(tracker), props);
        
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }

}

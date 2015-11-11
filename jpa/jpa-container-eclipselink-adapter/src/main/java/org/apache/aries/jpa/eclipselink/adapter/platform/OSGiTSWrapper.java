/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.eclipselink.adapter.platform;

import javax.transaction.TransactionManager;

import org.eclipse.persistence.transaction.JTATransactionController;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

@SuppressWarnings({"rawtypes", "unchecked"})
public class OSGiTSWrapper extends JTATransactionController {

    @Override
    protected TransactionManager acquireTransactionManager() throws Exception {
        BundleContext ctx = FrameworkUtil.getBundle(OSGiTSWrapper.class).getBundleContext();
        
        if (ctx != null) {
            ServiceReference ref = ctx.getServiceReference(TransactionManager.class.getName());
            
            if (ref != null) {
                return (TransactionManager) ctx.getService(ref);
            }            
        }
        
        return super.acquireTransactionManager();
    }
    
}

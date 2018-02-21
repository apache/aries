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

package org.apache.aries.jndi.url;

import org.apache.aries.jndi.spi.AugmenterInvoker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

public class BlueprintURLContextFactory implements ObjectFactory {

    private static AugmenterInvoker augmenterInvoker = null;
    final private Bundle _callersBundle;

    public BlueprintURLContextFactory(Bundle callersBundle) {
        _callersBundle = callersBundle;
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context callersCtx, Hashtable<?, ?> envmt) throws Exception {

        if (augmenterInvoker == null && _callersBundle != null) {
            BundleContext callerBundleContext = _callersBundle.getBundleContext();
            ServiceReference<?> augmenterSR = callerBundleContext.getServiceReference(AugmenterInvoker.class.getName());
            if (augmenterSR != null) augmenterInvoker = (AugmenterInvoker) callerBundleContext.getService(augmenterSR);
        }
        if (augmenterInvoker != null) augmenterInvoker.augmentEnvironment(envmt);

        BundleContext bc = (BundleContext) envmt.get(JNDIConstants.BUNDLE_CONTEXT);
        if (augmenterInvoker != null) augmenterInvoker.unaugmentEnvironment(envmt);

        Bundle b = (bc != null) ? bc.getBundle() : null;
        Object result = null;
        if (obj == null) {
            result = new BlueprintURLContext((b == null) ? _callersBundle : b, envmt);
        } else if (obj instanceof String) {
            Context ctx = new BlueprintURLContext((b == null) ? _callersBundle : b, envmt);
            try {
                result = ctx.lookup((String) obj);
            } finally {
                ctx.close();
            }
        }
        return result;
    }

}

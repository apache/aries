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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jndi.url;

import org.apache.aries.proxy.ProxyManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIConstants;

import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Activator implements BundleActivator {
    private static SingleServiceTracker<ProxyManager> proxyManager;
    private BundleContext ctx;
    private volatile ServiceRegistration<?> osgiUrlReg = null;
    private volatile ServiceRegistration<?> blueprintUrlReg = null;

    public static ProxyManager getProxyManager() {
        return proxyManager == null ? null : proxyManager.getService();
    }

    @Override
    public void start(BundleContext context) throws InvalidSyntaxException {
        ctx = context;
        proxyManager = new SingleServiceTracker<>(context, ProxyManager.class, this::serviceChanged);
        proxyManager.open();
        // Blueprint URL scheme requires access to the BlueprintContainer service.
        // We have an optional import
        // on org.osgi.service.blueprint.container: only register the blueprint:comp/URL
        // scheme if it's present
        try {
            ctx.getBundle().loadClass("org.osgi.service.blueprint.container.BlueprintContainer");
            Hashtable<String, Object> blueprintURlSchemeProps = new Hashtable<>();
            blueprintURlSchemeProps.put(JNDIConstants.JNDI_URLSCHEME, new String[]{"blueprint"});
            blueprintUrlReg = ctx.registerService(ObjectFactory.class.getName(),
                    new BlueprintURLContextServiceFactory(), blueprintURlSchemeProps);
        } catch (ClassNotFoundException cnfe) {
            // The blueprint packages aren't available, so do nothing. That's fine.
            Logger logger = Logger.getLogger("org.apache.aries.jndi");
            logger.log(Level.INFO, "Blueprint support disabled: " + cnfe);
            logger.log(Level.FINE, "Blueprint support disabled", cnfe);
        }
    }

    @Override
    public void stop(BundleContext context) {
        proxyManager.close();
        safeUnregisterService(osgiUrlReg);
        safeUnregisterService(blueprintUrlReg);
    }

    void serviceChanged(ProxyManager oldPm, ProxyManager newPm) {
        if (newPm == null) {
            safeUnregisterService(osgiUrlReg);
            osgiUrlReg = null;
        } else {
            Hashtable<String, Object> osgiUrlprops = new Hashtable<>();
            osgiUrlprops.put(JNDIConstants.JNDI_URLSCHEME, new String[]{"osgi", "aries"});
            osgiUrlReg = ctx.registerService(ObjectFactory.class.getName(),
                    new OsgiURLContextServiceFactory(), osgiUrlprops);
        }
    }

    private static void safeUnregisterService(ServiceRegistration<?> reg) {
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                //This can be safely ignored
            }
        }
    }

}
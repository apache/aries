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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Configuration for {@link BundleState} and {@link ServiceState}.
 *
 * @version $Rev$ $Date$
 */
public class StateConfig implements ManagedService {

    private static final String PID = StateConfig.class.getName();

    private static final String ATTRIBUTE_CHANGE_NOTIFICATION_ENABLED = "attributeChangeNotificationEnabled";
    private static final boolean DEFAULT_ATTRIBUTE_CHANGE_NOTIFICATION_ENABLED = true;

    private static final String SERVICE_CHANGE_NOTIFICATION_ENABLED = "serviceChangeNotificationEnabled";
    private static final boolean DEFAULT_SERVICE_CHANGE_NOTIFICATION_ENABLED = true;

    private static final String BUNDLE_CHANGE_NOTIFICATION_ENABLED = "bundleChangeNotificationEnabled";
    private static final boolean DEFAULT_BUNDLE_CHANGE_NOTIFICATION_ENABLED = true;

    private volatile boolean attributeChangeNotificationEnabled = DEFAULT_ATTRIBUTE_CHANGE_NOTIFICATION_ENABLED;
    private volatile boolean serviceChangeNotificationEnabled = DEFAULT_SERVICE_CHANGE_NOTIFICATION_ENABLED;
    private volatile boolean bundleChangeNotificationEnabled = DEFAULT_BUNDLE_CHANGE_NOTIFICATION_ENABLED;

    void setAttributeChangeNotificationEnabled(boolean attributeChangeNotificationEnabled) {
        this.attributeChangeNotificationEnabled = attributeChangeNotificationEnabled;
    }

    void setServiceChangeNotificationEnabled(boolean serviceChangeNotificationEnabled) {
        this.serviceChangeNotificationEnabled = serviceChangeNotificationEnabled;
    }

    void setBundleChangeNotificationEnabled(boolean bundleChangeNotificationEnabled) {
        this.bundleChangeNotificationEnabled = bundleChangeNotificationEnabled;
    }

    /**
     * Registers this service and returns an instance.
     *
     * @param context the bundle context
     * @return the service instance
     * @throws IOException
     */
    public static StateConfig register(BundleContext context) throws IOException {
        Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put("service.pid", PID);

        StateConfig stateConfig = new StateConfig();
        context.registerService(ManagedService.class, stateConfig, serviceProps);
        return stateConfig;
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        attributeChangeNotificationEnabled = getBoolean(dictionary, ATTRIBUTE_CHANGE_NOTIFICATION_ENABLED,
                DEFAULT_ATTRIBUTE_CHANGE_NOTIFICATION_ENABLED);
        serviceChangeNotificationEnabled = getBoolean(dictionary, SERVICE_CHANGE_NOTIFICATION_ENABLED,
                DEFAULT_SERVICE_CHANGE_NOTIFICATION_ENABLED);
        bundleChangeNotificationEnabled = getBoolean(dictionary, BUNDLE_CHANGE_NOTIFICATION_ENABLED,
                DEFAULT_BUNDLE_CHANGE_NOTIFICATION_ENABLED);
    }

    /**
     * Whether or not JMX attribute change notifications should be triggered when attributes change.
     *
     * @return <code>true</code> if attribute change notifications are enabled
     */
    public boolean isAttributeChangeNotificationEnabled() {
        return attributeChangeNotificationEnabled;
    }

    /**
     * Whether or not JMX OSGi service change notifications should be triggered when OSGi service change.
     *
     * @return <code>true</code> if OSGi service change notifications are enabled
     */
    public boolean isServiceChangeNotificationEnabled() {
        return serviceChangeNotificationEnabled;
    }

    /**
     * Whether or not JMX bundle change notifications should be triggered when bundle change.
     *
     * @return <code>true</code> if bundle change notifications are enabled
     */
    public boolean isBundleChangeNotificationEnabled() {
        return bundleChangeNotificationEnabled;
    }

    private static boolean getBoolean(Dictionary<String, ?> dictionary, String propertyName, boolean defaultValue) {
        Object object = (dictionary != null) ? dictionary.get(propertyName) : null;
        if (object == null) {
            return defaultValue;
        } else if (object instanceof Boolean) {
            return (Boolean) object;
        } else {
            String string = object.toString();
            return !string.isEmpty() ? Boolean.parseBoolean(string) : defaultValue;
        }
    }

}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.subsystem.ContentHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigAdminContentHandler implements ContentHandler {
    public static final String FELIXCM_CONTENT_TYPE = "felix.cm.config";
    public static final String PROPERTIES_CONTENT_TYPE = "osgi.config.properties";
    public static final String[] CONTENT_TYPES = {PROPERTIES_CONTENT_TYPE, FELIXCM_CONTENT_TYPE};

    private final ServiceTracker<ConfigurationAdmin,ConfigurationAdmin> cmTracker;
    private Map<String, Dictionary<String, Object>> configurations = new ConcurrentHashMap<String, Dictionary<String, Object>>();

    public ConfigAdminContentHandler(BundleContext ctx) {
        cmTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
                ctx, ConfigurationAdmin.class, null);
        cmTracker.open();
    }

    public void shutDown() {
        cmTracker.close();
    }

    @Override @SuppressWarnings({ "unchecked", "rawtypes" })
    public void install(InputStream is, String symbolicName, String contentType, Subsystem subsystem, Coordination coordination) {
        Dictionary configuration = null;
        try {
            if (PROPERTIES_CONTENT_TYPE.equals(contentType)) {
                Properties p = new Properties();
                p.load(is);
                configuration = p;
            } else if (FELIXCM_CONTENT_TYPE.equals(contentType)) {
                configuration = ConfigurationHandler.read(is);
            }
        } catch (IOException e) {
            coordination.fail(new Exception("Problem loading configuration " +
                    symbolicName + " for subsystem " + subsystem.getSymbolicName(), e));
            return;
        } finally {
            try { is.close(); } catch (IOException ioe) {}
        }

        if (configuration != null) {
            configurations.put(symbolicName, configuration);
        }
    }

    @Override
    public void start(String symbolicName, String contentType, Subsystem subsystem, Coordination coordination) {
        Dictionary<String, Object> configuration = configurations.get(symbolicName);
        if (configuration == null) {
            coordination.fail(new Exception("Cannot start configuration " + symbolicName + " for subsystem " + subsystem.getSymbolicName() +
                    " it was not previously loaded"));
            return;
        }

        try {
            ConfigurationAdmin cm = cmTracker.getService();
            if (cm == null) {
                coordination.fail(new Exception("No Configuration Admin Service found. Cannot apply configuration " +
                        symbolicName + " to subsystem " + subsystem.getSymbolicName()));
                return;
            }
            Configuration conf = cm.getConfiguration(symbolicName, null);
            conf.update(configuration);

            // Update has happened, we can forget the configuration data now
            configurations.remove(symbolicName);
        } catch (IOException e) {
            coordination.fail(new Exception("Problem applying configuration " + symbolicName + " in subsystem " +
                    subsystem.getSymbolicName(), e));
        }
    }

    @Override
    public void stop(String symbolicName, String contentType, Subsystem subsystem) {
        // We don't remove the configuration on stop, as this is generally not desired.
        // Specifically, other changes may have been made to the configuration that we
        // don't want to wipe out.
    }

    @Override
    public void uninstall(String symbolicName, String contentType, Subsystem subsystem) {
        // Nothing to uninstall
    }
}

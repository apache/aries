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
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

public class ConfigAdminPropsFileContentHandlerTest extends SubsystemTest {
    public ConfigAdminPropsFileContentHandlerTest() {
        installConfigAdmin = true;
    }

    @Override
    protected void createApplications() throws Exception {
        createApplication("cmContent", "org.foo.Bar.cfg", "com.blah.Blah.cfg",
                "cmContentBundleZ.jar");
    }

    @Test
    public void testConfigurationContentHandler() throws Exception {
        // This test works as follows: it first installs a subsystem (cmContent.esa)
        // that contains two configuration files (org.foo.Bar.cfg and com.blah.Blah.cfg)
        // These configuration files are marked as 'osgi.config' content type.
        // The ConfigAdminContentHandler handles the installation of this content
        // and registers them as configuration with the Config Admin Service.
        // The .esa file also contains an ordinary bundle that registers two
        // Config Admin ManagedServices. Each registerd under one of the PIDs.
        // Once they receive the expected configuration they each register a String
        // service to mark that they have.
        // After starting the subsystem this test waits for these 'String' services
        // to appear so that it knows that the whole process worked.

        Subsystem subsystem = installSubsystemFromFile("cmContent.esa");
        subsystem.start();

        // Now check that both Managed Services (Config Admin services) have been configured
        // If they are configured correctly they will register a marker String service to
        // indicate this.

        Filter f = bundleContext.createFilter(
                "(&(objectClass=java.lang.String)(test.pid=org.foo.Bar))");
        ServiceTracker<String, String> barTracker =
                new ServiceTracker<String, String>(bundleContext, f, null);
        try {
            barTracker.open();
            String blahSvc = barTracker.waitForService(2000);
            assertEquals("Bar!", blahSvc);
        } finally {
            barTracker.close();
        }

        Filter f2 = bundleContext.createFilter(
                "(&(objectClass=java.lang.String)(test.pid=com.blah.Blah))");
        ServiceTracker<String, String> blahTracker =
                new ServiceTracker<String, String>(bundleContext, f2, null);
        try {
            blahTracker.open();
            String blahSvc = blahTracker.waitForService(2000);
            assertEquals("Blah!", blahSvc);
        } finally {
            blahTracker.close();
        }

        stopAndUninstallSubsystemSilently(subsystem);
    }

    @Test
    public void testAries1352() throws Exception {
        // Same test than testConfigurationContentHandler, but an existing
        // configuration exists before the subsystem is installed.
        // The configuration should not be overwritten by the subsystem
        // installation.

        ConfigurationAdmin cm = bundleContext.getService(
                bundleContext.getServiceReference(ConfigurationAdmin.class));

        Configuration blahConf = cm.getConfiguration("com.blah.Blah", "?");
        Dictionary<String, Object> blahProps = new Hashtable<String, Object>(1);
        blahProps.put("configVal", "Hello");
        blahConf.update(blahProps);

        Subsystem subsystem = installSubsystemFromFile("cmContent.esa");
        subsystem.start();

        // No configuration exists for the service Bar: configuration
        // values are loaded by the subsystem.
        Filter f = bundleContext.createFilter(
                "(&(objectClass=java.lang.String)(test.pid=org.foo.Bar))");
        ServiceTracker<String, String> barTracker =
                new ServiceTracker<String, String>(bundleContext, f, null);
        try {
            barTracker.open();
            String blahSvc = barTracker.waitForService(2000);
            assertEquals("Bar!", blahSvc);
        } finally {
            barTracker.close();
        }

        // A configuration exists for Blah: the subsystem installation should
        // not overwrite it.
        Filter f2 = bundleContext.createFilter(
                "(&(objectClass=java.lang.String)(test.pid=com.blah.Blah))");
        ServiceTracker<String, String> blahTracker =
                new ServiceTracker<String, String>(bundleContext, f2, null);
        try {
            blahTracker.open();
            String blahSvc = blahTracker.waitForService(2000);
            assertEquals("Hello", blahSvc);
        } finally {
            blahTracker.close();
        }

        stopAndUninstallSubsystemSilently(subsystem);
        blahConf.delete();
    }
}

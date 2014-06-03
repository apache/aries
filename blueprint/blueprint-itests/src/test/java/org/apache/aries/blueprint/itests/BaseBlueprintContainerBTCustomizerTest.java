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
package org.apache.aries.blueprint.itests;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.itest.RichBundleContext;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public abstract class BaseBlueprintContainerBTCustomizerTest extends AbstractBlueprintIntegrationTest 
{
    protected Map<String, String> getCompositeManifest() {
        Map<String, String> compositeManifest = new HashMap<String, String>();
        compositeManifest.put(Constants.BUNDLE_SYMBOLICNAME, "test-composite");
        compositeManifest.put(Constants.BUNDLE_VERSION, "1.0.0");
        // this import-package is used by the blueprint.sample
        compositeManifest.put(Constants.IMPORT_PACKAGE, "org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\", org.osgi.service.blueprint.container;version=1.0, org.osgi.service.cm");
        // this export-package is used by pax junit runner as it needs to see the blueprint sample package 
        // for the test after the blueprint sample is started.
        compositeManifest.put(Constants.EXPORT_PACKAGE, "org.apache.aries.blueprint.sample");
        
        return compositeManifest;
    }
    
    protected Bundle installConfigurationAdmin(BundleContext ctx) throws Exception {
        
        Bundle configAdminBundle = null;
        // make sure we don't have a config admin already present
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ServiceTracker tracker = new ServiceTracker(ctx, ctx.createFilter("(" + Constants.OBJECTCLASS + "=" + ConfigurationAdmin.class.getName() + ")"), null);
        tracker.open();
            Object cfgAdminService = tracker.waitForService(5000);
        tracker.close();
        
        if (cfgAdminService == null) {
            MavenArtifactProvisionOption cfgAdminOption = CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject();
            InputStream cfgAdminStream = new URL(cfgAdminOption.getURL()).openStream();
            
            configAdminBundle = ctx.installBundle(cfgAdminOption.getURL(), cfgAdminStream);            
        }

        return configAdminBundle;
    }
    
    protected void applyCommonConfiguration(BundleContext ctx) throws Exception {
        ConfigurationAdmin ca = (new RichBundleContext(ctx)).getService(ConfigurationAdmin.class);        
        Configuration cf = ca.getConfiguration("blueprint-sample-placeholder", null);
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("key.b", "10");
        cf.update(props);
    }
    
    protected Bundle installTestBundle(BundleContext compositeBundleContext) throws IOException, MalformedURLException, BundleException {
        // install the blueprint sample onto the framework associated with the composite bundle
        MavenArtifactProvisionOption mapo = CoreOptions.mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").versionAsInProject();
        // let's use input stream to avoid invoking mvn url handler which isn't avail in the child framework.
        InputStream is = new URL(mapo.getURL()).openStream();
        return compositeBundleContext.installBundle(mapo.getURL(), is);
    }
}

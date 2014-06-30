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
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;

@SuppressWarnings("deprecation")
public abstract class BaseBlueprintContainerBTCustomizerTest extends AbstractBlueprintIntegrationTest 
{
	@Inject
	CompositeBundleFactory cbf;

	/**
	 * Make sure to adapt the imports and exports to the imports and exports needed by the sample
	 * bundle. If they are wrong then there might be an error like:
	 * composite bundle could not be resolved: bundle was disabled:null
	 * @return
	 */
    protected Map<String, String> getCompositeManifest() {
        Map<String, String> compositeManifest = new HashMap<String, String>();
        compositeManifest.put(Constants.BUNDLE_SYMBOLICNAME, "test-composite");
        compositeManifest.put(Constants.BUNDLE_VERSION, "1.0.0");
        // this import-package is used by the blueprint.sample
        compositeManifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework;version=\"[1.6,2)\","
        		+ "org.osgi.service.cm,"
        		+ "org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\","
        		+ "org.osgi.service.blueprint.container;version=\"[1.0,2)\"");
        // this export-package is used by pax junit runner as it needs to see the blueprint sample package 
        // for the test after the blueprint sample is started.
        compositeManifest.put(Constants.EXPORT_PACKAGE, "org.apache.aries.blueprint.sample;version=\"1.0.1\"");
        
        return compositeManifest;
    }
    
    protected MavenArtifactProvisionOption configAdminOption() {
		return CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject();
	}
    
    protected Bundle installBundle(BundleContext bundleContext, String url) throws IOException, MalformedURLException, BundleException {
        // let's use input stream to avoid invoking mvn url handler which isn't avail in the child framework.
        InputStream is = new URL(url).openStream();
        Bundle bundle = bundleContext.installBundle(url, is);
        Assert.assertNotNull(bundle);
        return bundle;
    }

	protected CompositeBundle createCompositeBundle() throws BundleException {
		Map<String, String> frameworkConfig = new HashMap<String, String>();
	    Map<String, String> compositeManifest = getCompositeManifest();
	    CompositeBundle cb = cbf.installCompositeBundle(frameworkConfig, "test-composite", compositeManifest);
		return cb;
	}
}

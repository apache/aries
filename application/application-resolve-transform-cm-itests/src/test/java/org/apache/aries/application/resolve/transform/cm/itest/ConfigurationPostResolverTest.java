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
package org.apache.aries.application.resolve.transform.cm.itest;

import static org.ops4j.pax.exam.CoreOptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.spi.resolve.PostResolveTransformer;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ExportedBundle;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.Provider;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.itest.RichBundleContext;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class ConfigurationPostResolverTest extends AbstractIntegrationTest {

    /**
     * This test validates that the transformer is correctly detecting the config admin package. Checks
     * are performed to validate that an existing import package is still honored etc.
     *
     * @throws Exception
     */
    @Test
    public void validatePostResolveTransform() throws Exception {

        RichBundleContext ctx = new RichBundleContext(bundleContext);
        PostResolveTransformer transformer = ctx.getService(PostResolveTransformer.class);
        Assert.assertNotNull("Unable to locate transformer", transformer);

        /**
         * Try processing deployed content that doesn't have any import for the
         * org.osgi.service.cm package, the resultant imports should be unaffected.
         */
        ApplicationMetadata mockApplicationMetadata = Skeleton.newMock(ApplicationMetadata.class);
        MockDeployedBundles originalDeployedBundles = new MockDeployedBundles();
        originalDeployedBundles.setDeployedContent(getNonConfigModelledResources());
        DeployedBundles transformedDeployedBundles = transformer.postResolveProcess(mockApplicationMetadata, originalDeployedBundles);
        Assert.assertNotNull("An instance should have been returned", transformedDeployedBundles);
        Assert.assertEquals(originalDeployedBundles.getImportPackage(), transformedDeployedBundles.getImportPackage());

        /**
         * Now try processing a deployed bundles instances that has an import for the org.osgi.service.cm package in multiple
         * modelled resources with an empty import package set in the mock deployed bundles instance.
         */
        originalDeployedBundles = new MockDeployedBundles();
        originalDeployedBundles.setDeployedContent(getConfigModelledResources());
        transformedDeployedBundles = transformer.postResolveProcess(mockApplicationMetadata, originalDeployedBundles);
        Assert.assertNotNull("An instance should have been returned", transformedDeployedBundles);
        Assert.assertNotSame("Missing config package", originalDeployedBundles.getImportPackage(), transformedDeployedBundles.getImportPackage());
        Assert.assertEquals("Missing config package", "org.osgi.service.cm;version=\"1.2.0\"", transformedDeployedBundles.getImportPackage());

        /**
         * Now try processing a deployed bundles instances that has an import for the org.osgi.service.cm package in multiple
         * modelled resources with a populated import package set in the mock deployed bundles instance.
         */
        originalDeployedBundles = new MockDeployedBundles();
        originalDeployedBundles.setDeployedContent(getConfigModelledResources());
        originalDeployedBundles.setImportPackage("org.foo.bar;version=\1.0.0\",org.bar.foo;version=\"1.0.0\"");
        transformedDeployedBundles = transformer.postResolveProcess(mockApplicationMetadata, originalDeployedBundles);
        Assert.assertNotNull("An instance should have been returned", transformedDeployedBundles);
        Assert.assertNotSame("Missing config package", originalDeployedBundles.getImportPackage(), transformedDeployedBundles.getImportPackage());
        Assert.assertEquals("Missing config package", "org.foo.bar;version=\1.0.0\",org.bar.foo;version=\"1.0.0\",org.osgi.service.cm;version=\"1.2.0\"", transformedDeployedBundles.getImportPackage());
    }

    private static Collection<ModelledResource> getNonConfigModelledResources() {
        Collection<ModelledResource> modelledResources = new ArrayList<ModelledResource>();
        MockModelledResource ms1 = new MockModelledResource();
        ms1.setImportedPackages(Arrays.asList(new MockImportedPackage("org.foo.bar", "1.0.0"), new MockImportedPackage("org.bar.foo", "1.0.0")));

        return modelledResources;
    }

    private static Collection<ModelledResource> getConfigModelledResources() {
        Collection<ModelledResource> resources = getNonConfigModelledResources();
        MockModelledResource mmr1 = new MockModelledResource();
        mmr1.setImportedPackages(Arrays.asList(new MockImportedPackage("org.osgi.service.cm", "1.2.0")));
        resources.add(mmr1);
        MockModelledResource mmr2 = new MockModelledResource();
        mmr2.setImportedPackages(Arrays.asList(new MockImportedPackage("org.osgi.service.cm", "1.2.0")));
        resources.add(mmr2);
        return resources;
    }

    /**
     * Create the configuration for the PAX container
     *
     * @return the various required options
     * @throws Exception
     */
    @Configuration
    public static Option[] configuration() throws Exception {
        return options(
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                junitBundles(),
                mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.osgi", "org.osgi.compendium").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.api").versionAsInProject(),
                mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolve.transform.cm").versionAsInProject()
        );
    }

    private static class MockDeployedBundles implements DeployedBundles {
        private Collection<ModelledResource> deployedContent;
        private String importPackage;

        public void addBundle(ModelledResource arg0) {
        }

        public String getContent() {
            return null;
        }

        public Collection<ModelledResource> getDeployedContent() {
            return deployedContent;
        }

        public void setDeployedContent(Collection<ModelledResource> deployedContent) {
            this.deployedContent = deployedContent;
        }

        public String getDeployedImportService() {
            return null;
        }

        public Collection<ModelledResource> getDeployedProvisionBundle() {
            return null;
        }

        public Map<String, String> getExtraHeaders() {
            return null;
        }

        public void setImportPackage(String importPackage) {
            this.importPackage = importPackage;
        }

        /**
         * Used to reflect external packages required
         */
        public String getImportPackage() throws ResolverException {
            return importPackage;
        }

        public String getProvisionBundle() {
            return null;
        }

        public Collection<ModelledResource> getRequiredUseBundle() throws ResolverException {
            return null;
        }

        public String getUseBundle() {
            return null;
        }

    }

    private static class MockModelledResource implements ModelledResource {

        private Collection<? extends ImportedPackage> importedPackages;

        public String toDeploymentString() {
            return null;
        }

        public ExportedBundle getExportedBundle() {
            return null;
        }

        public Collection<? extends ExportedPackage> getExportedPackages() {
            return null;
        }

        public Collection<? extends ExportedService> getExportedServices() {
            return null;
        }

        public ImportedBundle getFragmentHost() {
            return null;
        }

        public Collection<? extends ImportedPackage> getImportedPackages() {
            return importedPackages;
        }

        public void setImportedPackages(Collection<? extends ImportedPackage> importedPackages) {
            this.importedPackages = importedPackages;
        }

        public Collection<? extends ImportedService> getImportedServices() {
            return null;
        }

        public String getLocation() {
            return null;
        }

        public Collection<? extends ImportedBundle> getRequiredBundles() {
            return null;
        }

        public String getSymbolicName() {
            return null;
        }

        public ResourceType getType() {
            return null;
        }

        public String getVersion() {
            return null;
        }

        public boolean isFragment() {
            return false;
        }

    }

    private static class MockImportedPackage implements ImportedPackage {

        private String packageName;
        private String versionRange;

        public MockImportedPackage(String packageName, String versionRange) {
            this.packageName = packageName;
            this.versionRange = versionRange;
        }

        public String getAttributeFilter() {
            return null;
        }

        public ResourceType getType() {
            return null;
        }

        public boolean isMultiple() {
            return false;
        }

        public boolean isOptional() {
            return false;
        }

        public boolean isSatisfied(Provider provider) {
            return false;
        }

        public String toDeploymentString() {
            return packageName + ";version=\"" + versionRange + "\"";
        }

        public Map<String, String> getAttributes() {
            return null;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getVersionRange() {
            return versionRange;
        }

    }
}

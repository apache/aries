/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.application.resolve.transform.cm;

import java.util.Collection;
import java.util.Map;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.spi.resolve.PostResolveTransformer;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ModelledResource;

public class ConfigurationPostResolveTransformerImpl implements PostResolveTransformer 
{
    /*
     * (non-Javadoc)
     * @see org.apache.aries.application.management.spi.resolve.PostResolveTransformer#postResolveProcess(org.apache.aries.application.ApplicationMetadata, org.apache.aries.application.modelling.DeployedBundles)
     */
    public DeployedBundles postResolveProcess(ApplicationMetadata appMetaData, DeployedBundles deployedBundles) throws ResolverException {
        return new ConfigAwareDeployedBundles(deployedBundles);
    }
    
    /**
     * This class serves as a wrapper for a DeployedBundles instance, if the delegate instance
     * does not import the configuration management package and the deployed content contains the
     * configuration admin bundle, then the necessary import will be added. This is required so that the
     * blueprint-cm bundle will not encounter class casting issues from a ConfigurationAdmin service
     * that has been loaded in an isolated application scope, rather all *.cm related classes are
     * referenced by the same class loader.
     * 
     *
     * @version $Rev$ $Date$
     */
    private static class ConfigAwareDeployedBundles implements DeployedBundles
    {
        private static final String CONFIG_PACKAGE = "org.osgi.service.cm";
        
        private DeployedBundles deployedBundles;
        
        public ConfigAwareDeployedBundles(DeployedBundles deployedBundles) {
            this.deployedBundles = deployedBundles;
        }
        
        public void addBundle(ModelledResource resource) {
            deployedBundles.addBundle(resource);
        }

        public String getContent() {
            return deployedBundles.getContent();
        }

        public Collection<ModelledResource> getDeployedContent() {
            return deployedBundles.getDeployedContent();
        }

        public String getDeployedImportService() {
            return deployedBundles.getDeployedImportService();
        }

        public Collection<ModelledResource> getDeployedProvisionBundle()  {
            return deployedBundles.getDeployedProvisionBundle();
        }

        public Map<String, String> getExtraHeaders() {
            return deployedBundles.getExtraHeaders();
        }

        public String getImportPackage() throws ResolverException {
            String currentImportPackage = deployedBundles.getImportPackage();
            StringBuffer rawImportPackage = new StringBuffer((currentImportPackage != null ? currentImportPackage : ""));
            
            if (! rawImportPackage.toString().contains(CONFIG_PACKAGE)) {
                Collection<ModelledResource> deployedContent = deployedBundles.getDeployedContent();
                
                if (deployedContent != null) {
                    modelledResourceCheck:
                        for (ModelledResource mr : deployedContent) {
                            Collection<? extends ImportedPackage> importedPackages = mr.getImportedPackages();
                            
                            if (importedPackages != null) {
                                for (ImportedPackage importedPackage : importedPackages) {
                                    if (CONFIG_PACKAGE.equals(importedPackage.getPackageName())) {
                                        if (rawImportPackage.length() > 0) {
                                            rawImportPackage.append(",");
                                        }
                                        
                                        rawImportPackage.append(importedPackage.toDeploymentString());
                                        break modelledResourceCheck;
                                    }
                                }
                            }                    
                        }
                }
            }
            
            return (rawImportPackage.length() > 0 ? rawImportPackage.toString() : currentImportPackage);
        }

        public String getProvisionBundle() {
            return deployedBundles.getProvisionBundle();
        }

        public Collection<ModelledResource> getRequiredUseBundle() throws ResolverException {
            return deployedBundles.getRequiredUseBundle();
        }

        public String getUseBundle() {
            return deployedBundles.getUseBundle();
        }
    }
    
}
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

package org.apache.aries.subsystem.core.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.aries.application.Content;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.core.obr.BundleInfoImpl;
import org.apache.aries.subsystem.core.obr.ContentImpl;
import org.apache.aries.subsystem.core.obr.Manve2Repository;
import org.apache.aries.subsystem.core.obr.RepositoryDescriptorGenerator;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class ResourceResolverImpl implements ResourceResolver {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ResourceResolverImpl.class);

    final private BundleContext context;
    private RepositoryAdmin repositoryAdmin;
    private static boolean generated = false;
    private String obrPath;

    public ResourceResolverImpl(BundleContext context) {
        this.context = context;
    }

    public ResourceResolverImpl(BundleContext context, String obrPath) {
        this.context = context;
        this.obrPath = obrPath;
    }
    
    public  void generateOBR() {
        if (generated) {
            return;
        }
        synchronized(this) {
            if (obrPath == null) {
                // set to a default obr file which is local m2 repo
                String file = System.getProperty("user.home") + "/.m2/repository/";
                if (new File(file).exists()) {
                    obrPath = file;
                }
    
            }
    
            File rootFile = new File(obrPath);
            if (!rootFile.exists() || !rootFile.isDirectory()) {
                throw new IllegalArgumentException("obr path " + obrPath
                        + " is not valid");
            }
    
            Manve2Repository repo = new Manve2Repository(rootFile);
    
            SortedSet<String> ss = repo.listFiles();
            Set<BundleInfo> infos = new HashSet<BundleInfo>();
    
            for (String s : ss) {
                BundleInfo info = new BundleInfoImpl(s);
                infos.add(info);
            }
    
            Document doc;
            try {
                doc = RepositoryDescriptorGenerator.generateRepositoryDescriptor(
                        "Subsystem Repository description", infos);
                FileOutputStream fout = new FileOutputStream(obrPath
                        + "repository.xml");
    
                TransformerFactory.newInstance().newTransformer().transform(
                        new DOMSource(doc), new StreamResult(fout));
    
                fout.close();
    
                TransformerFactory.newInstance().newTransformer().transform(
                        new DOMSource(doc), new StreamResult(System.out));
            } catch (Exception e) {
                LOGGER.error("Exception occurred when generate obr", e);
                e.printStackTrace();
            }
    
            registerOBR();
            
            generated = true;

        }

    }

    private void registerOBR() {
        // set repositoryAdmin
        ServiceReference ref = context
                .getServiceReference(RepositoryAdmin.class.getName());
        
        if (ref != null) {
            this.repositoryAdmin = (RepositoryAdmin) context.getService(ref);
    
            try {
                this.repositoryAdmin.addRepository(new File(obrPath
                        + "repository.xml").toURI().toURL());
            } catch (Exception e) {
                LOGGER.warn("Exception occurred when register obr", e);
                e.printStackTrace();
            }
    
            this.context.ungetService(ref);
        } else {
            LOGGER.error("Unable to register OBR as RepositoryAdmin service is not available");
        }

    }

    /**
     * the format of resource is like bundlesymbolicname;version=1.0.0, for example com.ibm.ws.eba.example.blog.api;version=1.0.0,
     */
    public Resource find(String resource) throws SubsystemException {
        generateOBR();
        
        Content content = new ContentImpl(resource);
        
        String symbolicName = content.getContentName();
        // this version could possibly be a range
        String version = content.getVersion().toString();
        StringBuilder filterString = new StringBuilder();
        filterString.append("(&(name" + "=" + symbolicName + "))");
        filterString.append("(version" + "=" + version + "))");

        //org.apache.felix.bundlerepository.Resource[] res = this.repositoryAdmin.discoverResources(filterString.toString());
        Repository[] repos = this.repositoryAdmin.listRepositories();
        org.apache.felix.bundlerepository.Resource res = null;
        for (Repository repo : repos) {
            org.apache.felix.bundlerepository.Resource[] resources = repo.getResources();
            for (int i = 0; i < resources.length; i++) {
                if (resources[i].getSymbolicName().equals(symbolicName)) {
                    if (resources[i].getVersion().compareTo(new Version(version)) == 0) {
                        res = resources[i];
                    }
                }
            }
        }
        if (res == null) {
            throw new SubsystemException("unable to find the resource " + resource);
        }
        
        Map props = res.getProperties();
        

        Object type = props.get(SubsystemConstants.RESOURCE_TYPE_ATTRIBUTE);

        return new ResourceImpl(symbolicName, res.getVersion(), type == null ? SubsystemConstants.RESOURCE_TYPE_BUNDLE : (String)type, res.getURI() , props);
    }
    
    /**
     * the format of resource is like bundlesymbolicname;version=1.0.0, for example com.ibm.ws.eba.example.blog.api;version=1.0.0,
     */
    private org.apache.felix.bundlerepository.Resource findOBRResource(Resource resource) throws SubsystemException {
        String symbolicName = resource.getSymbolicName();
        // this version could possibly be a range
        Version version = resource.getVersion();

        //org.apache.felix.bundlerepository.Resource[] res = this.repositoryAdmin.discoverResources(filterString.toString());
        Repository[] repos = this.repositoryAdmin.listRepositories();
        org.apache.felix.bundlerepository.Resource res = null;
        for (Repository repo : repos) {
            org.apache.felix.bundlerepository.Resource[] resources = repo.getResources();
            for (int i = 0; i < resources.length; i++) {
                if (resources[i].getSymbolicName().equals(symbolicName)) {
                    if (resources[i].getVersion().compareTo(version) == 0) {
                        res = resources[i];
                    }
                }
            }
        }
        return res;
    }

    /**
     * convert to the resource from the obr resource
     */
    private Resource toResource(org.apache.felix.bundlerepository.Resource resource) throws SubsystemException {
        if (resource == null) {
            throw new SubsystemException("unable to find the resource " + resource);
        }
        
        Map props = resource.getProperties();
        

        Object type = props.get(SubsystemConstants.RESOURCE_TYPE_ATTRIBUTE);

        return new ResourceImpl(resource.getSymbolicName(), resource.getVersion(), type == null ? SubsystemConstants.RESOURCE_TYPE_BUNDLE : (String)type, resource.getURI() , props);
    }
    
    public List<Resource> resolve(List<Resource> subsystemContent,
            List<Resource> subsystemResources) throws SubsystemException {
        generateOBR();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Attempt to resolve subsystem content {} subsystem resource {}", subsystemContent.toString(), subsystemResources.toString());
        }
        Resolver obrResolver = this.repositoryAdmin.resolver();
        
        // add subsystem content to the resolver
        for (Resource res : subsystemContent) {
            org.apache.felix.bundlerepository.Resource obrRes = findOBRResource(res);
            obrResolver.add(obrRes);
        }
        
        // add subsystem resource to the resolver
        for (Resource res : subsystemResources) {
            org.apache.felix.bundlerepository.Resource obrRes = findOBRResource(res);
            obrResolver.add(obrRes);
        }
        
        // Question: do we need to create the repository.xml for the subsystem and add the repo to RepoAdmin?
        List<Resource> resources = new ArrayList<Resource>();
        if (obrResolver.resolve()) {
            for (org.apache.felix.bundlerepository.Resource res : obrResolver.getRequiredResources()) {
                resources.add(toResource(res));
            }
            
            // Question: should we handle optional resource differently?
            for (org.apache.felix.bundlerepository.Resource res : obrResolver.getOptionalResources()) {
                resources.add(toResource(res));
            }
        } else {
            // log the unsatisfied requirement
            Reason[] reasons = obrResolver.getUnsatisfiedRequirements();
            for (Reason reason : reasons) {
                LOGGER.warn("Unable to resolve subsystem content {} subsystem resource {} because of unsatisfied requirement {}", 
                        new Object[] {subsystemContent.toString(), subsystemResources.toString(), reason.getRequirement().getName()});
            }

        }
        
        return resources;
    }

}

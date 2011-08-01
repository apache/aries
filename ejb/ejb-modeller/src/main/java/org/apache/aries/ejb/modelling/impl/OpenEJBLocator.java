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
package org.apache.aries.ejb.modelling.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.ejb.modelling.EJBLocator;
import org.apache.aries.ejb.modelling.EJBRegistry;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.openejb.config.AnnotationDeployer;
import org.apache.openejb.config.AppModule;
import org.apache.openejb.config.EjbModule;
import org.apache.openejb.config.ReadDescriptors;
import org.apache.openejb.jee.EjbJar;
import org.apache.openejb.jee.EnterpriseBean;
import org.apache.openejb.jee.SessionBean;
import org.apache.openejb.jee.SessionType;
import org.apache.xbean.finder.ClassFinder;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenEJBLocator implements EJBLocator {

  private static final Logger logger = LoggerFactory.getLogger(OpenEJBLocator.class);
  
  public static class ResourceClassLoader extends ClassLoader {

    private final List<IDirectory> classpath;
    public ResourceClassLoader(List<IDirectory> cpEntries) {
      classpath = cpEntries;
    }
    
    @Override
    protected URL findResource(String resName) {
      for(IDirectory id : classpath) {
        IFile f = id.getFile(resName);
        if(f != null)
          try {
            return f.toURL();
          } catch (MalformedURLException e) {
            logger.error("Error getting URL for file " + f, e);
          }
      }
      return null;
    }
  }

  public void findEJBs(BundleManifest manifest, IDirectory bundle,
      EJBRegistry registry) throws ModellerException {

    try {
      IFile file = bundle.getFile("META-INF/ejb-jar.xml");
      EjbJar ejbJar = (file == null) ? new EjbJar() : ReadDescriptors.readEjbJar(file.toURL());
      
      EjbModule module = new EjbModule(ejbJar);
      
      List<IDirectory> cpEntries = getClassPathLocations(manifest, bundle);
      
      ClassLoader cl = new ResourceClassLoader(cpEntries);
      module.setClassLoader(cl);
      
      //This may become AnnotationFinder at some point in the future. We build our
      //own because we can't trust anyone to get the classpath right otherwise!
      module.setFinder(new ClassFinder(cl, getClassPathURLs(cpEntries)));
      
      AppModule app = new AppModule(module);
      
      new AnnotationDeployer().deploy(app);
      
      for(EnterpriseBean eb : ejbJar.getEnterpriseBeans()) {
        
        if(!!!(eb instanceof SessionBean))
          continue;
        else
          registerSessionBean(registry, (SessionBean) eb);
      }
      
    } catch (Exception e) {
      throw new ModellerException(e);
    }
  }

  private List<URL> getClassPathURLs(List<IDirectory> cpEntries) throws MalformedURLException {
    List<URL> result = new ArrayList<URL>();
    
    for(IDirectory id : cpEntries) {
      result.add(id.toURL());
    }
    return result;
  }

  private List<IDirectory> getClassPathLocations(BundleManifest manifest,
      IDirectory bundle) {
    List<IDirectory> result = new ArrayList<IDirectory>();
    
    String rawCp = manifest.getRawAttributes().getValue(Constants.BUNDLE_CLASSPATH);
    
    if(rawCp == null || rawCp.trim() == "")
      result.add(bundle);
    else {
      List<NameValuePair> splitCp = ManifestHeaderProcessor.parseExportString(rawCp);
      
      for(NameValuePair nvp : splitCp) {
        String name = nvp.getName().trim();
        if(".".equals(name))
          result.add(bundle);
        else {
          IFile f = bundle.getFile(name);
          
          if(f==null)
            continue;
          
          IDirectory converted = f.convertNested();
          if(converted != null)
            result.add(converted);
        }
      }
    }
    return result;
  }

  private void registerSessionBean(EJBRegistry registry, SessionBean sb) {
    
    String name = sb.getEjbName();
    String type = sb.getSessionType().toString();
    
    if(sb.getSessionType() == SessionType.STATEFUL)
      return;
    
    boolean added = false;
    
    for(String iface : sb.getBusinessLocal()) {
      added = true;
      registry.addEJBView(name, type, iface, false);
    }
    
    for(String iface : sb.getBusinessRemote()) {
      added = true;
      registry.addEJBView(name, type, iface, true);
    }
    
    if(sb.getLocal() != null) {
      added = true;
      registry.addEJBView(name, type, sb.getLocal(), false);
    }
    
    if(sb.getLocalHome() != null) {
      added = true;
      registry.addEJBView(name, type, sb.getLocalHome(), false);
    }
      
    if(sb.getRemote() != null) {
      added = true;
      registry.addEJBView(name, type, sb.getRemote(), true);
    }
    
    if(sb.getHome() != null) {
      added = true;
      registry.addEJBView(name, type, sb.getHome(), true);
    }
    //If not added elsewhere then we have a no-interface view
    if(!!!added) {
      registry.addEJBView(name, type, sb.getEjbClass(), false);
    }
  }
}

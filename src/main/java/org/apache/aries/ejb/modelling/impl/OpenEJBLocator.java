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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.ejb.modelling.EJBLocator;
import org.apache.aries.ejb.modelling.EJBRegistry;
import org.apache.aries.util.filesystem.ICloseableDirectory;
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
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EJBLocator} that uses OpenEJB to find EJBs
 *
 */
public class OpenEJBLocator implements EJBLocator {

  private static final Logger logger = LoggerFactory.getLogger(OpenEJBLocator.class);

  public class ClasspathIDirectory implements IDirectory {

    private final IDirectory parent;
    private final String entry;
    
    public ClasspathIDirectory(IDirectory parent, String name) {
      this.parent = parent;
      this.entry = (name.endsWith("/")) ? name : name + "/";
    }

    public IDirectory convert() {
      return parent.convert();
    }

    public IDirectory convertNested() {
      return parent.convertNested();
    }

    public IFile getFile(String arg0) {
      return parent.getFile(entry + arg0);
    }

    public long getLastModified() {
      return parent.getLastModified();
    }

    public String getName() {
      return parent.getName() + entry;
    }

    public IDirectory getParent() {
      return parent.getParent();
    }

    public IDirectory getRoot() {
      return parent.getRoot();
    }

    public long getSize() {
      return parent.getSize();
    }

    public boolean isDirectory() {
      return parent.isDirectory();
    }

    public boolean isFile() {
      return parent.isFile();
    }

    public boolean isRoot() {
      return parent.isRoot();
    }

    public Iterator<IFile> iterator() {
      return parent.iterator();
    }

    public List<IFile> listAllFiles() {
      List<IFile> files = new ArrayList<IFile>();
      for(IFile f : parent.listAllFiles()) {
        if(f.getName().startsWith(entry))
          files.add(f);
      }
      return files;
    }

    public List<IFile> listFiles() {
      List<IFile> files = new ArrayList<IFile>();
      for(IFile f : parent.listFiles()) {
        if(f.getName().startsWith(entry))
          files.add(f);
      }
      return files;
    }

    public InputStream open() throws IOException, UnsupportedOperationException {
      return parent.open();
    }

    public ICloseableDirectory toCloseable() {
      return parent.toCloseable();
    }

    public URL toURL() throws MalformedURLException {
      return parent.toURL();
    }
  }

  public void findEJBs(BundleManifest manifest, IDirectory bundle,
      EJBRegistry registry) throws ModellerException {

    logger.debug("Scanning " + manifest.getSymbolicName() + "_" + manifest.getManifestVersion() +
        " for EJBs");
    
    String ejbJarLocation = (manifest.getRawAttributes().getValue(
        "Web-ContextPath") == null) ? "META-INF/ejb-jar.xml" : "WEB-INF/ejb-jar.xml";
    
    try {
      //If we have an ejb-jar.xml then parse it 
      IFile file = bundle.getFile(ejbJarLocation);
      EjbJar ejbJar = (file == null) ? new EjbJar() : ReadDescriptors.readEjbJar(file.toURL());
      
      EjbModule module = new EjbModule(ejbJar);
      
      //We build our own because we can't trust anyone to get the classpath right otherwise!
      module.setFinder(new IDirectoryFinder(AnnotationDeployer.class.getClassLoader(), 
          getClassPathLocations(manifest, bundle)));
      
      //Scan our app for annotated EJBs
      AppModule app = new AppModule(module);
      new AnnotationDeployer().deploy(app);
      
      //Register our session beans
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
  
  /**
   * Find the classpath entries for our bundle
   * 
   * @param manifest
   * @param bundle
   * @return
   */
  private List<IDirectory> getClassPathLocations(BundleManifest manifest,
      IDirectory bundle) {
    List<IDirectory> result = new ArrayList<IDirectory>();
    
    String rawCp = manifest.getRawAttributes().getValue(Constants.BUNDLE_CLASSPATH);
    
    logger.debug("Classpath is " + rawCp);
    
    if(rawCp == null || rawCp.trim() == "")
      result.add(bundle);
    else {
      List<NameValuePair> splitCp = ManifestHeaderProcessor.parseExportString(rawCp);
      
      List<IFile> allFiles = null;
      
      for(NameValuePair nvp : splitCp) {
        String name = nvp.getName().trim();
        if(".".equals(name)) {
          result.add(bundle);
        }
        else {
          IFile f = bundle.getFile(name);
          
          if(f==null) {
            //This possibly just means no directory entries in a
            //Zip. Check to make sure
            if(allFiles == null)
              allFiles = bundle.listAllFiles();
            
            for(IFile file : allFiles) {
              if(file.getName().startsWith(name)) {
                 result.add(new ClasspathIDirectory(bundle, name));
                 break;
              }
            }
            
          } else {
            IDirectory converted = f.convertNested();
            if(converted != null)
              result.add(converted);
          }
        }
      }
    }
    return result;
  }

  /**
   * Register a located session bean with the {@link EJBRegistry}
   * @param registry
   * @param sb
   */
  private void registerSessionBean(EJBRegistry registry, SessionBean sb) {
    
    String name = sb.getEjbName();
    String type = sb.getSessionType().toString();
    
    logger.debug("Found EJB " + name + " of type " + type);
    
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
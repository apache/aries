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
package org.apache.aries.application.resolver.obr.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.spi.repository.RepositoryGenerator;
import org.apache.aries.application.management.spi.runtime.LocalPlatform;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.resolver.obr.ext.BundleResource;
import org.apache.aries.application.resolver.obr.ext.BundleResourceTransformer;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.FileUtils;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IOUtils;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
public final class RepositoryGeneratorImpl implements RepositoryGenerator
{
  private RepositoryAdmin repositoryAdmin;
  private ModelledResourceManager modelledResourceManager;
  private LocalPlatform tempDir;
  private static Logger logger = LoggerFactory.getLogger(RepositoryGeneratorImpl.class);
  private static Collection<BundleResourceTransformer> bundleResourceTransformers = new ArrayList<BundleResourceTransformer>();
  private static final String MANDATORY_DIRECTIVE = Constants.MANDATORY_DIRECTIVE + ":";


  public void setModelledResourceManager( ModelledResourceManager modelledResourceManager) {
    this.modelledResourceManager = modelledResourceManager;
  }

  public void setTempDir(LocalPlatform tempDir) {
    this.tempDir = tempDir;
  }

  public void setBundleResourceTransformers (List<BundleResourceTransformer> brts) { 
    bundleResourceTransformers = brts;
  }

  public RepositoryGeneratorImpl(RepositoryAdmin repositoryAdmin) {
    this.repositoryAdmin = repositoryAdmin;
  }
  private static void addProperty(Document doc, Element capability, String name,
      String value, String type)
  {
    logger.debug(LOG_ENTRY, "addProperty", new Object[]{doc, capability, name, value, type});
    Element p = doc.createElement("p");
    p.setAttribute("n", name);
    p.setAttribute("v", value);
    if (type != null) p.setAttribute("t", type);
    capability.appendChild(p);
    logger.debug(LOG_ENTRY, "addProperty", new Object[]{});
  }



  /**
   * Write out the resource element
   * 
   * @param r
   *          resource
   * @param writer
   *          buffer writer
   * @throws IOException
   */
  private  static void writeResource(Resource r, String uri, Document doc, Element root) throws IOException
  {
    logger.debug(LOG_ENTRY, "writeResource", new Object[]{r, uri, doc, root});
    Element resource = doc.createElement("resource");
    resource.setAttribute(Resource.VERSION, r.getVersion().toString());
    resource.setAttribute("uri", r.getURI());
    resource.setAttribute(Resource.SYMBOLIC_NAME, r.getSymbolicName());
    resource.setAttribute(Resource.ID, r.getSymbolicName() + "/" + r.getVersion());
    resource.setAttribute(Resource.PRESENTATION_NAME, r.getPresentationName());
    root.appendChild(resource);


    for (Capability c : r.getCapabilities())
      writeCapability(c, doc, resource);

    for (Requirement req : r.getRequirements()) {
      writeRequirement(req, doc, resource);

    }
    logger.debug(LOG_EXIT, "writeResource");

  }

  /**
   * Write out the capability
   * 
   * @param c capability
   * @param writer buffer writer
   * @throws IOException
   */
  private  static void writeCapability(Capability c, Document doc, Element resource) throws IOException
  {
    logger.debug(LOG_ENTRY, "writeCapability", new Object[]{c, doc, resource});
    Element capability = doc.createElement("capability");
    capability.setAttribute("name", c.getName());
    resource.appendChild(capability);

    Property[] props = c.getProperties();

    for (Property entry : props) {

      String name = (String) entry.getName();
      String objectAttrs = entry.getValue();

      String type = getType(name);

      // remove the beginning " and tailing "
      if (objectAttrs.startsWith("\"") && objectAttrs.endsWith("\""))
        objectAttrs = objectAttrs.substring(1, objectAttrs.length() - 1);
      addProperty(doc, capability, name, objectAttrs, type);
    }

    logger.debug(LOG_EXIT, "writeCapability");
  }

  /**
   * write the requirement
   * 
   * @param req
   *          requirement
   * @param writer
   *          buffer writer
   * @throws IOException
   */
  private static void  writeRequirement(Requirement req, Document doc, Element resource) throws IOException
  {
    logger.debug(LOG_ENTRY, "writeRequirement", new Object[]{req, doc, resource});
    Element requirement = doc.createElement("require");
    requirement.setAttribute("name", req.getName());
    requirement.setAttribute("extend", String.valueOf(req.isExtend()));
    requirement.setAttribute("multiple", String.valueOf(req.isMultiple()));
    requirement.setAttribute("optional", String.valueOf(req.isOptional()));
    requirement.setAttribute("filter", req.getFilter());
    requirement.setTextContent(req.getComment());
    resource.appendChild(requirement);
    logger.debug(LOG_EXIT, "writeRequirement");
  }


  public void generateRepository(String repositoryName,
      Collection<? extends ModelledResource> byValueBundles, OutputStream os)
  throws ResolverException, IOException
  {
    logger.debug(LOG_ENTRY, "generateRepository", new Object[]{repositoryName, byValueBundles, os});
    generateRepository(repositoryAdmin, repositoryName, byValueBundles, os);
    logger.debug(LOG_EXIT, "generateRepository");
  }

  public static void generateRepository (RepositoryAdmin repositoryAdmin, String repositoryName,
      Collection<? extends ModelledResource> byValueBundles, OutputStream os)
  throws ResolverException, IOException {
    logger.debug(LOG_ENTRY, "generateRepository", new Object[]{repositoryAdmin, repositoryName, byValueBundles, os});
    Document doc;
    try {
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    } catch (ParserConfigurationException pce) {
      throw new ResolverException(pce);
    }
    Element root = doc.createElement("repository");

    root.setAttribute("name", repositoryName);
    doc.appendChild(root);
    for (ModelledResource mr : byValueBundles) {
      BundleResource bundleResource = new BundleResource(mr, repositoryAdmin);
      if (bundleResourceTransformers.size() > 0) { 
        for (BundleResourceTransformer brt : bundleResourceTransformers) { 
          bundleResource = brt.transform (bundleResource);
        }
      }
      writeResource (bundleResource, mr.getLocation(), doc, root);
    }

    try {
      Transformer trans = TransformerFactory.newInstance().newTransformer();
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
      trans.transform(new DOMSource(doc), new StreamResult(os));
    } catch (TransformerException te) {
      logger.debug(LOG_EXIT, "generateRepository", te);
      throw new ResolverException(te);
    }
    logger.debug(LOG_EXIT, "generateRepository");
  }
  private static String getType(String name) {
    logger.debug(LOG_ENTRY, "getType", new Object[]{name});
    String type = null;
    if (Constants.VERSION_ATTRIBUTE.equals(name) || (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(name))) {
      type =  "version";
    } else if (Constants.OBJECTCLASS.equals(name) || MANDATORY_DIRECTIVE.equals(name))
      type = "set";
    logger.debug(LOG_EXIT, "getType", new Object[]{type});
    return type;
  }

  public void generateRepository(String[] source, OutputStream fout) throws IOException{

    logger.debug(LOG_ENTRY, "generateRepository", new Object[]{source, fout});
    List<URI> jarFiles = new ArrayList<URI>();
    InputStream in = null;
    OutputStream out = null;
    File wstemp = null;
    Set<ModelledResource> mrs = new HashSet<ModelledResource>();
    if (source != null) {
      try {
        for (String urlString : source) {
          
          // for each entry, we need to find out whether it is in local file system. If yes, we would like to
          // scan the bundles recursively under that directory
          URI entry;
          try {
            File f = new File(urlString);
            if (f.exists()) {
              entry = f.toURI();
            } else {
              entry = new URI(urlString);
            }
            if ("file".equals(entry.toURL().getProtocol())) {
              jarFiles.addAll(FileUtils.getBundlesRecursive(entry));
            } else {
              jarFiles.add(entry);
            }
          } catch (URISyntaxException use) {
            throw new IOException(urlString + " is not a valide uri.");
          }

        }
        for (URI jarFileURI : jarFiles) {
          String uriString = jarFileURI.toString();
          File f = null;
          if ("file".equals(jarFileURI.toURL().getProtocol())) {
            f = new File(jarFileURI);
          } else {
            int lastIndexOfSlash = uriString.lastIndexOf("/");
            String fileName = uriString.substring(lastIndexOfSlash + 1);
            //we need to download this jar/war to wstemp and work on it
            URLConnection jarConn = jarFileURI.toURL().openConnection();
            in = jarConn.getInputStream();
            if (wstemp == null) {
              wstemp = new File(tempDir.getTemporaryDirectory(), "generateRepositoryXML_" + System.currentTimeMillis());
              boolean created = wstemp.mkdirs();
              if (created) {
                logger.debug("The temp directory was created successfully.");
              } else {
                logger.debug("The temp directory was NOT created.");
              }
            }
            //Let's open the stream to download the bundles from remote
            f = new File(wstemp, fileName);
            out = new FileOutputStream(f);
            IOUtils.copy(in, out);
          } 

          IDirectory jarDir = FileSystem.getFSRoot(f);
          mrs.add(modelledResourceManager.getModelledResource(uriString, jarDir));

        }
        generateRepository("Resource Repository", mrs, fout);

      } catch (Exception e) {
        logger.debug(LOG_EXIT, "generateRepository");
        throw new IOException(e);
      } finally {
        IOUtils.close(in);
        IOUtils.close(out);
        if (wstemp != null) {
          IOUtils.deleteRecursive(wstemp);
        }
      }
    } else {

      logger.debug("The URL list is empty");
    }

    logger.debug(LOG_EXIT, "generateRepository");
  }


}
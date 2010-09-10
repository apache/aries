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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.aries.application.management.RepositoryGenerator;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.resolver.obr.ext.BundleResource;
import org.apache.aries.application.resolver.obr.ext.BundleResourceTransformer;
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
  private static Logger logger = LoggerFactory.getLogger(RepositoryGeneratorImpl.class);
  private static Collection<BundleResourceTransformer> bundleResourceTransformers = new ArrayList<BundleResourceTransformer>();
  
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

    String mandatoryDirective = Constants.MANDATORY_DIRECTIVE + ":";
    boolean mandatoryPresent = false;
    for (Property entry : props) {


      String name = (String) entry.getName();
      String objectAttrs = entry.getValue();
      if (name.endsWith(":")) {
        if (mandatoryDirective.equals(name)) {
          mandatoryPresent = true;
          // remove the : and write it out
          name = name.substring(0, name.length() - 1);
        } else {
          // ignore other directives
          continue;
        }
      }
      String type = getType(name);

      // remove the beginning " and tailing "
      if (objectAttrs.startsWith("\"") && objectAttrs.endsWith("\""))
        objectAttrs = objectAttrs.substring(1, objectAttrs.length() - 1);
      addProperty(doc, capability, name, objectAttrs, type);
    }

    // OBR's strange behaviour requires that we write out the mandatory entry
    // with an empty string if the mandatory is not specified
    if (!!!mandatoryPresent) {
      addProperty(doc, capability, Constants.MANDATORY_DIRECTIVE, "" , null);
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
    } else if (Constants.OBJECTCLASS.equals(name))
      type = "set";
    logger.debug(LOG_EXIT, "getType", new Object[]{type});
    return type;
  }

}
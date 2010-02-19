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
package org.apache.aries.application.resolver.obr.generator;

import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.aries.application.Content;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.service.obr.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class RepositoryDescriptorGenerator
{
  public static Document generateRepositoryDescriptor(String name, Set<BundleInfo> bundles) throws ParserConfigurationException
  {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element root = doc.createElement("repository");
    
    root.setAttribute("name", name);
    doc.appendChild(root);
    
    for (BundleInfo info : bundles) {
      Element resource = doc.createElement("resource");
      resource.setAttribute(Resource.VERSION, info.getVersion().toString());
      resource.setAttribute("uri", info.getLocation());
      resource.setAttribute(Resource.SYMBOLIC_NAME, info.getSymbolicName());
      resource.setAttribute(Resource.PRESENTATION_NAME, info.getHeaders().get(Constants.BUNDLE_NAME));
      resource.setAttribute(Resource.ID, info.getSymbolicName() + "/" + info.getVersion());
      root.appendChild(resource);
      
      addBundleCapability(doc, resource, info);
      
      for (Content p : info.getExportPackage()) {
        addPackageCapability(doc, resource, info, p);
      }
      
      for (Content p : info.getImportPackage()) {
        addPackageRequirement(doc, resource, info, p);
      }
      
      for (Content p : info.getRequireBundle()) {
        addBundleRequirement(doc, resource, info, p);
      }
    }
    
    return doc;
  }

  private static void addBundleRequirement(Document doc, Element resource, BundleInfo info, Content p)
  {
    Element requirement = doc.createElement("require");
    requirement.setAttribute("name", "bundle");
    
    requirement.setAttribute("extend", "false");
    requirement.setAttribute("multiple", "false");
    requirement.setAttribute("optional", "false");
    
    requirement.setAttribute("filter", ManifestHeaderProcessor.generateFilter("bundle", p.getContentName(), p.getAttributes()));
    
    resource.appendChild(requirement);
  }

  private static void addPackageRequirement(Document doc, Element resource, BundleInfo info, Content p)
  {
    Element requirement = doc.createElement("require");
    requirement.setAttribute("name", "package");
    
    requirement.setAttribute("extend", "false");
    requirement.setAttribute("multiple", "false");
    
    String optional = p.getDirective("optional");
    if (optional == null) optional = "false";
    
    requirement.setAttribute("optional", optional);
    
    requirement.setAttribute("filter", ManifestHeaderProcessor.generateFilter("package", p.getContentName(), p.getAttributes()));
    
    resource.appendChild(requirement);
  }

  private static void addPackageCapability(Document doc, Element resource, BundleInfo info, Content p)
  {
    Element capability = doc.createElement("capability");
    capability.setAttribute("name", "package");
    resource.appendChild(capability);
    
    addProperty(doc, capability, "package", p.getContentName(), null);
    addProperty(doc, capability, Constants.VERSION_ATTRIBUTE, p.getAttribute("version"), "version");
    addProperty(doc, capability, Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, info.getSymbolicName(), null);
    addProperty(doc, capability, Constants.BUNDLE_VERSION_ATTRIBUTE, info.getVersion().toString(), "version");
    
    for (Map.Entry<String, String> entry : p.getAttributes().entrySet()) {
      if (!!!Constants.VERSION_ATTRIBUTE.equals(entry.getKey())) {
        addProperty(doc, capability, entry.getKey(), entry.getValue(), null);
      }
    }
    
    String mandatory = p.getDirective(Constants.MANDATORY_DIRECTIVE);
    if (mandatory == null) mandatory = "";
    addProperty(doc, capability, Constants.MANDATORY_DIRECTIVE, mandatory, "set");
  }

  private static void addBundleCapability(Document doc, Element resource, BundleInfo info)
  {
    Element capability = doc.createElement("capability");
    capability.setAttribute("name", "bundle");
    resource.appendChild(capability);
    
    addProperty(doc, capability, Resource.SYMBOLIC_NAME, info.getSymbolicName(), null);
    addProperty(doc, capability, Constants.VERSION_ATTRIBUTE, info.getVersion().toString(), "version");
    addProperty(doc, capability, Resource.PRESENTATION_NAME, info.getHeaders().get(Constants.BUNDLE_NAME), null);
    addProperty(doc, capability, Constants.BUNDLE_MANIFESTVERSION, "2", "version");
    addProperty(doc, capability, Constants.FRAGMENT_ATTACHMENT_DIRECTIVE, info.getBundleDirectives().get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE), null);
    addProperty(doc, capability, Constants.SINGLETON_DIRECTIVE, info.getBundleDirectives().get(Constants.SINGLETON_DIRECTIVE), null);
  }

  private static void addProperty(Document doc, Element capability, String name,
      String value, String type)
  {
    Element p = doc.createElement("p");
    p.setAttribute("n", name);
    p.setAttribute("v", value);
    if (type != null) p.setAttribute("t", type);
    capability.appendChild(p);
  }
}
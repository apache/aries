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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application.modelling.impl;

import static org.apache.aries.application.modelling.ResourceType.SERVICE;
import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.modelling.Provider;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.WrappedReferenceMetadata;
import org.apache.aries.application.modelling.utils.impl.ModellingHelperImpl;
import org.apache.aries.application.utils.FilterUtils;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * an Import-Service entry
 */
public class ImportedServiceImpl implements ImportedService
{
  private final static String DEPRECATED_FILTER_ATTRIBUTE = "filter";
  private final boolean _optional;
  private final String _iface;
  private final String _componentName;
  private final String _blueprintFilter;
  private final Filter _attributeFilter;
  private final boolean _isMultiple;
  private final String _id;
  private final Map<String, String> _attributes;
  private String _toString;
  private String _attribFilterString;   // The manner in which we set means it can't be final
  private final static Pattern SERVICE_EQUALS_SERVICE = Pattern.compile("\\(" + ResourceType.SERVICE.toString()
      + "=" + ResourceType.SERVICE.toString() + "\\)");
  private final Logger logger = LoggerFactory.getLogger(ImportedServiceImpl.class);
  /**
   * Build an ImportedServiceImpl from its elements
   * @param optional
   * @param iface
   * @param componentName
   * @param blueprintFilter
   * @param id
   * @param isMultiple
   * @throws InvalidAttributeException
   */
  public ImportedServiceImpl (boolean optional, String iface, String componentName, 
      String blueprintFilter, String id, boolean isMultiple) 
    throws InvalidAttributeException 
  {
    
    _optional = optional;
    _iface = iface;
    _componentName = componentName;
    _blueprintFilter = FilterUtils.removeMandatoryFilterToken(blueprintFilter);
    _id = id;
    _isMultiple = isMultiple;
    _attributes = new HashMap<String, String>();
    _attributeFilter = generateAttributeFilter (_attributes);
    
    
  }

  private Filter generateAttributeFilter (Map<String, String> attrsToPopulate) throws InvalidAttributeException {
    logger.debug(LOG_ENTRY, "generateAttributeFilter", new Object[]{attrsToPopulate});
    Filter result = null;
    
    try {
      attrsToPopulate.put(ModellingConstants.OBR_SERVICE, ModellingConstants.OBR_SERVICE);
      if (_blueprintFilter != null) { 
        // We may get blueprint filters of the form (&(a=b)(c=d)). We can't put these in 'whole' because we'll 
        // end up generating a filter of the form (&(objectClass=foo)(&(a=b)(c=d)) which subsequent calls to 
        // parseFilter will choke on. So as an interim fix we'll strip off a leading &( and trailing ) if present. 
        String reducedBlueprintFilter;
        if (_blueprintFilter.startsWith("(&")) { 
          reducedBlueprintFilter = _blueprintFilter.substring(2, _blueprintFilter.length() - 1);
        } else { 
          reducedBlueprintFilter = _blueprintFilter;
        }
        
        attrsToPopulate.put(ManifestHeaderProcessor.NESTED_FILTER_ATTRIBUTE, reducedBlueprintFilter);
      }
      if (_componentName != null) { 
        attrsToPopulate.put ("osgi.service.blueprint.compname", _componentName);
      }
      if (_iface != null) { 
        attrsToPopulate.put (Constants.OBJECTCLASS, _iface);
      }
      _attribFilterString = ManifestHeaderProcessor.generateFilter(_attributes);
      if (! "".equals(_attribFilterString)) { 
        result = FrameworkUtil.createFilter(FilterUtils.removeMandatoryFilterToken(_attribFilterString));
      } 
    } catch (InvalidSyntaxException isx) { 
      
      InvalidAttributeException iax = new InvalidAttributeException(isx);
      logger.debug(LOG_EXIT, "generateAttributeFilter", new Object[]{isx});
      throw iax;
    }
    logger.debug(LOG_EXIT, "generateAttributeFilter", new Object[]{result});
    return result;
  }
  
  /** 
   * Deprecated constructor for building these from deprecated Export-Service manifest headers. Do not use this 
   * constructor for any other purpose. 
   * @param ifaceName
   * @param attributes
   * @throws InvalidAttributeException 
   */
  @Deprecated
  public ImportedServiceImpl (String ifaceName, Map<String, String> attributes) throws InvalidAttributeException {
    
    _optional = ("optional".equals(attributes.get("availability:")));
    _iface = ifaceName;
    _isMultiple = false;
    _componentName = null;
    _id = null;
    _attributes = new HashMap<String, String>(attributes);
    
    // The syntax for this deprecated header allows statements of the form, 
    // ImportService: myService;filter="(a=b")
    _blueprintFilter = _attributes.remove(DEPRECATED_FILTER_ATTRIBUTE);
    _attributeFilter = generateAttributeFilter (_attributes);
    
    
  }

  public String getFilter() {
    logger.debug(LOG_ENTRY, "getFilter");
    logger.debug(LOG_EXIT, "getFilter", _blueprintFilter);
    return _blueprintFilter;
  }


  public ResourceType getType() {
    logger.debug(LOG_ENTRY, "getType");
    logger.debug(LOG_EXIT, "getType",  ResourceType.SERVICE);
    return ResourceType.SERVICE;
  }


 public boolean isMultiple() {
   logger.debug(LOG_ENTRY, "isMultiple");
   logger.debug(LOG_EXIT, "isMultiple",  _isMultiple);
    return _isMultiple;
  }



  public boolean isOptional() {
    logger.debug(LOG_ENTRY, "isOptional");
    logger.debug(LOG_EXIT, "isOptional",  _optional);
    return _optional;
  }


  public boolean isSatisfied(Provider capability) {
    logger.debug(LOG_ENTRY, "isSatisfied", capability);
    
    if (capability.getType() != SERVICE) { 
      logger.debug(LOG_EXIT, "isSatisfied",  false);
      return false;
    }
    Dictionary<String, Object> dict = new Hashtable<String, Object> (capability.getAttributes());
    
    // If there's a value for ObjectClass, it may be a comma separated list.
    String objectClass = (String) dict.get(Constants.OBJECTCLASS);
    if (objectClass != null) { 
      String [] split = objectClass.split (",");
      dict.put (Constants.OBJECTCLASS, split);
    }
    
    if (_attributeFilter == null) { 
      logger.debug(LOG_EXIT, "isSatisfied",  true);
      return true;
    }
    boolean allPresent = ModellingHelperImpl.areMandatoryAttributesPresent_(_attributes, capability);
    boolean result = allPresent && _attributeFilter.match(dict);
    logger.debug(LOG_EXIT, "isSatisfied",  result);
    return result;
  }

  
  public String getComponentName() {
    logger.debug(LOG_ENTRY, "getComponentName");
    logger.debug(LOG_EXIT, "getComponentName",  _componentName);
    return _componentName;
  }

  
  public String getId() {
    logger.debug(LOG_ENTRY, "getId");
    logger.debug(LOG_EXIT, "getId",  _id);
    return _id;
  }

  
  public String getInterface() {
    logger.debug(LOG_ENTRY, "getInterface");
    logger.debug(LOG_EXIT, "getInterface",  _iface);
   return _iface;
  }

  public boolean isList() {
    logger.debug(LOG_ENTRY, "isList");    
    boolean result = isMultiple();
    logger.debug(LOG_EXIT, "isList",  result);
    return result;
  }


  public String getAttributeFilter() {
    logger.debug(LOG_ENTRY, "getAttributeFilter");
    logger.debug(LOG_EXIT, "getAttributeFilter",  _attribFilterString);
    return _attribFilterString;
  }
  
  @Override
  public boolean equals (Object o) { 
    
    boolean equal = false;
    if (o==null) { 
      equal = false;
    } else if (o==this) { 
      equal = true;
    } else if (!(o instanceof WrappedReferenceMetadata)) { 
      equal = false;
    } else { 
      equal = toString().equals(o.toString());
    }
    
    return equal;
  }
  
  @Override
  public int hashCode() {
    
    int result = toString().hashCode();
    
    return result;
  }
  
  @Override 
  public String toString() { 
    logger.debug(LOG_ENTRY, "toString");
    
    if (_toString != null) { 
      logger.debug(LOG_EXIT, "toString",  _toString);
      return _toString;
    }
    StringBuffer buf = new StringBuffer("<reference>");
    buf.append("<componentName>" + _componentName + "</componentName>");
    buf.append("<id>" + _id + "</id>");
    buf.append("<interface>" + _iface + "</interface>");
    buf.append("<isList>" + _isMultiple + "</isList>");
    buf.append("<isOptional>" + _optional + "</isOptional>");
    // We don't have a method for writing filters in a canonical form
    buf.append("<filter>" + _blueprintFilter + "</filter>");
    _toString = buf.toString();
    logger.debug(LOG_EXIT, "toString",  _toString);
    return _toString;
  }

  /**
   * A String suitable for use in DeployedImport-Service
   */
  public String toDeploymentString() {
    logger.debug(LOG_ENTRY, "toDeploymentString");    
    String baseFilter = getAttributeFilter();
    // We may have one or more (service=service) elements that must be removed.
    String reducedFilter = SERVICE_EQUALS_SERVICE.matcher(baseFilter).replaceAll("");    
    // now trim off mandatory:<*service occurrences
    String result = FilterUtils.removeMandatoryFilterToken(reducedFilter);
    logger.debug(LOG_EXIT, "toDeploymentString",  result);
    return result;
  }
}

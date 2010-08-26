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


package org.apache.aries.application.resolver.obr.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.modelling.Consumer;
import org.apache.aries.application.utils.FilterUtils;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Requirement;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequirementImpl implements Requirement
{


  private final Logger logger = LoggerFactory.getLogger(RequirementImpl.class);
  private final Consumer consumer;

  private final String name;
  private final String filter;
  private final boolean multiple;
  private final boolean optional;
  private final boolean extend;
  private final String comment;

  public RequirementImpl(String name, Filter filter, boolean multiple, boolean optional, boolean extend, String comment)
  {
    this.name = name;
    this.filter = filter.toString();
    this.multiple = multiple;
    this.optional = optional;
    this.extend = extend;
    this.comment = comment;
    this.consumer = null;
  }

  public RequirementImpl(Consumer consumer) {
    this.consumer = consumer;
    this.name = getName();
    this.filter= getFilter();
    this.multiple= isMultiple();
    this.optional= isOptional();
    this.extend = false;
    this.comment = getComment();

  }



  public String getComment()
  {

    logger.debug(LOG_ENTRY,"getComment" );
    if (consumer!= null) {
      String cleanFilter = FilterUtils.removeMandatoryFilterToken(consumer.getAttributeFilter());
      Map<String, String> atts = ManifestHeaderProcessor.parseFilter(cleanFilter);
      String comment = "Requires " + consumer.getType().toString() + " with attributes " + atts;
      logger.debug(LOG_EXIT,"getComment", comment );
      return comment;
    } else {
      logger.debug(LOG_EXIT,"getComment", this.comment );
      return this.comment;
    }
  }


  public String getFilter()
  {
    String result;
    if (consumer != null) {
      result = consumer.getAttributeFilter();
    } else {
      result = this.filter;
    }
    return result;
  }


  public String getName()
  {

    String result;
    if (consumer != null) {
      result = consumer.getType().toString();
    } else {
      result = this.name;
    }
    return result;
  }


  public boolean isExtend()
  {
    return this.extend;
  }


  public boolean isMultiple()
  {
    boolean result;
    if (consumer != null ) {
      result = consumer.isMultiple();
    } else {
      result = this.multiple;
    }
    return result;
  }


  public boolean isOptional()
  {
    boolean result;
    if (consumer != null) {
      result = consumer.isOptional();
    } else {
      result = this.optional;
    }
    return result;
  }

  @SuppressWarnings("unchecked")

  public boolean isSatisfied(Capability cap)
  {
   
    logger.debug(LOG_ENTRY,"isSatisfied", cap );
    boolean result = false;

    String name = getName();
    if (name.equals(cap.getName())) {
      String filterToCreate = getFilter();
      try {
        Filter f = FrameworkUtil.createFilter(FilterUtils.removeMandatoryFilterToken(filterToCreate));
        Hashtable<String, Object> hash = new Hashtable<String, Object>();
        List<Property> props = Arrays.asList(cap.getProperties());
        if ((props != null) && (!!!props.isEmpty())) {
          for (Property prop : props) {
            hash.put(prop.getName(), prop.getValue());
          }
        }

        result = f.match(hash);
      } catch (InvalidSyntaxException e) {
        logger.error(e.getMessage());
      }
    }
    logger.debug(LOG_EXIT,"isSatisfied", result );
    return result;
  }
}

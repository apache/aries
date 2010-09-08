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
package org.apache.aries.application.utils;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class FilterUtils
{
  private static final Pattern regexp = Pattern.compile("\\(mandatory:.*?\\)");

  private static final  Logger logger = LoggerFactory.getLogger(FilterUtils.class);
  /**
   * Filters we generate may contain stanzas like (mandatory:<*symbolicname)
   * These are for OBR, and are not OSGi friendly!!! This method removes them.
   * 
   * @param filter
   * @return A filter with the mandatory stanzas removed or null if a null filter is supplied
   */
  public static String removeMandatoryFilterToken(String filter) {
    logger.debug(LOG_ENTRY, "areMandatoryAttributesPresent", new Object[]{filter});
    if(filter != null) {
      filter = regexp.matcher(filter).replaceAll("");
    
      int openBraces = 0;
      for (int i=0; openBraces < 3; i++) {
        i = filter.indexOf('(', i);
        if (i == -1) { 
          break;
        } else { 
          openBraces++;
        }
      }
      // Need to prune (& or (| off front and ) off end
      if (openBraces < 3 && 
          (filter.startsWith("(&") || filter.startsWith("(|"))) { 
        filter = filter.substring(2, filter.length() - 1);
      }
    }
    logger.debug(LOG_EXIT, "removeMandatoryFilterToken", filter);
    return filter;
  }
}

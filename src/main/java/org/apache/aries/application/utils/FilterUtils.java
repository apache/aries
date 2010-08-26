/*
 * @start_prolog@
 * ============================================================================
 * IBM Confidential OCO Source Materials
 *
 * 5724-J08, 5724-I63, 5724-H88, 5724-H89, 5655-N02, 5733-W70 Copyright IBM Corp. 2010
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 * ============================================================================
 * @end_prolog@
 * 
 * Change activity:
 * 
 * Issue       Date        Name        Description
 * ----------- ----------- --------    ------------------------------------
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

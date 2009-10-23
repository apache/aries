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
package org.apache.aries.application.utils.manifest;

import static org.apache.aries.application.utils.AppConstants.TRACE_GROUP;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestProcessor
{
  /**
   * Reads a manifest's main attributes into a String->String map.
   * <p>
   * Will always return a map, empty if the manifest had no attributes.
   * 
   * @param mf The manifest to read.
   * @return Map of manifest main attributes.
   */
  public static Map<String, String> readManifestIntoMap(Manifest mf){

    HashMap<String, String> props = new HashMap<String, String>();
    
    Attributes mainAttrs = mf.getMainAttributes();
    if (mainAttrs!=null){
      Set<Entry<Object, Object>> attributeSet =  mainAttrs.entrySet(); 
      if (attributeSet != null){
        // Copy all the manifest headers across. The entry set should be a set of
        // Name to String mappings, by calling String.valueOf we do the conversion
        // to a string and we do not NPE.
        for (Map.Entry<Object, Object> entry : attributeSet) {
          props.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
      }    
    }
         
    return props;
  }
  
  /**
   * 
   * Splits a delimiter separated string, tolerating presence of non separator commas
   * within double quoted segments.
   * 
   * Eg.
   * com.ibm.ws.eba.helloWorldService;version="[1.0.0, 1.0.0]" &
   * com.ibm.ws.eba.helloWorldService;version="1.0.0"
   * com.ibm.ws.eba.helloWorld;version="2";bundle-version="[2,30)"
   * com.acme.foo;weirdAttr="one;two;three";weirdDir:="1;2;3"
   *  @param value          the value to be split
   *  @param delimiter      the delimiter string such as ',' etc.
   *  @return List<String>  the components of the split String in a list
   */
  public static List<String> split(String value, String delimiter)
  {

    List<String> result = new ArrayList<String>();
    if (value != null) {
      String[] packages = value.split(delimiter);
      
      for (int i = 0; i < packages.length; ) {
        String tmp = packages[i++].trim();
        // if there is a odd number of " in a string, we need to append
        while (count(tmp, "\"") % 2 == 1) {
          // check to see if we need to append the next package[i++]          
            tmp = tmp + delimiter + packages[i++].trim();          
        }
        
        result.add(tmp);
      }
    }

    return result;
  }  
  
  /**
   * count the number of characters in a string
   * @param parent The string to be searched
   * @param subString The substring to be found
   * @return the number of occurrence of the subString
   */
   private static int count(String parent, String subString) {
     
     int count = 0 ;
     int i = parent.indexOf(subString);
     while (i > -1) {
       if (parent.length() >= i+1)
         parent = parent.substring(i+1);
       count ++;
       i = parent.indexOf(subString);
     }
     return count;
   }  
}

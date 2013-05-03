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
package org.apache.aries.util.manifest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;

/**
 * This class contains utilities for parsing manifests. It provides methods to
 * parse the manifest, read a manifest into a map and to split an manifest
 * entry that follows the Import-Package syntax.
 */
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
   * mapToManifest
   */
  public static Manifest mapToManifest (Map<String,String> attributes)
  {
    Manifest man = new Manifest();
    Attributes att = man.getMainAttributes();
    att.putValue(Attributes.Name.MANIFEST_VERSION.toString(), Constants.MANIFEST_VERSION);
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      att.putValue(entry.getKey(),  entry.getValue());
    }
    return man;
  }
  
  /**
   * This method parses the manifest using a custom manifest parsing routine.
   * This means that we can avoid the 76 byte line length in a manifest providing
   * a better developer experience.
   * 
   * @param in the input stream to read the manifest from.
   * @return   the parsed manifest
   * @throws IOException
   */
  public static Manifest parseManifest(InputStream in) throws IOException
  {
    Manifest man = new Manifest();
    
    try
    {
      // I'm assuming that we use UTF-8 here, but the jar spec doesn't say.
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      
      String line;
      StringBuilder attribute = null;
      
      String namedAttribute = null;
      
      do {
        line = reader.readLine();
  
        // if we get a blank line skip to the next one
        if (line != null && line.trim().length() == 0) continue;
        if (line != null && line.charAt(0) == ' ' && attribute != null) {
          // we have a continuation line, so add to the builder, ignoring the
          // first character
          attribute.append(line.trim());
        } else if (attribute == null) {
          attribute = new StringBuilder(line.trim());
        } else if (attribute != null) {
          // We have fully parsed an attribute
          int index = attribute.indexOf(":");
          String attributeName = attribute.substring(0, index).trim();
          // TODO cope with index + 1 being after the end of attribute
          String attributeValue = attribute.substring(index + 1).trim();
          
          if ("Name".equals(attributeName)) {
            man.getEntries().put(attributeValue, new Attributes());
            namedAttribute = attributeValue;
          } else {
        	Attributes.Name nameToAdd = new Attributes.Name(attributeName);
            if (namedAttribute == null || !man.getMainAttributes().containsKey(nameToAdd)) {
              man.getMainAttributes().put(nameToAdd, attributeValue);
            } else {
              man.getAttributes(namedAttribute).put(nameToAdd, attributeValue);
            }
          }
          
          if (line != null) attribute = new StringBuilder(line.trim());
        }
      } while (line != null);
    }
    finally {
      IOUtils.close(in);
    }
    return man;
  }
  
  /**
   * Obtain a manifest from an IDirectory. 
   * 
   * @param appDir
   * @param manifestName the name of manifest
   * @return Manifest, or null if none found.
   * @throws IOException
   */
  public static Manifest obtainManifestFromAppDir(IDirectory appDir, String manifestName) throws IOException{
    IFile manifestFile = appDir.getFile(manifestName);
    Manifest man = null;
    if (manifestFile != null) {
      man = parseManifest(manifestFile.open());
    }
    return man;
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

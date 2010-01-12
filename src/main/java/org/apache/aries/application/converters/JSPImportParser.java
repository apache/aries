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
package org.apache.aries.application.converters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSPImportParser {

  /**
   * 
   * @param is
   *          An input stream of character-based text. We expect this to be JSP
   *          source code.
   * @return Each java package found within valid JSP import tags
   * @throws IOException
   */
  public static Collection<String> getImports (InputStream is) throws IOException {
    Collection<String> importedPackages = new LinkedList<String>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line = null;
    do { 
      line = reader.readLine();
      // searchMatchedGroupForImports could take (line): I've not done that because 
      // the entry trace, once working, will print out lots of useless information.  
      if (line != null) { 
        Matcher hasJSPimport = lineWithJSPimport.matcher(line);
        if (hasJSPimport.find()) {
          Collection<String> foundImports = searchMatchedGroupForImports (hasJSPimport.group());
          for (String found : foundImports) { 
            if (!importedPackages.contains(found)) { 
              importedPackages.add(found);
            }
          }
        }
      }
    } while (line != null);
    
    return importedPackages;
  }
  
  private static final Pattern lineWithJSPimport = Pattern.compile("<%@\\s*page\\s*import.*%>");
  private static final Pattern stanzaEnd = Pattern.compile("%>");
  private static final Pattern imports = Pattern.compile("import\\s*=\\s*\"(.*?)\"");
  
  /**
   * 
   * @param groupExtent a block of text known to contain a JSP import
   * @return Each package found within valid JSP import tags
   */
  private static LinkedList<String> searchMatchedGroupForImports (String groupExtent) {
    LinkedList<String> packagesFound = new LinkedList<String>();
    String importStanzas[] = stanzaEnd.split(groupExtent);
    for (String s: importStanzas){ 
      Matcher oneImport = imports.matcher(s);
      if (oneImport.find()) {
        String thisStanzasImports = oneImport.group();
        String allPackages = thisStanzasImports.substring(thisStanzasImports.indexOf("\"")+1, 
            thisStanzasImports.lastIndexOf("\""));
        String [] imports = allPackages.split(",");
        for (String p : imports) { 
          String thisPackage = p.substring(0,p.lastIndexOf('.')).trim();
          
          if (!!!thisPackage.startsWith("java."))
            packagesFound.add(thisPackage);
        }
      }
    }

    return packagesFound;
  }
}

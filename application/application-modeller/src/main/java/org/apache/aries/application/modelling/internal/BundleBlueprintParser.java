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
package org.apache.aries.application.modelling.internal;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.aries.application.utils.AppConstants.*;

/**
 * A bundle may contain a Bundle-Blueprint: header as per p649 of the v4 spec. If present, 
 * this denotes where to look for blueprint xml files. We could use Bundle.findEntries() 
 * to deal with the wildcards that the last entry in the list may contain, but our caller
 * is introspecting .jar files within an EBA and does not have  access to Bundle objects, 
 * so we need this extra support. Our caller needs to iterate over the files 
 * within a jar in each case asking this class, 'is this a blueprint file'?
 *
 */
public class BundleBlueprintParser {
  
  public static final String DEFAULT_HEADER = "OSGI-INF/blueprint/*.xml";
  private static final Logger logger = LoggerFactory.getLogger(BundleBlueprintParser.class);
  
  String _mfHeader = null;
  List<Path> _paths;
  
  static class Path { 
    String directory;
    String filename;  // This will either be a simple filename or 'null', in which case filenamePattern will be set
    Pattern filenamePattern;
    public Path (String d, String f) { 
      directory = d;
      if (f.contains("*")) { 
        filename = null;
        String pattern = f.replace(".", "\\.");
        pattern = pattern.replace("*", ".*");
        filenamePattern = Pattern.compile(pattern);
      } else { 
        filename = f;
        filenamePattern = null;
      }
    }
    
    /**
     * Match this Path object against a specific directory, file pair. Case sensitive. 
     * @param dir Directory
     * @param fil Filename - may not contain a wildcard
     * @return true these match
     */
    public boolean matches (String dir, String fil) {
      boolean match = false;
      if (!directory.equals(dir)) { 
        match = false;
      } else if (filename != null) { 
        match = (filename.equals(fil));
      } else { 
        match = filenamePattern.matcher(fil).matches();
      }
      return match;
    }
  }
  
  /**
   * BundleBlueprintParser constructor
   * @param bundleMf BundleManifest to construct the parser from
   */
  public BundleBlueprintParser (BundleManifest bundleMf) {
    String bundleBPHeader = bundleMf.getRawAttributes().getValue("Bundle-Blueprint");
    setup (bundleBPHeader);
  }

  /**
   * BundleBlueprintParser alternative constructor
   * @param bundleBPHeader Bundle-Blueprint header to construct the parser from
   */
  public BundleBlueprintParser (String bundleBPHeader) {
    setup (bundleBPHeader);
  }
  
  /**
   * Default constructor
   */
  public BundleBlueprintParser () { 
    setup(null);
  }
  
  static final boolean _blueprintHeaderMandatory;
  static 
  { 
	  String blueprintHeaderMandatory = AccessController.doPrivileged(new PrivilegedAction<String>() 
	  {
		  public String run()
	      {
	        return System.getProperty("org.apache.aries.blueprint.header.mandatory", "false");
	      }
	  });
	  _blueprintHeaderMandatory = blueprintHeaderMandatory.toLowerCase().equals("true");
  }
  
  /**
   * @return true if this bundle might contain blueprint files
   */
  public boolean mightContainBlueprint() {
	  return _mfHeader != null && _mfHeader.trim().length() > 0;
  }
  
  private void setup (String bundleBPHeader) { 
    _paths = new LinkedList <Path>();
    if (bundleBPHeader == null) { 
    	if (_blueprintHeaderMandatory) { 
    		_mfHeader = null;
    	} else { 
    		_mfHeader = DEFAULT_HEADER;	
    	}
    } else { 
      _mfHeader = bundleBPHeader;
    }
    
    logger.debug("Bundle-Blueprint header: {}", _mfHeader);
    
    // Break this comma separated list up
    List<String> files = ManifestHeaderProcessor.split(_mfHeader, ",");
    clauses: for (String fileClause : files) {

      // we could be doing directives, so we split again, the clause can
      // have multiple paths with directives at the end.
        
      List<String> yetMoreFiles = ManifestHeaderProcessor.split(fileClause, ";");
      for (String f : yetMoreFiles) {
          
        // if f has an = in it then we have hit the directive, which must
        // be at the end, we do not have any directives so we just continue
        // onto the next clause.
        if (f.contains("=")) continue clauses;
          
        // we need to make sure we have zero spaces here, otherwise stuff may
        // not be found.
        f = f.trim();
        if (f.startsWith("\"") && f.endsWith("\"")) {
          f = f.substring(1,f.length()-1);
        }
          
        int index = f.lastIndexOf('/');
        String path = "";
        String file = f;
        if (index != -1) {
          path = f.substring(0, index);
          file = f.substring(index + 1);
        }
        _paths.add(new Path(path, file));
      }
    }
  }  
 
  /**
   * Iterate through the list of valid file patterns. Return true if this matches against
   * the header provided to the constructor. We're going to have to be case sensitive. 
   *  @param directory Directory name
   *  @param filename File name 
   *  @return true if this is a blueprint file according to the Bundle-Blueprint header
   */
  public boolean isBPFile (String directory, String filename) { 
    logger.debug(LOG_ENTRY, "isBPFile", new Object[] {directory, filename});
      
    boolean result=false;
    for (Path path: _paths) { 
      if (path.matches(directory, filename)) { 
        result = true;
        break;
      }
    }
    
    logger.debug(LOG_EXIT, "isBPFile", result);
    return result;
  }
       
}

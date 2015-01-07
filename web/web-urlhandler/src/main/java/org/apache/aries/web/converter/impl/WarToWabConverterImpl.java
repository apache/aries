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
package org.apache.aries.web.converter.impl;

import static org.apache.aries.web.converter.WarToWabConverter.WEB_CONTEXT_PATH;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.aries.web.converter.WabConversion;
import org.apache.aries.web.converter.WarToWabConverter.InputStreamProvider;
import org.objectweb.asm.ClassReader;
import org.osgi.framework.Constants;

public class WarToWabConverterImpl implements WabConversion {
  private static final String DEFAULT_BUNDLE_VERSION = "1.0";
  private static final String DEFAULT_BUNDLE_MANIFESTVERSION = "2";
  private static final String INITIAL_CLASSPATH_ENTRY = "WEB-INF/classes";
  private static final String CLASSPATH_LIB_PREFIX = "WEB-INF/lib/";
  
  private static final String SERVLET_IMPORTS = 
      "javax.servlet;version=2.5," +
      "javax.servlet.http;version=2.5";
  
  private static final String JSP_IMPORTS =
      "javax.servlet.jsp;version=2.1," +
      "javax.servlet.jsp.el;version=2.1," +
      "javax.servlet.jsp.tagext;version=2.1," +
      "javax.servlet.jsp.resources;version=2.1";
    
  private static final String DEFAULT_IMPORT_PACKAGE_LIST = 
      SERVLET_IMPORTS + "," + JSP_IMPORTS;

  private CaseInsensitiveMap properties;

  // InputStream for the new WAB file
  private CachedOutputStream wab;
  private Manifest wabManifest;
  private String warName;
  private InputStreamProvider input;
  
  // State used for updating the manifest
  private Set<String> importPackages;
  private Set<String> exemptPackages;
  private Map<String, Manifest> manifests; 
  private ArrayList<String> classPath;
  private boolean signed;

  public WarToWabConverterImpl(InputStreamProvider warFile, String name, Properties properties) throws IOException {
      this(warFile, name, new CaseInsensitiveMap(properties));
  }
  
  public WarToWabConverterImpl(InputStreamProvider warFile, String name, CaseInsensitiveMap properties) throws IOException {
    this.properties = properties;
    classPath = new ArrayList<String>();
    importPackages = new TreeSet<String>();
    exemptPackages = new TreeSet<String>();
    input = warFile;
    this.warName = name;
  }
    
  private void generateManifest() throws IOException {
    if (wabManifest != null) {
        // WAB manifest is already generated
        return;
    }
    
    JarInputStream jarInput = null;
    try {
      jarInput = new JarInputStream(input.getInputStream());
      Manifest manifest = jarInput.getManifest();
      if (isBundle(manifest)) {
          wabManifest = updateBundleManifest(manifest);
      } else {
          scanForDependencies(jarInput);
          // Add the new properties to the manifest byte stream
          wabManifest = updateManifest(manifest);
      }
    } 
    finally {
      try { if (jarInput != null) jarInput.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }

  private void convert() throws IOException {
    if (wab != null) {
        // WAB is already converted
        return;
    }
    
    generateManifest();
    
    CachedOutputStream output = new CachedOutputStream();
    JarOutputStream jarOutput = null;
    JarInputStream jarInput = null;
    ZipEntry entry = null;

    // Copy across all entries from the original jar
    int val;
    try {
      jarOutput = new JarOutputStream(output, wabManifest);
      jarInput = new JarInputStream(input.getInputStream());
      byte[] buffer = new byte[2048];
      while ((entry = jarInput.getNextEntry()) != null) {
        // skip signature files if war is signed
        if (signed && isSignatureFile(entry.getName())) {
            continue;
        }
        jarOutput.putNextEntry(entry);        
        while ((val = jarInput.read(buffer)) > 0) {
          jarOutput.write(buffer, 0, val);
        }
      }
    }
    finally {
      if (jarOutput != null) {
        jarOutput.close();
      }
      if (jarInput != null) {
        jarInput.close();
      }
    }
    
    wab = output;
  }

  private boolean isBundle(Manifest manifest)  {
      if (manifest == null) {
          return false;          
      }
      // Presence of _any_ of these headers indicates a bundle...
      Attributes attributes = manifest.getMainAttributes();
      if (attributes.getValue(Constants.BUNDLE_SYMBOLICNAME) != null ||
          attributes.getValue(Constants.BUNDLE_VERSION) != null ||
          attributes.getValue(Constants.BUNDLE_MANIFESTVERSION) != null ||
          attributes.getValue(Constants.IMPORT_PACKAGE) != null ||
          attributes.getValue(WEB_CONTEXT_PATH) != null) {
          return true;
      }
      return false;
  }
  
  private void scanRecursive(final JarInputStream jarInput, boolean topLevel) throws IOException 
  {
    ZipEntry entry;
    
    while ((entry = jarInput.getNextEntry()) != null) {
      if (entry.getName().endsWith(".class")) {
        PackageFinder pkgFinder = new PackageFinder();
        new ClassReader(jarInput).accept(pkgFinder, ClassReader.SKIP_DEBUG);

        importPackages.addAll(pkgFinder.getImportPackages());
        exemptPackages.addAll(pkgFinder.getExemptPackages());
      } else if (entry.getName().endsWith(".jsp")) { 
        Collection<String> thisJSPsImports = JSPImportParser.getImports(jarInput);
        importPackages.addAll(thisJSPsImports);
      } else if (entry.getName().endsWith(".jar")) {
        
        JarInputStream newJar = new JarInputStream(new InputStream() {
          @Override
          public int read() throws IOException
          {
            return jarInput.read();
          }
        });
        
        // discard return, we only care about the top level jars
        scanRecursive(newJar,false);
        
        // do not add jar embedded in already embedded jars
        if (topLevel) {
          manifests.put(entry.getName(), newJar.getManifest());
        }
      }
    }
  }
  
  /**
   * 
   * Read in the filenames inside the war (used for manifest update) Also
   * analyse the bytecode of any .class files in order to find any required
   * imports
   */
  private void scanForDependencies(final JarInputStream jarInput) throws IOException 
  {
    manifests = new HashMap<String, Manifest>();
    
    scanRecursive(jarInput, true);

    // Process manifests from jars in order to work out classpath dependencies
    ClassPathBuilder classPathBuilder = new ClassPathBuilder(manifests);
    for (String fileName : manifests.keySet())
      if (fileName.startsWith(CLASSPATH_LIB_PREFIX)) {
        classPath.add(fileName);
        classPath = classPathBuilder.updatePath(fileName, classPath);
      }
        
    // Remove packages that are part of the classes we searched through
    for (String s : exemptPackages)
      if (importPackages.contains(s))
        importPackages.remove(s);
  }

  protected Manifest updateBundleManifest(Manifest manifest) throws IOException {
      String webCPath = properties.get(WEB_CONTEXT_PATH);
      if (webCPath == null) {
          webCPath = manifest.getMainAttributes().getValue(WEB_CONTEXT_PATH);
      }
      if (webCPath == null) {
          throw new IOException("Must specify " + WEB_CONTEXT_PATH + " parameter. The " + 
                                WEB_CONTEXT_PATH + " header is not defined in the source bundle.");
      } else {
          webCPath = addSlash(webCPath);
          manifest.getMainAttributes().put(new Attributes.Name(WEB_CONTEXT_PATH), webCPath);
      }
      
      // converter is not allowed to specify and override the following properties
      // when source is already a bundle
      checkParameter(Constants.BUNDLE_VERSION);
      checkParameter(Constants.BUNDLE_MANIFESTVERSION);
      checkParameter(Constants.BUNDLE_SYMBOLICNAME);
      checkParameter(Constants.IMPORT_PACKAGE);
      checkParameter(Constants.BUNDLE_CLASSPATH);
              
      return manifest;
  }
  
  private void checkParameter(String parameter) throws IOException {
      if (properties.containsKey(parameter)) {
          throw new IOException("Cannot override " + parameter + " header when converting a bundle");
      }
  }
  
  protected Manifest updateManifest(Manifest manifest) throws IOException
  {
    // If for some reason no manifest was generated, we start our own so that we don't null pointer later on
    if (manifest == null) {
      manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1");
    } else {
      // remove digest attributes if was is signed
      signed = removeDigestAttributes(manifest);
    }
    
    // Compare the manifest and the supplied properties
    
    //
    // Web-ContextPath
    //

    String webCPath = properties.get(WEB_CONTEXT_PATH);
    if (webCPath == null) {
        throw new IOException(WEB_CONTEXT_PATH + " parameter is missing.");
    }
    properties.put(WEB_CONTEXT_PATH, addSlash(webCPath));  

    //
    // Bundle-Version
    //

    if (manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION) == null
        && !properties.containsKey(Constants.BUNDLE_VERSION)) {
      properties.put(Constants.BUNDLE_VERSION, DEFAULT_BUNDLE_VERSION);
    }

    //
    // Bundle-ManifestVersion
    //

    String manifestVersion = properties.get(Constants.BUNDLE_MANIFESTVERSION);
    if (manifestVersion == null) {
        manifestVersion = manifest.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION);
        if (manifestVersion == null) {
            manifestVersion = DEFAULT_BUNDLE_MANIFESTVERSION;
        }
    } else if (!manifestVersion.equals("2")) {
        throw new IOException("Unsupported bundle manifest version " + manifestVersion);
    }
    properties.put(Constants.BUNDLE_MANIFESTVERSION, manifestVersion);

    // 
    // Bundle-ClassPath
    //

    ArrayList<String> classpath = new ArrayList<String>();

    // Set initial entry into classpath
    classpath.add(INITIAL_CLASSPATH_ENTRY);

    // Add any files from the WEB-INF/lib directory + their dependencies
    classpath.addAll(classPath);
    
    // Get the list from the URL and add to classpath (removing duplicates)
    mergePathList(properties.get(Constants.BUNDLE_CLASSPATH), classpath, ",");

    // Get the existing list from the manifest file and add to classpath
    // (removing duplicates)
    mergePathList(manifest.getMainAttributes().getValue(Constants.BUNDLE_CLASSPATH), classpath, ",");

    // Construct the classpath string and set it into the properties
    StringBuffer classPathValue = new StringBuffer();
    for (String entry : classpath) {
      classPathValue.append(",");
      classPathValue.append(entry);
    }

    if (!classpath.isEmpty()) {
      properties.put(Constants.BUNDLE_CLASSPATH, classPathValue.toString().substring(1));
    }

    @SuppressWarnings("serial")
    ArrayList<String> packages = new ArrayList<String>() {
      @Override
      public boolean contains(Object elem) {
        // Check for exact match of export list
        if (super.contains(elem))
          return true;

        if (!!!(elem instanceof String))
          return false;

        String expPackageStmt = (String) elem;
        String expPackage = expPackageStmt.split("\\s*;\\s*")[0];
        
        Pattern p = Pattern.compile("^\\s*"+Pattern.quote(expPackage)+"((;|\\s).*)?\\s*$");
        for (String s : this) {
          Matcher m = p.matcher(s);
          if (m.matches()) {
            return true;
          }
        }

        return false;
      }

    };
    
    //
    // Import-Package
    //
    packages.clear();
    
    // Get the list from the URL and add to classpath (removing duplicates)
    mergePathList(properties.get(Constants.IMPORT_PACKAGE), packages, ",");

    // Get the existing list from the manifest file and add to classpath
    // (removing duplicates)
    mergePathList(manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE), packages, ",");

    // Add the default set of packages
    mergePathList(DEFAULT_IMPORT_PACKAGE_LIST, packages, ",");

    // Analyse the bytecode of any .class files in the jar to find any other
    // required imports
    if (!!!importPackages.isEmpty()) {
      StringBuffer generatedImports = new StringBuffer();
      for (String entry : importPackages) {
        generatedImports.append(',');
        generatedImports.append(entry);
        generatedImports.append(";resolution:=optional");
      }      
      
      mergePathList(generatedImports.substring(1), packages, ",");
    }

    // Construct the string and set it into the properties
    StringBuffer importValues = new StringBuffer();
    for (String entry : packages) {
      importValues.append(",");
      importValues.append(entry);
    }
    if (!packages.isEmpty()) {
      properties.put(Constants.IMPORT_PACKAGE, importValues.toString().substring(1));
    }
     
    // Take the properties map and add them to the manifest file
    for (Map.Entry<String, String> entry : properties.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        manifest.getMainAttributes().putValue(key, value);
    }
    
    //
    // Bundle-SymbolicName
    //

    if (manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) == null) {
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, warName + "_" + manifest.hashCode());
    }
    
    return manifest;
  }

  private static String addSlash(String contextPath) {
      if (!contextPath.startsWith("/")) {
          contextPath = "/" + contextPath;
      }
      return contextPath;
  }
  
  // pathlist = A "delim" delimitted list of path entries
  private static void mergePathList(String pathlist, ArrayList<String> paths, String delim) {
      if (pathlist != null) {
          List<String> tokens = parseDelimitedString(pathlist, delim, true);
          for (String token : tokens) {
              if (!paths.contains(token)) {
                  paths.add(token);
              }
          }
      }
  }
  
  private static List<String> parseDelimitedString(String value, String delim, boolean includeQuotes) {   
      if (value == null) {       
          value = "";
      }

      List<String> list = new ArrayList<String>();

      int CHAR = 1;
      int DELIMITER = 2;
      int STARTQUOTE = 4;
      int ENDQUOTE = 8;

      StringBuffer sb = new StringBuffer();

      int expecting = (CHAR | DELIMITER | STARTQUOTE);

      for (int i = 0; i < value.length(); i++) {        
          char c = value.charAt(i);

          boolean isDelimiter = (delim.indexOf(c) >= 0);
          boolean isQuote = (c == '"');

          if (isDelimiter && ((expecting & DELIMITER) > 0)) {            
              list.add(sb.toString().trim());
              sb.delete(0, sb.length());
              expecting = (CHAR | DELIMITER | STARTQUOTE);
          } else if (isQuote && ((expecting & STARTQUOTE) > 0)) { 
              if (includeQuotes) {
                  sb.append(c);
              }
              expecting = CHAR | ENDQUOTE;
          } else if (isQuote && ((expecting & ENDQUOTE) > 0)) {    
              if (includeQuotes) {
                  sb.append(c);
              }
              expecting = (CHAR | STARTQUOTE | DELIMITER);
          } else if ((expecting & CHAR) > 0) {            
              sb.append(c);
          } else {
              throw new IllegalArgumentException("Invalid delimited string: " + value);
          }
      }

      if (sb.length() > 0) {        
          list.add(sb.toString().trim());
      }

      return list;
  }
  
  private static boolean removeDigestAttributes(Manifest manifest) {
      boolean foundDigestAttribute = false;
      for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
          Attributes attributes = entry.getValue();
          for (Object attributeName : attributes.keySet()) {    
              String name = ((Attributes.Name) attributeName).toString();
              name = name.toLowerCase();
              if (name.endsWith("-digest") || name.contains("-digest-")) {
                  attributes.remove(attributeName);
                  foundDigestAttribute = true;
              }
          }
      }
      return foundDigestAttribute;
  }
  
  private static boolean isSignatureFile(String entryName) {
      String[] parts = entryName.split("/");
      if (parts.length == 2) {
          String name = parts[1].toLowerCase();
          return (parts[0].equals("META-INF") &&
                  (name.endsWith(".sf") || 
                   name.endsWith(".dsa") || 
                   name.endsWith(".rsa") || 
                   name.startsWith("sig-")));
      } else {
          return false;
      }
  }
  
  public InputStream getWAB() throws IOException {
    convert();
    return wab.getInputStream();
  }
  
  public Manifest getWABManifest() throws IOException {
    generateManifest();
    return wabManifest;
  }

  public int getWabLength() throws IOException {
    convert();
    return wab.size();
  }
  
}

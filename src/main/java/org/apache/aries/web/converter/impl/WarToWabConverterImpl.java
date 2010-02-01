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
import static org.apache.aries.web.converter.WarToWabConverter.WEB_JSP_EXTRACT_LOCATION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.aries.web.converter.WarToWabConverter.InputStreamProvider;
import org.objectweb.asm.ClassReader;
import org.osgi.framework.Constants;

public class WarToWabConverterImpl {
  private static final String DEFAULT_BUNDLE_VERSION = "1.0";
  private static final String DEFAULT_BUNDLE_MANIFESTVERSION = "2";
  private static final String INITIAL_CLASSPATH_ENTRY = "WEB-INF/classes";
  private static final String CLASSPATH_LIB_PREFIX = "WEB-INF/lib/";
  private static final String DEFAULT_IMPORT_PACKAGE_LIST = "javax.servlet;version=2.5,"
      + "javax.servlet.http;version=2.5,"
      + "javax.el;version=2.1,"
      + "javax.servlet.jsp;version=2.1,"
      + "javax.servlet.jsp.el;version=2.1,"
      + "javax.servlet.jsp.tagext;version=2.1";

  private static final String DEFAULT_WEB_CONTEXT_PATH = "/";
  private static final String DEFAULT_WEB_JSP_EXTRACT_LOCATION = "/";

  private Properties properties;

  // InputStream for the new WAB file
  private byte[] wabFile;
  private Manifest wabManifest;
  private String warName;
  private InputStreamProvider input;
  
  private boolean converted = false;

  // State used for updating the manifest
  private Set<String> importPackages;
  private Set<String> exemptPackages;
  private Map<String, Manifest> manifests; 
  private ArrayList<String> classPath;

  public WarToWabConverterImpl(InputStreamProvider warFile, String name, Properties properties) throws IOException {
    this.properties = properties;
    classPath = new ArrayList<String>();
    importPackages = new HashSet<String>();
    exemptPackages = new HashSet<String>();
    input = warFile;
    this.warName = name;
  }
  
  private void convert() throws IOException {

    ZipEntry entry;
    JarInputStream jarInput = null;

    try {
      jarInput = new JarInputStream(input.getInputStream());
      scanForDependencies(jarInput);

      // Add the new properties to the manifest byte stream
      Manifest manifest = jarInput.getManifest();
      wabManifest = updateManifest(manifest);
    } 
    finally {
      try { if (jarInput != null) jarInput.close(); } catch (IOException e) { e.printStackTrace(); }
    }

    // Create a new jar file in memory with the new manifest and the old data
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    JarOutputStream jarOutput = null;
    jarInput = null;

    // Copy across all entries from the original jar
    int val;
    try {
      jarOutput = new JarOutputStream(output, wabManifest);
      jarInput = new JarInputStream(input.getInputStream());
      while ((entry = jarInput.getNextEntry()) != null) {
        jarOutput.putNextEntry(entry);
        while ((val = jarInput.read()) != -1)
          jarOutput.write(val);
      }
    }
    finally {
      if (jarOutput != null)
        jarOutput.close();
      if (jarInput != null)
        jarInput.close();
    }

    // Create a stream to the in-memory jar
    wabFile = output.toByteArray();
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

  protected Manifest updateManifest(Manifest manifest) throws IOException
  {
    // If for some reason no manifest was generated, we start our own so that we don't null pointer later on
    if (manifest == null) {
      manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1");
    }
    
    // Compare the manifest and the supplied properties

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

    if (manifest.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) == null
        && !properties.containsKey(Constants.BUNDLE_MANIFESTVERSION)) {
      properties.put(Constants.BUNDLE_MANIFESTVERSION,
          DEFAULT_BUNDLE_MANIFESTVERSION);
    }

    //
    // Bundle-SymbolicName
    //

    if (manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) == null
        && !properties.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
      properties.put(Constants.BUNDLE_SYMBOLICNAME, warName + "_"
          + manifest.hashCode());
    }

    // 
    // Bundle-ClassPath
    //

    ArrayList<String> classpath = new ArrayList<String>();

    // Set initial entry into classpath
    classpath.add(INITIAL_CLASSPATH_ENTRY);

    // Add any files from the WEB-INF/lib directory + their dependencies
    classpath.addAll(classPath);
    
    // Get the list from the URL and add to classpath (removing duplicates)
    mergePathList(properties.getProperty(Constants.BUNDLE_CLASSPATH),
        classpath, ",");

    // Get the existing list from the manifest file and add to classpath
    // (removing duplicates)
    mergePathList(manifest.getMainAttributes().getValue(
        Constants.BUNDLE_CLASSPATH), classpath, ",");

    // Construct the classpath string and set it into the properties
    StringBuffer classPathValue = new StringBuffer();
    for (String entry : classpath) {
      classPathValue.append(",");
      classPathValue.append(entry);
    }

    if (!classpath.isEmpty())
      properties.put(Constants.BUNDLE_CLASSPATH, classPathValue.toString()
          .substring(1));

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
    mergePathList(properties.getProperty(Constants.IMPORT_PACKAGE), packages,
        ",");

    // Get the existing list from the manifest file and add to classpath
    // (removing duplicates)
    mergePathList(manifest.getMainAttributes().getValue(
        Constants.IMPORT_PACKAGE), packages, ",");

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
    if (!packages.isEmpty())
      properties.put(Constants.IMPORT_PACKAGE, importValues.toString()
          .substring(1));

    //
    // Web-ContextPath
    //

    String webCPath = properties.getProperty(WEB_CONTEXT_PATH);
    if (webCPath == null) {
        webCPath = manifest.getMainAttributes().getValue(WEB_CONTEXT_PATH);
    }
    if (webCPath == null) {
        properties.put(WEB_CONTEXT_PATH, DEFAULT_WEB_CONTEXT_PATH);
    } else {
        // always ensure context path starts with slash
        if (!webCPath.startsWith("/")) {
            webCPath = "/" + webCPath;
        }
        properties.put(WEB_CONTEXT_PATH, webCPath);
    }

    //
    // Web-JSPExtractLocation
    //

    if (manifest.getMainAttributes().getValue(WEB_JSP_EXTRACT_LOCATION) == null
        && !properties.containsKey(WEB_JSP_EXTRACT_LOCATION)) {
      properties
          .put(WEB_JSP_EXTRACT_LOCATION, DEFAULT_WEB_JSP_EXTRACT_LOCATION);
    }

    // Take the properties map and add them to the manifest file
    for (Object s : properties.keySet())
      manifest.getMainAttributes().put(new Attributes.Name((String) s), properties.get(s));
    
    return manifest;
  }

  // pathlist = A "delim" delimitted list of path entries
  public static void mergePathList(String pathlist, ArrayList<String> classpath,
      String delim) {
    if (pathlist != null) {
      StringTokenizer tok = new StringTokenizer(pathlist, delim);
      while (tok.hasMoreTokens()) {
        String token = tok.nextToken().trim();
        if (!classpath.contains(token))
          classpath.add(token);
      }
    }
  }

  public InputStream getWAB() throws IOException {
    ensureConverted();
    return new ByteArrayInputStream(wabFile);
  }
  
  public Manifest getWABManifest() throws IOException {
    ensureConverted();
    return wabManifest;
  }

  public int getWabLength() throws IOException {
    ensureConverted();
    return wabFile.length;
  }
  
  private void ensureConverted() throws IOException {
    if (!!!converted) {
      convert();
      converted = true;
    }
  }

}

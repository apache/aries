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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.versioning.check;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.versioning.utils.BinaryCompatibilityStatus;
import org.apache.aries.versioning.utils.MethodDeclaration;
import org.apache.aries.versioning.utils.SemanticVersioningClassVisitor;
import org.apache.aries.versioning.utils.SemanticVersioningUtils;
import org.objectweb.asm.ClassReader;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticVersioningChecker {

  private static final Logger _logger = LoggerFactory.getLogger(SemanticVersioningChecker.class);
  private URLClassLoader newJarsLoader;
  private URLClassLoader oldJarsLoader;
  private final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
  /**
   * This method is to scan the current location against base and process each individual jars 
   * in the current location against the jar with the same symbolic names in the base and produce a xml report specified by versioningReport.
   * @param base baseline jars
   * @param current current version of jars
   * @param versioningReport the validation reports
   */
  public static void checkSemanticVersioning(URL base, URL current, File versioningReport) {
    //For each jar under the current location, open the jar and then use ASM to 
    //work out whether there are binary incompatible changes against the base location.
    try {
      File baseDir = new File(base.toURI());
      File currentDir = new File(current.toExternalForm());
      if ( baseDir.exists() && currentDir.exists()) {
        new SemanticVersioningChecker().performVersioningCheck(FileSystem.getFSRoot(baseDir), FileSystem.getFSRoot(currentDir), versioningReport);
      } else {
        _logger.debug("No bundles found to process.");
      }
    } catch (URISyntaxException use) {
      _logger.error(use.getMessage());
    }
  }

  // for each jar, open its manifest and found the packages
  private  void  performVersioningCheck(IDirectory  baseDir, IDirectory currentDir, File versionStatusFile)  {
    
    FileWriter versionStatusFileWriter = null;
    try {
      versionStatusFileWriter = new FileWriter(versionStatusFile, false);
      Map<String, BundleInfo> baseBundles = new HashMap<String, BundleInfo>();
      Map<String, BundleInfo> currentBundles = new HashMap<String, BundleInfo>();
      
        //scan each individual bundle and find the corresponding bundle in the baseline and verify the version changes
        currentBundles = getBundles(currentDir);
        baseBundles = getBundles(baseDir);
        URL[] newJarURLs = getListURLs(currentBundles.values()).toArray(new URL[0]);
        newJarsLoader = new URLClassLoader(newJarURLs);

        URL[] oldJarURLs = getListURLs(baseBundles.values()).toArray(new URL[0]);

        oldJarsLoader = new URLClassLoader(oldJarURLs);

        //Write the xml header
        writeRecordToWriter(versionStatusFileWriter, xmlHeader + "\r\n");
        
        // write the comparison base and current level into the file
        writeRecordToWriter(versionStatusFileWriter, "<semanaticVersioning currentDir= \"" + currentDir + "\" baseDir = \"" + baseDir + "\">");
        for (Map.Entry<String, BundleInfo> entry : currentBundles.entrySet()) {
          String bundleSymbolicName = entry.getKey();

          String bundleElement = null;
          boolean bundleVersionCorrect = true;
          // find the same bundle in the base and check whether all the versions are correct
          BundleInfo currentBundle = entry.getValue();
          BundleInfo baseBundle = baseBundles.get(bundleSymbolicName);
          StringBuilder pkgElements = new StringBuilder();
          String reason = null;
          if (baseBundle == null) {
            _logger.debug("The bundle " + bundleSymbolicName + " has no counterpart in the base. The semanit version validation does not apply to this bundle.");
          } else {
            // open the manifest and scan the export package and find the package name and exported version
            // The tool assume the one particular package just exports under one version
            Map<String, PackageContent> currBundleExpPkgContents = getAllExportedPkgContents(currentBundle);
            Map<String, PackageContent> baseBundleExpPkgContents = new HashMap<String, PackageContent>();
            boolean pkg_major_change = false;
            boolean pkg_minor_change = false;
            String fatal_package = null;
            if (!!!currBundleExpPkgContents.isEmpty()) {
              baseBundleExpPkgContents =  getAllExportedPkgContents(baseBundle);
              // compare each class right now
              for (Map.Entry<String, PackageContent> pkg : baseBundleExpPkgContents.entrySet()) {
                String pkgName = pkg.getKey();
                Map<String, IFile> baseClazz = pkg.getValue().getClasses();
                Map<String, IFile> baseXsds = pkg.getValue().getXsds();
                PackageContent currPkgContents = currBundleExpPkgContents.get(pkgName);
                if (currPkgContents == null) {
                  // The package is no longer exported any more. This should lead to bundle major version change.
                  pkg_major_change = true;
                  fatal_package = pkgName;
                  _logger.debug("The package " + pkgName + " in the bundle of " + bundleSymbolicName + " is no longer to be exported. Major change.");
                } else {
                  Map<String, IFile> curClazz = currPkgContents.getClasses();
                  Map<String, IFile> curXsds = currPkgContents.getXsds();
                  //check whether there should be major change/minor change/micro change in this package.
                  //1. Use ASM to visit all classes in the package 
                  VersionChange majorChange = new VersionChange();
                  VersionChange minorChange = new VersionChange();
                  // check all classes to see whether there are minor or major changes
                  visitPackage(pkgName, baseClazz, curClazz, majorChange, minorChange);
                  // If there is no binary compatibility changes, check whether xsd files have been added, changed or deleted
                  if (!!!majorChange.isChange()) {
                    checkXsdChangesInPkg(pkgName, baseXsds, curXsds, majorChange);
                    // If everything is ok with the existing classes. Need to find out whether there are more API (abstract classes) in the current bundle.
                    // loop through curClazz and visit it and find out whether one of them is abstract.
                    // check whether there are more xsd or abstract classes added
                    if (!!!(majorChange.isChange() || minorChange.isChange())){
                      checkAdditionalClassOrXsds(pkgName, curClazz, curXsds, minorChange);
                    }
                  }
                  // We have scanned the whole packages, report the result
                  if (majorChange.isChange() || minorChange.isChange()) {
                    String oldVersion = pkg.getValue().getPackageVersion();
                    String newVersion = currPkgContents.getPackageVersion();
                    if (majorChange.isChange()) {
                      pkg_major_change = true;
                      fatal_package = pkgName;
                      if (!!!isVersionCorrect(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion)) {                      
                        pkgElements.append(getPkgStatusText( pkgName, VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion, majorChange.getReason(), majorChange.getChangeClass()));                      
                      }
                    } else if (minorChange.isChange()) {
                      pkg_minor_change = true;
                      if (fatal_package == null) fatal_package = pkgName;
                      if (!!!isVersionCorrect(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion)) {
                        pkgElements.append(getPkgStatusText( pkgName, VERSION_CHANGE_TYPE.MINOR_CHANGE, pkg.getValue().getPackageVersion(), currPkgContents.getPackageVersion(), minorChange.getReason(), minorChange.getChangeClass()));
                      }
                    }
                    pkgElements.append("\r\n");
                  }
                }
              }
              // If there is a package version change, the bundle version needs to be updated.
              // If there is a major change in one of the packages, the bundle major version needs to be increased.
              // If there is a minor change in one of the packages, the bundle minor version needs to be increased.
              String oldVersion = baseBundle.getBundleManifest().getVersion().toString();
              String newVersion = currentBundle.getBundleManifest().getVersion().toString();


              if (pkg_major_change || pkg_minor_change) {

                if (pkg_major_change) {
                  // The bundle version's major value should be increased.
                  reason = "Some packages have major changes. For an instance, the package " + fatal_package + " has major version changes.";
                  bundleElement = getBundleStatusText(currentBundle.getBundle().getName(), bundleSymbolicName, VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion, reason);
                  bundleVersionCorrect = isVersionCorrect(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion);
                } else if (pkg_minor_change) {
                  reason = "Some packages have minor changes. For an instance, the package " + fatal_package + " has minor version changes.";
                  bundleElement = getBundleStatusText(currentBundle.getBundle().getName(), bundleSymbolicName, VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion, reason);
                  bundleVersionCorrect = isVersionCorrect(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion);
                } 
              } else {
                reason = "The bundle has no version changes.";
                bundleElement = getBundleStatusText(currentBundle.getBundle().getName(), bundleSymbolicName, VERSION_CHANGE_TYPE.NO_CHANGE, oldVersion, newVersion, reason);
                bundleVersionCorrect = isVersionCorrect(VERSION_CHANGE_TYPE.NO_CHANGE, oldVersion, newVersion);
              }
            }
          }
          // Need to write bundle element and then package elements
          if ((!!!bundleVersionCorrect) || ((pkgElements.length() > 0) )) {
            writeRecordToWriter(versionStatusFileWriter, bundleElement);
            writeRecordToWriter(versionStatusFileWriter, pkgElements.toString());
            writeRecordToWriter(versionStatusFileWriter, "</bundle>");
          }
        }
        writeRecordToWriter(versionStatusFileWriter, "</semanaticVersioning>");
      
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      IOUtils.close(versionStatusFileWriter);
    }
    
    return ;
  }
  /**
   * Check whether the package has gained additional class or xsd files. If yes, log a minor change.
   * @param pkgName
   * @param curClazz
   * @param curXsds
   * @param minorChange
   */
  private void checkAdditionalClassOrXsds(String pkgName, Map<String, IFile> curClazz,
      Map<String, IFile> curXsds, VersionChange minorChange)
  {
    String reason;
    Collection<IFile> ifiles = curClazz.values();
    Iterator<IFile> iterator = ifiles.iterator();
    while (iterator.hasNext()) {
      IFile ifile = iterator.next();
      String changeClass = ifile.getName();
      SemanticVersioningClassVisitor cv = getVisitor(ifile, newJarsLoader);
      if (cv.getClassDeclaration() != null) {
        // If this is a public/protected class, it will need to increase the minor version of the package.
        minorChange.setChange(true);
        if (minorChange.isChange()) {
          reason = "The package " + pkgName + " has gained at least one class : " + getClassName(changeClass)+ ".";
          minorChange.update(reason, changeClass);
          break;
        }
      }
    }
    if (!!!(minorChange.isChange() || curXsds.isEmpty())) {
      /// a new xsd file was added, it is a minor change
      IFile firstXsd = null;
      Iterator<IFile> xsdIterator = curXsds.values().iterator();
      firstXsd = xsdIterator.next();

      reason = "In the package " + pkgName + ", The schema file(s) are added: " + curXsds.keySet()+ ".";
      minorChange.update(reason, firstXsd.getName());
    }
  }


  /**
   * Check whether the package has xsd file changes or deleted. If yes, log a minor change.
   * @param pkgName
   * @param baseXsds
   * @param curXsds
   * @param majorChange
   * @throws IOException
   */

  private void checkXsdChangesInPkg(String pkgName, Map<String, IFile> baseXsds,
      Map<String, IFile> curXsds, VersionChange majorChange) throws IOException
      {
    String reason;
    for (Map.Entry<String, IFile> file : baseXsds.entrySet()) {
      // scan the latest version of the class
      IFile curXsd = curXsds.get(file.getKey());
      String changeClass = file.getValue().getName();
      // check whether the xsd have been deleted or changed or added
      if (curXsd == null) {  
        reason = "In the package " + pkgName + ", The schema file has been deleted: " + file.getKey()+ ".";
        majorChange.update(reason, changeClass);
        break;                      
      } else {
        // check whether it is the same
        //read the current xsd file
        curXsds.remove(file.getKey());
        String curFileContent = readXsdFile(curXsd.open());
        String oldFileContent = readXsdFile(file.getValue().open());
        if (!!!(curFileContent.equals(oldFileContent))) {

          reason = "In the package " + pkgName + ", The schema file has been updated: " + file.getKey()+ ".";
          majorChange.update(reason, changeClass);
          break;
        }
      }
    }
      }

  /**
   * Visit the whole package to scan each class to see whether we need to log minor or major changes.
   * @param pkgName
   * @param baseClazz
   * @param curClazz
   * @param majorChange
   * @param minorChange
   */
  private void visitPackage(String pkgName, Map<String, IFile> baseClazz,
      Map<String, IFile> curClazz, VersionChange majorChange, VersionChange minorChange)
  {
    String reason;
    for (Map.Entry<String, IFile> file : baseClazz.entrySet()) {
      // scan the latest version of the class
      IFile curFile = curClazz.get(file.getKey());
      String changeClass = file.getValue().getName();
      //Scan the base version
      SemanticVersioningClassVisitor oldcv = getVisitor(file.getValue(), oldJarsLoader);
      if ((oldcv.getClassDeclaration() != null)) {
        if (curFile == null) {
          // the class we are scanning has been deleted from the current version of WAS
          // This should be a major increase
          reason = "In the package " + pkgName + ", The class/interface has been deleted: " + getClassName(changeClass) + ".";
          majorChange.update(reason, changeClass);

          break;
        } else {
          // check for binary compatibility
          // load the class from the current version of WAS
          // remove it from the curClazz collection as we would like to know whether there are more classes added
          curClazz.remove(file.getKey());
          SemanticVersioningClassVisitor newcv = getVisitor(curFile, newJarsLoader);
          // check for binary compatibility
          BinaryCompatibilityStatus bcs = newcv.getClassDeclaration().getBinaryCompatibleStatus(oldcv.getClassDeclaration());
          if (bcs.isCompatible()) {
            // check to see whether there are extra methods
            MethodDeclaration extraMethod = newcv.getClassDeclaration().getExtraMethods(oldcv.getClassDeclaration());
            if (extraMethod != null) {
              // This should be a minor increase
              reason = "In the package " + pkgName + ", the " + SemanticVersioningUtils.getReadableMethodSignature(extraMethod.getName(), extraMethod.getDesc()) + " has been added to the class or its super classes/interfaces " + getClassName(changeClass) + ".";
              minorChange.update(reason, changeClass);
            }
          } else {
            // break binary compatibility 
            reason = "In the package " + pkgName + ", binary incompatibility occurred in the class " + getClassName(changeClass)+ ". " + bcs.getReason();
            majorChange.update(reason, changeClass);
            break; 
          }
        }
      }
    }
  }

  private String readXsdFile(InputStream is) {
    BufferedReader br = new BufferedReader( new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line = null;
    try {
      while(( line = br.readLine()) != null) {
        sb.append(line);
      }
    } catch (IOException ioe) {
      IOUtils.close(br);
    }
    return sb.toString();
  }
  private void writeRecordToWriter(FileWriter fileWriter, String stringToWrite) throws IOException {
    if (fileWriter != null) {
      fileWriter.append(stringToWrite);
      fileWriter.append("\r\n");
    }
  }
  private Map<String, PackageContent> getAllExportedPkgContents(BundleInfo currentBundle)
  {
    String packageExports = currentBundle.getBundleManifest().getRawAttributes().getValue(Constants.EXPORT_PACKAGE);
    List<NameValuePair> exportPackageLists = ManifestHeaderProcessor.parseExportString(packageExports);
    // only perform validation if there are some packages exported. Otherwise, not interested.
    Map<String, PackageContent> exportedPackages = new HashMap<String, PackageContent>();
    if (!!!exportPackageLists.isEmpty()) {
      File bundleFile = currentBundle.getBundle();
      IDirectory bundleDir = FileSystem.getFSRoot(bundleFile);
      for (NameValuePair exportedPackage : exportPackageLists) {
        String packageName = exportedPackage.getName();
        String packageVersion = exportedPackage.getAttributes().get(Constants.VERSION_ATTRIBUTE);
        // need to go through each package and scan every class
        exportedPackages.put(packageName, new PackageContent(packageName, packageVersion));
      }
      // scan the jar and list all the files under each package
      List<IFile> allFiles = bundleDir.listAllFiles();
      for (IFile file : allFiles) {
        String directoryFullPath = file.getName(); 
        String directoryName = null;
        String fileName = null;
        if (file.isFile() && ((file.getName().endsWith(SemanticVersioningUtils.classExt) ||(file.getName().endsWith(SemanticVersioningUtils.schemaExt))))) {
          if (directoryFullPath.lastIndexOf("/") != -1) {
            directoryName = directoryFullPath.substring(0, directoryFullPath.lastIndexOf("/"));
            fileName = directoryFullPath.substring(directoryFullPath.lastIndexOf("/") + 1);
          }
        } 

        if (directoryName != null) {
          String pkgName = directoryName.replaceAll("/", ".") ;
          PackageContent pkgContent = exportedPackages.get(pkgName);
          if (pkgContent != null) {
            if (file.getName().endsWith(SemanticVersioningUtils.classExt)) {
              pkgContent.addClass(fileName, file);
            } else {
              pkgContent.addXsd(fileName, file);
            }
            exportedPackages.put(pkgName, pkgContent);
          }
        }
      }
    }
    return exportedPackages;
  }

  private Map<String, BundleInfo> getBundles(IDirectory ds)
  {
    Map<String, BundleInfo> bundles = new HashMap<String, BundleInfo>();
    List<IFile> includedFiles = ds.listAllFiles();

    for (IFile ifile : includedFiles) {


      
      if (ifile.getName().endsWith(SemanticVersioningUtils.jarExt)) {
        // scan its manifest
        try {
        BundleManifest manifest = BundleManifest.fromBundle(ifile.open());
        // find the bundle symbolic name, store them in a map with bundle symbolic name as key, bundleInfo as value
        if (manifest.getSymbolicName() != null) {
          
          bundles.put(manifest.getSymbolicName(), new BundleInfo(manifest, new File(ifile.toURL().getPath())));
          
        }
        } catch (MalformedURLException mue) {
          _logger.debug("Exception thrown when processing" + ifile.getName(), mue);
        } catch (IOException ioe) {
          _logger.debug("Exception thrown when processing" + ifile.getName(), ioe);
        }
      }
    }
    return bundles;
  }

  private String getStatusText( VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr) {

    Version oldVersion = Version.parseVersion(oldVersionStr);
    Version newVersion = Version.parseVersion(newVersionStr);
    Version recommendedVersion = Version.parseVersion(oldVersionStr);
    if (status == VERSION_CHANGE_TYPE.MAJOR_CHANGE) {
      recommendedVersion = new Version(oldVersion.getMajor() + 1, 0, 0);
    } else if (status == VERSION_CHANGE_TYPE.MINOR_CHANGE) {
      recommendedVersion = new Version(oldVersion.getMajor(), oldVersion.getMinor() + 1,  0);
    } else {
      recommendedVersion = oldVersion;
    }
    return " oldVersion=\"" + oldVersion 
    + "\" currentVersion=\"" + newVersion +
    "\" recommendedVersion=\"" + recommendedVersion + "\" correct=\"" + isVersionCorrect(status, oldVersionStr, newVersionStr);
  }

  private String transformForXml(String str){
    str = str.replaceAll("<", "&lt;");
    str = str.replaceAll(">", "&gt;");
    return str;
  }
  private boolean isVersionCorrect(VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr) {
    boolean versionCorrect = false;

    Version oldVersion = Version.parseVersion(oldVersionStr);
    Version newVersion = Version.parseVersion(newVersionStr);

    if (status == VERSION_CHANGE_TYPE.MAJOR_CHANGE) {
      if (newVersion.getMajor() > oldVersion.getMajor()) {
        versionCorrect = true;
      }
    } else if (status == VERSION_CHANGE_TYPE.MINOR_CHANGE){
      if ((newVersion.getMajor() > oldVersion.getMajor()) || ( newVersion.getMinor() > oldVersion.getMinor())) {
        versionCorrect = true;
      }
    } else {
      if ((newVersion.getMajor() == oldVersion.getMajor()) && (newVersion.getMinor() == oldVersion.getMinor())) {
        versionCorrect = true;
      }
    }
    return versionCorrect;
  }

  private String getPkgStatusText(String pkgName,  VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr,  String reason, String key_class) {

    String modified_key_class = key_class;
    if (key_class.endsWith(SemanticVersioningUtils.classExt)) {
      modified_key_class = key_class.substring(0, key_class.lastIndexOf(SemanticVersioningUtils.classExt)) + SemanticVersioningUtils.javaExt;
    }

    return   "<package name=\"" + pkgName + "\"" +  getStatusText(status, oldVersionStr, newVersionStr) + "\" reason=\""  +transformForXml(reason) + "\" key_class=\"" + modified_key_class +  "\" change=\"" + status.text() + "\"/>";
  }

  private String getBundleStatusText(String bundleFileName, String bundleSymbolicName, VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr, String reason) {
    return "<bundle fileName=\"" + bundleFileName + "\" bundleSymbolicName =\"" + bundleSymbolicName + "\"" + getStatusText(status, oldVersionStr, newVersionStr) + "\" reason=\"" + transformForXml(reason) + "\" change=\"" + status.text() + "\">";
  }

  private SemanticVersioningClassVisitor getVisitor(IFile file, URLClassLoader loader ) {
    SemanticVersioningClassVisitor oldcv = new SemanticVersioningClassVisitor(loader);
    try {
      ClassReader cr = new ClassReader(file.open());
      cr.accept(oldcv, 0);
    } catch (IOException ioe) {
      _logger.debug("The file " + file + "cannot be opened.");
    }
    return oldcv; 
  }

  private Collection<URL> getListURLs(Collection<BundleInfo> bundles){
    Collection<URL> urls = new HashSet<URL>();
    try {
      for (BundleInfo bundle : bundles) {

        URL url = bundle.getBundle().toURI().toURL();
        urls.add(url);

      }

    } catch (MalformedURLException e) {
      _logger.debug(e.getMessage());
    }
    return urls;
  }

  private String getClassName(String fullClassPath) {
    String[] chunks = fullClassPath.split("/");
    String className = chunks[chunks.length - 1];
    className = className.replace(SemanticVersioningUtils.classExt, SemanticVersioningUtils.javaExt);
    return className;
  }

  private class BundleInfo {
    private final BundleManifest bundleManifest;
    private final File bundle;
    BundleInfo(BundleManifest bm, File bundle) {
      this.bundleManifest =bm;
      this.bundle = bundle;
    }
    public BundleManifest getBundleManifest()
    {
      return bundleManifest;
    }
    public File getBundle()
    {
      return bundle;
    }
  }
  enum VERSION_CHANGE_TYPE { MAJOR_CHANGE("major"), MINOR_CHANGE("minor"), NO_CHANGE("no");
  private final String text;
  VERSION_CHANGE_TYPE(String text) {
    this.text = text;
  }
  public String text() {
    return this.text;
  }

  };

  private class PackageContent {
    private final String  packageName;
    private final String packageVersion;
    private final Map<String, IFile> classes = new HashMap<String, IFile>();
    private final Map<String, IFile> xsds = new HashMap<String, IFile>();
    PackageContent(String pkgName, String pkgVersion) {
      packageName = pkgName;
      packageVersion = pkgVersion;
    }
    public void addClass(String className, IFile file) {
      classes.put(className, file);
    }
    public void addXsd(String className, IFile file) {
      xsds.put(className, file);
    }
    public Map<String, IFile> getClasses () {
      return classes;
    }
    public Map<String, IFile> getXsds () {
      return xsds;
    }

    public String getPackageVersion()
    {
      return packageVersion;
    }
    public String getPackageName() {
      return packageName;
    }
  }

  private class VersionChange {
    boolean change = false;
    String reason= null;
    String changeClass = null;
    public boolean isChange()
    {
      return change;
    }
    public void setChange(boolean change)
    {
      this.change = change;
    }
    public String getReason()
    {
      return reason;
    }
    public String getChangeClass()
    {
      return changeClass;
    }
    public void update(String reason, String changeClass) {
      this.change = true;
      this.reason = reason;
      this.changeClass = changeClass;
    }
  }

}

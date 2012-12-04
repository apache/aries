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


package org.apache.aries.versioning.check;

import static org.apache.aries.versioning.utils.SemanticVersioningUtils.oneLineBreak;
import static org.apache.aries.versioning.utils.SemanticVersioningUtils.twoLineBreaks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.filesystem.IFile;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.versioning.utils.BinaryCompatibilityStatus;
import org.apache.aries.versioning.utils.ClassDeclaration;
import org.apache.aries.versioning.utils.FieldDeclaration;
import org.apache.aries.versioning.utils.MethodDeclaration;
import org.apache.aries.versioning.utils.SemanticVersioningClassVisitor;
import org.apache.aries.versioning.utils.SemanticVersioningUtils;
import org.objectweb.asm.ClassReader;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @version $Rev:$ $Date:$
*/
public class BundleCompatibility {
    private static final Logger _logger = LoggerFactory.getLogger(BundleCompatibility.class);


    private URLClassLoader oldJarsLoader;
    private URLClassLoader newJarsLoader;
    private String bundleSymbolicName;
    private String bundleElement;
    private boolean bundleVersionCorrect;
    private BundleInfo currentBundle;
    private BundleInfo baseBundle;
    private StringBuilder pkgElements = new StringBuilder();

    private VersionChange bundleChange;
    private final Map<String, VersionChange> packageChanges = new HashMap<String, VersionChange>();

    public BundleCompatibility(String bundleSymbolicName, BundleInfo currentBundle, BundleInfo baseBundle, URLClassLoader oldJarsLoader, URLClassLoader newJarsLoader) {
        this.bundleSymbolicName = bundleSymbolicName;
        this.currentBundle = currentBundle;
        this.baseBundle = baseBundle;
        this.oldJarsLoader = oldJarsLoader;
        this.newJarsLoader = newJarsLoader;
    }

    public VersionChange getBundleChange() {
        return bundleChange;
    }

    public Map<String, VersionChange> getPackageChanges() {
        return packageChanges;
    }

    public String getBundleElement() {
        return bundleElement;
    }

    public StringBuilder getPkgElements() {
        return pkgElements;
    }

    public boolean isBundleVersionCorrect() {
        return bundleVersionCorrect;
    }

    public BundleCompatibility invoke() throws IOException {
        String reason = null;
        // open the manifest and scan the export package and find the package name and exported version
        // The tool assume the one particular package just exports under one version
        Map<String, PackageContent> currBundleExpPkgContents = getAllExportedPkgContents(currentBundle);
        Map<String, PackageContent> baseBundleExpPkgContents;
        boolean pkg_major_change = false;
        boolean pkg_minor_change = false;
        String fatal_package = null;
        if (!!!currBundleExpPkgContents.isEmpty()) {
            baseBundleExpPkgContents = getAllExportedPkgContents(baseBundle);
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
                    VersionChangeReason majorChange = new VersionChangeReason();
                    VersionChangeReason minorChange = new VersionChangeReason();
                    // check all classes to see whether there are minor or major changes
                    visitPackage(pkgName, baseClazz, curClazz, majorChange, minorChange);
                    // If there is no binary compatibility changes, check whether xsd files have been added, changed or deleted
                    if (!!!majorChange.isChange()) {
                        checkXsdChangesInPkg(pkgName, baseXsds, curXsds, majorChange);
                        // If everything is ok with the existing classes. Need to find out whether there are more API (abstract classes) in the current bundle.
                        // loop through curClazz and visit it and find out whether one of them is abstract.
                        // check whether there are more xsd or abstract classes added
                        if (!!!(majorChange.isChange() || minorChange.isChange())) {
                            checkAdditionalClassOrXsds(pkgName, curClazz, curXsds, minorChange);
                        }
                    }
                    // We have scanned the whole packages, report the result
//                    if (majorChange.isChange() || minorChange.isChange()) {
                        String oldVersion = pkg.getValue().getPackageVersion();
                        String newVersion = currPkgContents.getPackageVersion();
                        if (majorChange.isChange()) {
                            packageChanges.put(pkgName, new VersionChange(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion));
                            pkg_major_change = true;
                            fatal_package = pkgName;
                            if (!!!isVersionCorrect(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion)) {
                                pkgElements.append(getPkgStatusText(pkgName, VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion, majorChange.getReason(), majorChange.getChangeClass()));
                            }
                        } else if (minorChange.isChange()) {
                            packageChanges.put(pkgName, new VersionChange(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion));
                            pkg_minor_change = true;
                            if (fatal_package == null) fatal_package = pkgName;
                            if (!!!isVersionCorrect(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion)) {
                                pkgElements.append(getPkgStatusText(pkgName, VERSION_CHANGE_TYPE.MINOR_CHANGE, pkg.getValue().getPackageVersion(), currPkgContents.getPackageVersion(), minorChange.getReason(), minorChange.getChangeClass()));
                            }
                        }  else {
                            packageChanges.put(pkgName, new VersionChange(VERSION_CHANGE_TYPE.NO_CHANGE, oldVersion, newVersion));
                            pkgElements.append(getPkgStatusText(pkgName, VERSION_CHANGE_TYPE.NO_CHANGE, pkg.getValue().getPackageVersion(), currPkgContents.getPackageVersion(), "", ""));
                        }
                        pkgElements.append("\r\n");
//                    }
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
                    bundleChange = new VersionChange(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion);
                    reason = "Some packages have major changes. For an instance, the package " + fatal_package + " has major version changes.";
                    bundleElement = getBundleStatusText(currentBundle.getBundle().getName(), bundleSymbolicName, VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion, reason);
                    bundleVersionCorrect = isVersionCorrect(VERSION_CHANGE_TYPE.MAJOR_CHANGE, oldVersion, newVersion);
                } else if (pkg_minor_change) {
                    bundleChange = new VersionChange(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion);
                    reason = "Some packages have minor changes. For an instance, the package " + fatal_package + " has minor version changes.";
                    bundleElement = getBundleStatusText(currentBundle.getBundle().getName(), bundleSymbolicName, VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion, reason);
                    bundleVersionCorrect = isVersionCorrect(VERSION_CHANGE_TYPE.MINOR_CHANGE, oldVersion, newVersion);
                }
            } else {
                bundleChange = new VersionChange(VERSION_CHANGE_TYPE.NO_CHANGE, oldVersion, newVersion);
                reason = "The bundle has no version changes.";
                bundleElement = getBundleStatusText(currentBundle.getBundle().getName(), bundleSymbolicName, VERSION_CHANGE_TYPE.NO_CHANGE, oldVersion, newVersion, reason);
                bundleVersionCorrect = isVersionCorrect(VERSION_CHANGE_TYPE.NO_CHANGE, oldVersion, newVersion);
            }
        }
        return this;
    }

    private Map<String, PackageContent> getAllExportedPkgContents(BundleInfo currentBundle) {
        String packageExports = currentBundle.getBundleManifest().getRawAttributes().getValue(Constants.EXPORT_PACKAGE);
        List<ManifestHeaderProcessor.NameValuePair> exportPackageLists = ManifestHeaderProcessor.parseExportString(packageExports);
        // only perform validation if there are some packages exported. Otherwise, not interested.
        Map<String, PackageContent> exportedPackages = new HashMap<String, PackageContent>();
        if (!!!exportPackageLists.isEmpty()) {
            File bundleFile = currentBundle.getBundle();
            IDirectory bundleDir = FileSystem.getFSRoot(bundleFile);
            for (ManifestHeaderProcessor.NameValuePair exportedPackage : exportPackageLists) {
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
                if (file.isFile() && ((file.getName().endsWith(SemanticVersioningUtils.classExt) || (file.getName().endsWith(SemanticVersioningUtils.schemaExt))))) {
                    if (directoryFullPath.lastIndexOf("/") != -1) {
                        directoryName = directoryFullPath.substring(0, directoryFullPath.lastIndexOf("/"));
                        fileName = directoryFullPath.substring(directoryFullPath.lastIndexOf("/") + 1);
                    }
                }

                if (directoryName != null) {
                    String pkgName = directoryName.replaceAll("/", ".");
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

    private String getBundleStatusText(String bundleFileName, String bundleSymbolicName, VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr, String reason) {
      if (!isVersionCorrect(status, oldVersionStr, newVersionStr)) {
        return "The bundle " + bundleSymbolicName + " has the following changes:\r\n" + reason + "\r\nThe bundle version should be " + getRecommendedVersion(status, oldVersionStr) + ".";
      } else {
        return "";
      } 
      
      
    }

    /**
     * Visit the whole package to scan each class to see whether we need to log minor or major changes.
     *
     * @param pkgName
     * @param baseClazz
     * @param curClazz
     * @param majorChange
     * @param minorChange
     */
    private void visitPackage(String pkgName, Map<String, IFile> baseClazz,
                              Map<String, IFile> curClazz, VersionChangeReason majorChange, VersionChangeReason minorChange) {
        StringBuilder major_reason = new StringBuilder();
        StringBuilder minor_reason = new StringBuilder();
        boolean is_major_change = false;
        boolean is_minor_change = false;
        String fatal_class = null;
        boolean foundNewAbstract = false;
        for (Map.Entry<String, IFile> file : baseClazz.entrySet()) {
            // scan the latest version of the class
            IFile curFile = curClazz.get(file.getKey());
            String changeClass = file.getValue().getName();
            //Scan the base version
            SemanticVersioningClassVisitor oldcv = getVisitor(file.getValue(), oldJarsLoader);
            // skip the property files as they are compiled as class file as well
            ClassDeclaration cd = oldcv.getClassDeclaration();
            if ((cd != null) && (!SemanticVersioningUtils.isPropertyFile(cd))) {

                if (curFile == null) {
                    // the class we are scanning has been deleted from the current version of WAS
                    // This should be a major increase
                    major_reason.append(twoLineBreaks + "The class/interface " + getClassName(changeClass) + " has been deleted from the package.");
                    //majorChange.update(reason, changeClass);
                    is_major_change = true;
                    // only replace the fatal class if not set as the class won't be found in cmvc due to the fact it has been deleted.
                    if (fatal_class == null) {
                        fatal_class = changeClass;
                    }
                } else {
                    // check for binary compatibility
                    // load the class from the current version of WAS
                    // remove it from the curClazz collection as we would like to know whether there are more classes added
                    curClazz.remove(file.getKey());
                    SemanticVersioningClassVisitor newcv = getVisitor(curFile, newJarsLoader);
                    // check for binary compatibility
                    ClassDeclaration newcd = newcv.getClassDeclaration();
                    BinaryCompatibilityStatus bcs = newcd.getBinaryCompatibleStatus(oldcv.getClassDeclaration());

                    if (!bcs.isCompatible()) {
                        major_reason.append(twoLineBreaks + "In the " + getClassName(changeClass) + " class or its supers, the following changes have been made since the last release.");
                        // break binary compatibility
                        for (String reason : bcs) {
                            major_reason.append(oneLineBreak).append(reason);
                        }
                        is_major_change = true;
                        fatal_class = changeClass;
                    } else {
                        //check to see whether more methods are added
                        ClassDeclaration oldcd = oldcv.getClassDeclaration();
                        Collection<MethodDeclaration> extraMethods = newcd.getExtraMethods(oldcd);

                        boolean containsConcrete = false;
                        boolean containsAbstract = false;

                        boolean abstractClass = newcd.isAbstract();

                        StringBuilder subRemarks = new StringBuilder();
                        String concreteSubRemarks = null;
                        for (MethodDeclaration extraMethod : extraMethods) {
                            //only interested in the visible methods not the system generated ones
                            if (!extraMethod.getName().contains("$")) {
                                if (abstractClass) {
                                    if (extraMethod.isAbstract()) {
                                        foundNewAbstract = true;
                                        containsAbstract = true;
                                        subRemarks.append(oneLineBreak + SemanticVersioningUtils.getReadableMethodSignature(extraMethod.getName(), extraMethod.getDesc()));
                                    } else {
                                        //only list one abstract method, no need to list all
                                        containsConcrete = true;
                                        concreteSubRemarks = oneLineBreak + SemanticVersioningUtils.getReadableMethodSignature(extraMethod.getName(), extraMethod.getDesc());
                                    }
                                } else {
                                    containsConcrete = true;
                                    concreteSubRemarks = oneLineBreak + SemanticVersioningUtils.getReadableMethodSignature(extraMethod.getName(), extraMethod.getDesc());
                                    break;
                                }
                            }
                        }

                        if (containsConcrete || containsAbstract) {
                            is_minor_change = true;
                            if (!is_major_change) {
                                fatal_class = changeClass;
                            }
                            if (containsAbstract) {

                                minor_reason.append(twoLineBreaks + "In the " + getClassName(changeClass) + " class or its supers, the following abstract methods have been added since the last release of WAS.");
                                minor_reason.append(subRemarks);
                            } else {
                                minor_reason.append(twoLineBreaks + "In the " + getClassName(changeClass) + " class or its supers, the following method has been added since the last release of WAS.");
                                minor_reason.append(concreteSubRemarks);
                            }
                        }
                        //check to see whether there are extra public/protected fields if there is no additional methods

                        if (!is_minor_change) {
                            for (FieldDeclaration field : newcd.getExtraFields(oldcd)) {
                                if (field.isPublic() || field.isProtected()) {
                                    is_minor_change = true;
                                    String extraFieldRemarks = oneLineBreak + " " + SemanticVersioningUtils.transform(field.getDesc()) + " " + field.getName();
                                    if (!is_major_change) {
                                        fatal_class = changeClass;
                                    }
                                    minor_reason.append(twoLineBreaks + "In the " + getClassName(changeClass) + " class or its supers, the following fields have been added since the last release of WAS.");
                                    minor_reason.append(extraFieldRemarks);
                                    break;
                                }
                            }

                        }

                    }
                }
            }
        }
        if (is_major_change) {
            majorChange.update(major_reason.toString(), fatal_class, false);
        }
        if (is_minor_change) {
            minorChange.update(minor_reason.toString(), fatal_class, (foundNewAbstract ? true : false));
        }
    }

    /**
     * Check whether the package has xsd file changes or deleted. If yes, log a minor change.
     *
     * @param pkgName
     * @param baseXsds
     * @param curXsds
     * @param majorChange
     * @throws java.io.IOException
     */

    private void checkXsdChangesInPkg(String pkgName, Map<String, IFile> baseXsds,
                                      Map<String, IFile> curXsds, VersionChangeReason majorChange) throws IOException {
        String reason;
        for (Map.Entry<String, IFile> file : baseXsds.entrySet()) {
            // scan the latest version of the class
            IFile curXsd = curXsds.get(file.getKey());
            String changeClass = file.getValue().getName();
            // check whether the xsd have been deleted or changed or added
            if (curXsd == null) {
                reason = "In the package " + pkgName + ", The schema file has been deleted: " + file.getKey() + ".";
                majorChange.update(reason, changeClass, false);
                break;
            } else {
                // check whether it is the same
                //read the current xsd file
                curXsds.remove(file.getKey());
                String curFileContent = readXsdFile(curXsd.open());
                String oldFileContent = readXsdFile(file.getValue().open());
                if (!!!(curFileContent.equals(oldFileContent))) {

                    reason = "In the package " + pkgName + ", The schema file has been updated: " + file.getKey() + ".";
                    majorChange.update(reason, changeClass, false);
                    break;
                }
            }
        }
    }

    /**
     * Check whether the package has gained additional class or xsd files. If yes, log a minor change.
     *
     * @param pkgName
     * @param curClazz
     * @param curXsds
     * @param minorChange
     */
    private void checkAdditionalClassOrXsds(String pkgName, Map<String, IFile> curClazz,
                                            Map<String, IFile> curXsds, VersionChangeReason minorChange) {
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
                    reason = "The package " + pkgName + " has gained at least one class : " + getClassName(changeClass) + ".";
                    minorChange.update(reason, changeClass, false);
                    break;
                }
            }
        }
        if (!!!(minorChange.isChange() || curXsds.isEmpty())) {
            /// a new xsd file was added, it is a minor change
            IFile firstXsd = null;
            Iterator<IFile> xsdIterator = curXsds.values().iterator();
            firstXsd = xsdIterator.next();

            reason = "In the package " + pkgName + ", The schema file(s) are added: " + curXsds.keySet() + ".";
            minorChange.update(reason, firstXsd.getName(), false);
        }
    }

    static boolean isVersionCorrect(VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr) {
        boolean versionCorrect = false;

        Version oldVersion = Version.parseVersion(oldVersionStr);
        Version newVersion = Version.parseVersion(newVersionStr);

        if (status == VERSION_CHANGE_TYPE.MAJOR_CHANGE) {
            if (newVersion.getMajor() > oldVersion.getMajor()) {
                versionCorrect = true;
            }
        } else if (status == VERSION_CHANGE_TYPE.MINOR_CHANGE) {
            if ((newVersion.getMajor() > oldVersion.getMajor()) || (newVersion.getMinor() > oldVersion.getMinor())) {
                versionCorrect = true;
            }
        } else {
            if ((newVersion.getMajor() == oldVersion.getMajor()) && (newVersion.getMinor() == oldVersion.getMinor())) {
                versionCorrect = true;
            }
        }
        return versionCorrect;
    }
    private String getRecommendedVersion( VERSION_CHANGE_TYPE status, String oldVersionStr) {
      Version oldVersion = Version.parseVersion(oldVersionStr);
      Version recommendedNewVersion;
      
      if (status == BundleCompatibility.VERSION_CHANGE_TYPE.MAJOR_CHANGE) {
          recommendedNewVersion = new Version(oldVersion.getMajor() + 1, 0, 0);
      } else if (status == BundleCompatibility.VERSION_CHANGE_TYPE.MINOR_CHANGE) {
          recommendedNewVersion = new Version(oldVersion.getMajor(), oldVersion.getMinor() + 1, 0);
      } else {
          recommendedNewVersion = oldVersion;
      }
      return recommendedNewVersion.toString();
    }

    private String getPkgStatusText(String pkgName, VERSION_CHANGE_TYPE status, String oldVersionStr, String newVersionStr, String reason, String key_class) {

       
        

        if (!isVersionCorrect(status, oldVersionStr, newVersionStr)) {
          return "The package " + pkgName + " has the following changes:" + reason + "\r\nThe package version should be " + getRecommendedVersion(status, oldVersionStr) + ".";
        } else {
          return "";
        }
        
    }

   

    private String getClassName(String fullClassPath) {
        String[] chunks = fullClassPath.split("/");
        String className = chunks[chunks.length - 1];
        className = className.replace(SemanticVersioningUtils.classExt, SemanticVersioningUtils.javaExt);
        return className;
    }

    private String readXsdFile(InputStream is) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException ioe) {
            IOUtils.close(br);
        }
        return sb.toString();
    }

    private SemanticVersioningClassVisitor getVisitor(IFile file, URLClassLoader loader) {
        SemanticVersioningClassVisitor oldcv = new SemanticVersioningClassVisitor(loader);
        try {
            ClassReader cr = new ClassReader(file.open());
            cr.accept(oldcv, 0);
        } catch (IOException ioe) {
            _logger.debug("The file " + file + "cannot be opened.");
        }
        return oldcv;
    }

    enum VERSION_CHANGE_TYPE {
        MAJOR_CHANGE("major"), MINOR_CHANGE("minor"), NO_CHANGE("no");
        private final String text;

        VERSION_CHANGE_TYPE(String text) {
            this.text = text;
        }

        public String text() {
            return this.text;
        }

    }

    private static class PackageContent {
        private final String packageName;
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

        public Map<String, IFile> getClasses() {
            return classes;
        }

        public Map<String, IFile> getXsds() {
            return xsds;
        }

        public String getPackageVersion() {
            return packageVersion;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    private static class VersionChangeReason {
        boolean change = false;
        String reason = null;
        String changeClass = null;
        boolean moreAbstractMethod = false;


        public boolean isMoreAbstractMethod() {
            return moreAbstractMethod;
        }

        public boolean isChange() {
            return change;
        }

        public void setChange(boolean change) {
            this.change = change;
        }

        public String getReason() {
            return reason;
        }

        public String getChangeClass() {
            return changeClass;
        }

        public void update(String reason, String changeClass, boolean moreAbstractMethod) {
            this.change = true;
            this.reason = reason;
            this.changeClass = changeClass;
            this.moreAbstractMethod = moreAbstractMethod;
        }
    }

}

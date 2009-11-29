/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx.framework;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.codec.Package;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.jmx.framework.PackageStateMBean;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * <p>
 * <tt>PackageState</tt> represents implementation of PackageStateMBean.
 * </p>
 * 
 * @see PackageStateMBean
 * 
 * @version $Rev$ $Date$
 */
public class PackageState implements PackageStateMBean {

    /**
     * {@link PackageAdmin} service reference.
     */
    private PackageAdmin packageAdmin;
    private BundleContext context;

    /**
     * Constructs new PackagesState MBean.
     * 
     * @param context bundle context.
     * @param packageAdmin {@link PackageAdmin} service reference.
     */
    public PackageState(BundleContext context, PackageAdmin packageAdmin) {
        this.context = context;
        this.packageAdmin = packageAdmin;
    }

    /**
     * @see org.osgi.jmx.framework.PackageStateMBean#getExportingBundle(java.lang.String, java.lang.String)
     */
    public long getExportingBundle(String packageName, String version) throws IOException {
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(packageName);
        if (exportedPackages != null) {
            Version ver = Version.parseVersion(version);
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (exportedPackage.getVersion().equals(ver)) {
                    return exportedPackage.getExportingBundle().getBundleId();
                }
            }
        }
        return -1;
    }

    /**
     * @see org.osgi.jmx.framework.PackageStateMBean#getImportingBundles(java.lang.String, java.lang.String)
     */
    public long[] getImportingBundles(String packageName, String version) throws IOException {
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(packageName);
        if (exportedPackages != null) {
            Version ver = Version.parseVersion(version);
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (exportedPackage.getVersion().equals(ver)) {
                    Bundle[] bundles = exportedPackage.getImportingBundles();
                    if (bundles != null) {
                        long[] importingBundles = new long[bundles.length];
                        for (int i = 0; i < bundles.length; i++) {
                            importingBundles[i] = bundles[i].getBundleId();
                        }
                        return importingBundles;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.framework.PackageStateMBean#isRemovalPending(java.lang.String, java.lang.String)
     */
    public boolean isRemovalPending(String packageName, String version) throws IOException {
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(packageName);
        if (exportedPackages != null) {
            Version ver = Version.parseVersion(version);
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (exportedPackage.getVersion().equals(ver) && exportedPackage.isRemovalPending()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @see org.osgi.jmx.framework.PackageStateMBean#listPackages()
     */
    public TabularData listPackages() throws IOException {
        Set<Package> packages = new HashSet<Package>();
        for (Bundle bundle : context.getBundles()) {
            ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(bundle);
            for (ExportedPackage exportedPackage : exportedPackages) {
                packages.add(new Package(exportedPackage));
            }

        }
        return Package.tableFrom(packages);
    }

}

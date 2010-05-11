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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.codec.PackageData;
import org.apache.aries.jmx.util.FrameworkUtils;
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
     * @see org.osgi.jmx.framework.PackageStateMBean#getExportingBundles(String, String)
     */
    public long[] getExportingBundles(String packageName, String version) throws IOException {
        if (packageName == null || packageName.length() < 1) {
            throw new IOException("Package name cannot be null or empty");
        }
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(packageName);
        if (exportedPackages != null) {
            Version ver = Version.parseVersion(version);
            List<Bundle> exportingBundles = new ArrayList<Bundle>();
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (exportedPackage.getVersion().equals(ver)) {
                    Bundle bundle  = exportedPackage.getExportingBundle();
                    exportingBundles.add(bundle);
                }
            }
            
            return FrameworkUtils.getBundleIds(exportingBundles);
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.framework.PackageStateMBean#getImportingBundles(String, String, long)
     */
    public long[] getImportingBundles(String packageName, String version, long exportingBundle) throws IOException {
        if (packageName == null || packageName.length() < 1) {
            throw new IOException("Package name cannot be null or empty");
        }
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(packageName);
        if (exportedPackages != null) {
            Version ver = Version.parseVersion(version);
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (exportedPackage.getVersion().equals(ver)
                        && exportedPackage.getExportingBundle().getBundleId() == exportingBundle) {
                    Bundle[] bundles = exportedPackage.getImportingBundles();
                    if (bundles != null) {
                        return FrameworkUtils.getBundleIds(bundles);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.framework.PackageStateMBean#isRemovalPending(String, String, long)
     */
    public boolean isRemovalPending(String packageName, String version, long exportingBundle) throws IOException {
        if (packageName == null || packageName.length() < 1) {
            throw new IOException("Package name cannot be null or empty");
        }
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(packageName);
        if (exportedPackages != null) {
            Version ver = Version.parseVersion(version);
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (exportedPackage.getVersion().equals(ver)
                        && exportedPackage.getExportingBundle().getBundleId() == exportingBundle
                        && exportedPackage.isRemovalPending()) {
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
        Set<PackageData> packages = new HashSet<PackageData>();
        for (Bundle bundle : context.getBundles()) {
            ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(bundle);
            if (exportedPackages != null) {
                for (ExportedPackage exportedPackage : exportedPackages) {
                    packages.add(new PackageData(exportedPackage));
                }
            }

        }
        return PackageData.tableFrom(packages);
    }

}

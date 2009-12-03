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
package org.apache.aries.jmx.util;

import static org.osgi.jmx.framework.BundleStateMBean.ACTIVE;
import static org.osgi.jmx.framework.BundleStateMBean.INSTALLED;
import static org.osgi.jmx.framework.BundleStateMBean.RESOLVED;
import static org.osgi.jmx.framework.BundleStateMBean.STARTING;
import static org.osgi.jmx.framework.BundleStateMBean.STOPPING;
import static org.osgi.jmx.framework.BundleStateMBean.UNINSTALLED;
import static org.osgi.jmx.framework.BundleStateMBean.UNKNOWN;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 * This class contains common utilities related to Framework operations for the MBean implementations
 * 
 * @version $Rev$ $Date$
 */
public class FrameworkUtils {

    private FrameworkUtils() {
        super();
    }

    /**
     * 
     * Returns the Bundle object for a given id 
     * 
     * @param bundleContext
     * @param bundleId
     * @return
     * @throws IllegalArgumentException
     *             if no Bundle is found with matching bundleId
     */
    public static Bundle resolveBundle(BundleContext bundleContext, long bundleId) throws IllegalArgumentException {
        if (bundleContext == null) {
            throw new IllegalArgumentException("Argument bundleContext cannot be null");
        }
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle with id [" + bundleId + "] Not Found");
        }
        return bundle;
    }

    /**
     * Returns an array of bundleIds
     * 
     * @param bundles
     *            array of <code>Bundle</code> objects
     * @return bundleIds in sequence
     */
    public static long[] getBundleIds(Bundle[] bundles) {
        long[] result = (bundles == null) ? new long[0] : new long[bundles.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = bundles[i].getBundleId();
        }
        return result;
    }

    /**
     * 
     * Returns the ServiceReference object with matching service.id
     * 
     * @param bundleContext
     * @param serviceId
     * @return ServiceReference with matching service.id property
     * @throws IllegalArgumentException
     *             if bundleContext is null or no service is found with the given id
     */
    public static ServiceReference resolveService(BundleContext bundleContext, long serviceId) {
        if (bundleContext == null) {
            throw new IllegalArgumentException("Argument bundleContext cannot be null");
        }
        ServiceReference result = null;
        try {
            ServiceReference[] references = bundleContext.getAllServiceReferences(null, "(" + Constants.SERVICE_ID + "=" + serviceId + ")");
            if (references == null || references.length < 1) {
                throw new IllegalArgumentException("Service with id [" + serviceId + "] Not Found");
            } else {
                result = references[0];
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Failure when resolving service ", e);
        } 
        return result;
    }
    
    /**
     * Returns an array of service.id values
     * 
     * @param serviceReferences
     *            array of <code>ServiceReference</code> objects
     * @return service.id values in sequence
     */
    public static long[] getServiceIds(ServiceReference[] serviceReferences) {
        long result[] = (serviceReferences == null) ? new long[0] : new long[serviceReferences.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (Long) serviceReferences[i].getProperty(Constants.SERVICE_ID);
        }
        return result;
    }

    /**
     * Returns the packages exported by the specified bundle
     * 
     * @param bundle
     * @param packageAdmin
     * @return
     * @throws IllegalArgumentException
     *             if bundle or packageAdmin are null
     */
    public static String[] getBundleExportedPackages(Bundle bundle, PackageAdmin packageAdmin)
            throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        String[] exportedPackages;
        ExportedPackage[] exported = packageAdmin.getExportedPackages(bundle);
        if (exported != null) {
            exportedPackages = new String[exported.length];
            for (int i = 0; i < exported.length; i++) {
                exportedPackages[i] = exported[i].getName() + ";" + exported[i].getVersion().toString();
            }
        } else {
            exportedPackages = new String[0];
        }
        return exportedPackages;
    }

    /**
     * Returns the bundle ids of any resolved fragments
     * 
     * @param bundle
     * @param packageAdmin
     * @return
     * @throws IllegalArgumentException
     *             if bundle or packageAdmin are null
     */
    public static long[] getFragmentIds(Bundle bundle, PackageAdmin packageAdmin) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        long[] fragmentIds;
        Bundle[] fragments = packageAdmin.getFragments(bundle);
        if (fragments != null) {
            fragmentIds = getBundleIds(fragments);
        } else {
            fragmentIds = new long[0];
        }
        return fragmentIds;
    }

    /**
     * Returns the bundle ids of any resolved hosts
     * 
     * @param fragment
     * @param packageAdmin
     * @return
     * @throws IllegalArgumentException
     *             if fragment or packageAdmin are null
     */
    public static long[] getHostIds(Bundle fragment, PackageAdmin packageAdmin) throws IllegalArgumentException {
        if (fragment == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        long[] hostIds;
        Bundle[] hosts = packageAdmin.getHosts(fragment);
        if (hosts != null) {
            hostIds = getBundleIds(hosts);
        } else {
            hostIds = new long[0];
        }
        return hostIds;
    }

    /**
     * Returns the resolved package imports for the given bundle
     * 
     * @param bundle
     * @param packageAdmin
     * @return
     * @throws IllegalArgumentException
     *             if fragment or packageAdmin are null
     */
    public static String[] getBundleImportedPackages(Bundle bundle, PackageAdmin packageAdmin)
            throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        List<String> result = new ArrayList<String>();
        // TODO - Is there an easier way to achieve this? Unable to find a direct way through Framework
        // API to find the actual package wiring
        Bundle[] bundles = bundle.getBundleContext().getBundles();
        for (Bundle candidate : bundles) {
            if (candidate.equals(bundle)) {
                continue;
            }
            ExportedPackage[] candidateExports = packageAdmin.getExportedPackages(candidate);
            if (candidateExports != null) {
                for (ExportedPackage exportedPackage : candidateExports) {
                    Bundle[] userBundles = exportedPackage.getImportingBundles();
                    if (userBundles != null && arrayContains(userBundles, bundle)) {
                        result.add(exportedPackage.getName() + ";" + exportedPackage.getVersion().toString());
                    }
                }// end for candidateExports
            }
        }// end for bundles
        return result.toArray(new String[result.size()]);
    }

    /**
     * Returns the service.id values for services registered by the given bundle
     * 
     * @param bundle
     * @return
     * @throws IllegalArgumentException
     *             if bundle is null
     * @throws IlleglStateException
     *             if bundle has been uninstalled
     */
    public static long[] getRegisteredServiceIds(Bundle bundle) throws IllegalArgumentException, IllegalStateException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        long[] serviceIds;
        ServiceReference[] serviceReferences = bundle.getRegisteredServices();
        if (serviceReferences != null) {
            serviceIds = new long[serviceReferences.length];
            for (int i = 0; i < serviceReferences.length; i++) {
                serviceIds[i] = (Long) serviceReferences[i].getProperty(Constants.SERVICE_ID);
            }
        } else {
            serviceIds = new long[0];
        }
        return serviceIds;
    }

    /**
     * Returns the service.id values of services being used by the given bundle
     * 
     * @param bundle
     * @return
     * @throws IllegalArgumentException
     *             if bundle is null
     * @throws IlleglStateException
     *             if bundle has been uninstalled
     */
    public static long[] getServicesInUseByBundle(Bundle bundle) throws IllegalArgumentException, IllegalStateException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        long[] serviceIds;
        ServiceReference[] serviceReferences = bundle.getServicesInUse();
        if (serviceReferences != null) {
            serviceIds = new long[serviceReferences.length];
            for (int i = 0; i < serviceReferences.length; i++) {
                serviceIds[i] = (Long) serviceReferences[i].getProperty(Constants.SERVICE_ID);
            }
        } else {
            serviceIds = new long[0];
        }
        return serviceIds;
    }

    /**
     * Returns the status of pending removal
     * 
     * @param bundle
     * @return true if the bundle is pending removal
     * @throws IllegalArgumentException
     *             if bundle or packageAdmin are null
     */
    public static boolean isBundlePendingRemoval(Bundle bundle, PackageAdmin packageAdmin)
            throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        boolean result = false;
        RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(bundle.getSymbolicName());
        if (requiredBundles != null) {
            for (RequiredBundle requiredBundle : requiredBundles) {
                Bundle required = requiredBundle.getBundle();
                if (required != null && required.equals(bundle)) {
                    result = requiredBundle.isRemovalPending();
                    break;
                }
            }// end for requiredBundles
        }
        return result;
    }

    /**
     * Checks if the given bundle is currently required by other bundles
     * 
     * @param bundle
     * @param packageAdmin
     * @return
     * @throws IllegalArgumentException
     *             if bundle or packageAdmin are null
     */
    public static boolean isBundleRequiredByOthers(Bundle bundle, PackageAdmin packageAdmin)
            throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        boolean result = false;
        RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(bundle.getSymbolicName());
        if (requiredBundles != null) {
            for (RequiredBundle requiredBundle : requiredBundles) {
                Bundle required = requiredBundle.getBundle();
                if (required != null && required.equals(bundle)) {
                    Bundle[] requiring = requiredBundle.getRequiringBundles();
                    if (requiring != null && requiring.length > 0) {
                        result = true;
                        break;
                    }
                }
            }// end for requiredBundles
        }
        return result;
    }

    /**
     * Returns an array of ids of bundles the given bundle depends on
     * 
     * @param bundle
     * @param packageAdmin
     * @return
     * @throws IllegalArgumentException
     *             if bundle or packageAdmin are null
     */
    public static long[] getBundleDependencies(Bundle bundle, PackageAdmin packageAdmin)
            throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        List<Bundle> dependencies = new ArrayList<Bundle>();
        // TODO - Is there an easier way to achieve this? Unable to find a direct way through Framework
        // API to resolve the current dependencies
        for (Bundle candidate : bundle.getBundleContext().getBundles()) {
            if (candidate.equals(bundle)) {
                continue;
            }
            RequiredBundle[] candidateRequiredBundles = packageAdmin.getRequiredBundles(candidate.getSymbolicName());
            if (candidateRequiredBundles == null) {
                continue;
            } else {
                for (RequiredBundle candidateRequiredBundle : candidateRequiredBundles) {
                    Bundle[] bundlesRequiring = candidateRequiredBundle.getRequiringBundles();
                    if (bundlesRequiring != null && arrayContains(bundlesRequiring, bundle)) {
                        dependencies.add(candidateRequiredBundle.getBundle());
                    }
                }
            }
        }
        return getBundleIds(dependencies.toArray(new Bundle[dependencies.size()]));
    }

    /**
     * Returns an array of ids of bundles that depend on the given bundle
     * 
     * @param bundle
     * @param packageAdmin
     * @return
     * @throws IllegalArgumentException
     *             if bundle or packageAdmin are null
     */
    public static long[] getDependentBundles(Bundle bundle, PackageAdmin packageAdmin) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        long[] bundleIds = new long[0];
        RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(bundle.getSymbolicName());
        if (requiredBundles != null) {
            for (RequiredBundle requiredBundle : requiredBundles) {
                Bundle required = requiredBundle.getBundle();
                if (required != null && required.equals(bundle)) {
                    bundleIds = getBundleIds(requiredBundle.getRequiringBundles());
                }
            }
        }
        return bundleIds;
    }

    /**
     * Returns a String representation of the bundles state
     * 
     * @param bundle
     * @return
     */
    public static String getBundleState(Bundle bundle) {
        String state = UNKNOWN;
        switch (bundle.getState()) {
        case Bundle.INSTALLED:
            state = INSTALLED;
            break;
        case Bundle.RESOLVED:
            state = RESOLVED;
            break;
        case Bundle.STARTING:
            state = STARTING;
            break;
        case Bundle.ACTIVE:
            state = ACTIVE;
            break;
        case Bundle.STOPPING:
            state = STOPPING;
            break;
        case Bundle.UNINSTALLED:
            state = UNINSTALLED;
        }
        return state;
    }

    /*
     * Checks if an object exists in the given array (based on object equality)
     */
    public static boolean arrayContains(Object[] array, Object value) {
        boolean result = false;
        if (array != null && value != null) {
            for (Object element : array) {
                if (value.equals(element)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
}

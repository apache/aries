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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.util.ManifestHeaderUtils;
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
    public static Bundle resolveBundle(BundleContext bundleContext, long bundleId) throws IOException {
        if (bundleContext == null) {
            throw new IllegalArgumentException("Argument bundleContext cannot be null");
        }
        Bundle bundle = bundleContext.getBundle(bundleId);
        if (bundle == null) {
            throw new IOException("Bundle with id [" + bundleId + "] not found");
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
        long[] result;
        if (bundles == null) {
            result = new long[0];
        } else {
            result = new long[bundles.length];
            for (int i = 0; i < bundles.length; i++) {
                result[i] = bundles[i].getBundleId();
            }
        }
        return result;
    }
    
    public static long[] getBundleIds(List<Bundle> bundles) { 
        long[] result;
        if (bundles == null) {
            result = new long[0];
        } else {
            result = new long[bundles.size()];
            for (int i = 0; i < bundles.size(); i++) {
                result[i] = bundles.get(i).getBundleId();
            }
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
     * @throws IllegalArgumentException if bundleContext is null
     * @throws IOException if no service is found with the given id
     */
    public static ServiceReference resolveService(BundleContext bundleContext, long serviceId) throws IOException {
        if (bundleContext == null) {
            throw new IllegalArgumentException("Argument bundleContext cannot be null");
        }
        ServiceReference result = null;
        try {
            ServiceReference[] references = bundleContext.getAllServiceReferences(null, "(" + Constants.SERVICE_ID
                    + "=" + serviceId + ")");
            if (references == null || references.length < 1) {
                throw new IOException("Service with id [" + serviceId + "] not found");
            } else {
                result = references[0];
            }
        } catch (InvalidSyntaxException e) {
            IOException ioex = new IOException("Failure when resolving service ");
            ioex.initCause(e);
            throw ioex;
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
     * @param localBundleContext
     *            BundleContext object of this bundle/caller
     * @param bundle
     *            target Bundle object to query imported packages for
     * @param packageAdmin
     * 
     * @return
     * @throws IllegalArgumentException
     *             if fragment or packageAdmin are null
     */
    public static String[] getBundleImportedPackages(BundleContext localBundleContext, Bundle bundle,
            PackageAdmin packageAdmin) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        
        List<String> result = new ArrayList<String>();
        for (ExportedPackage ep : getBundleImportedPackagesRaw(localBundleContext, bundle, packageAdmin)) {
          result.add(ep.getName()+";"+ep.getVersion());
        }
        
        return result.toArray(new String[0]);
    }
    
    @SuppressWarnings("unchecked")
    private static Collection<ExportedPackage> getBundleImportedPackagesRaw(BundleContext localBundleContext, Bundle bundle, PackageAdmin packageAdmin) throws IllegalArgumentException 
    {
      List<ExportedPackage> result = new ArrayList<ExportedPackage>();
      Dictionary<String, String> bundleHeaders = bundle.getHeaders();
      String dynamicImportHeader = bundleHeaders.get(Constants.DYNAMICIMPORT_PACKAGE);
      // if DynamicImport-Package used, then do full iteration
      // else means no dynamic import or has dynamic import but no wildcard "*" in it.
      if (dynamicImportHeader != null && dynamicImportHeader.contains("*")) {
          Bundle[] bundles = localBundleContext.getBundles();
          for (Bundle candidate : bundles) {
              if (candidate.equals(bundle)) {
                  continue;
              }
              ExportedPackage[] candidateExports = packageAdmin.getExportedPackages(candidate);
              if (candidateExports != null) {
                  for (ExportedPackage exportedPackage : candidateExports) {
                      Bundle[] userBundles = exportedPackage.getImportingBundles();
                      if (userBundles != null && arrayContains(userBundles, bundle)) {
                          result.add(exportedPackage);
                      }
                  }// end for candidateExports
              }
          }// end for bundles
      } else { // only query ExportPackage for package names declared as imported
          List<String> importPackages = new ArrayList<String>();
          String importPackageHeader = bundleHeaders.get(Constants.IMPORT_PACKAGE);
          if (importPackageHeader != null && importPackageHeader.length() > 0) {
            importPackages.addAll(extractHeaderDeclaration(importPackageHeader));
          }
          if (dynamicImportHeader != null) {
            importPackages.addAll(extractHeaderDeclaration(dynamicImportHeader));
          }
          for (String packageName : importPackages) {
              ExportedPackage[] candidateExports = packageAdmin.getExportedPackages(packageName);
              if (candidateExports != null) {
                  for (ExportedPackage exportedPackage : candidateExports) {
                      Bundle[] userBundles = exportedPackage.getImportingBundles();
                      if (userBundles != null && arrayContains(userBundles, bundle)) {
                          result.add(exportedPackage);
                      }
                  }// end for candidateExports
              }
          }
      }
      return result;
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
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(bundle);
        if (exportedPackages != null) {
            for (ExportedPackage exportedPackage : exportedPackages) {
                if (exportedPackage.isRemovalPending()) {
                    result = true;
                    break;
                }
            }
        }
        if (!result) {
            RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(bundle.getSymbolicName());
            if (requiredBundles != null) {
                for (RequiredBundle requiredBundle : requiredBundles) {
                    Bundle required = requiredBundle.getBundle();
                    if (required == bundle) {
                        result = requiredBundle.isRemovalPending();
                        break;
                    }
                }
            }
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
        // Check imported packages (statically or dynamically)
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(bundle);
        if (exportedPackages != null) {
            for (ExportedPackage exportedPackage : exportedPackages) {
                Bundle[] importingBundles = exportedPackage.getImportingBundles();
                if (importingBundles != null && importingBundles.length > 0) {
                    result = true;
                    break;
                }
            }
        }
        if (!result) {
            // Check required bundles
            RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(bundle.getSymbolicName());
            if (requiredBundles != null) {
                for (RequiredBundle requiredBundle : requiredBundles) {
                    Bundle required = requiredBundle.getBundle();
                    if (required == bundle) {
                        Bundle[] requiring = requiredBundle.getRequiringBundles();
                        if (requiring != null && requiring.length > 0) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }
        if (!result) {
            // Check fragment bundles
            Bundle[] fragments = packageAdmin.getFragments(bundle);
            if (fragments != null && fragments.length > 0) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Returns an array of ids of bundles the given bundle depends on
     * 
     * @param localBundleContext
     *            BundleContext object of this bundle/caller
     * @param bundle
     *            target Bundle object to query dependencies for
     * @param packageAdmin
     * 
     * @return
     * @throws IllegalArgumentException
     *             if bundle or packageAdmin are null
     */
    @SuppressWarnings("unchecked")
    public static long[] getBundleDependencies(BundleContext localBundleContext, 
                                               Bundle bundle,
                                               PackageAdmin packageAdmin) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null) {
            throw new IllegalArgumentException("Argument packageAdmin cannot be null");
        }
        Set<Bundle> dependencies = new HashSet<Bundle>();
        
        for (ExportedPackage ep : getBundleImportedPackagesRaw(localBundleContext, bundle, packageAdmin)) {
          dependencies.add(ep.getExportingBundle());
        }
        
        // Handle required bundles
        Dictionary<String, String> bundleHeaders = bundle.getHeaders();
        String requireBundleHeader = bundleHeaders.get(Constants.REQUIRE_BUNDLE);
        if (requireBundleHeader != null) { // only check if Require-Bundle is used
        	List<String> bundleSymbolicNames = extractHeaderDeclaration(requireBundleHeader);
            for (String bundleSymbolicName: bundleSymbolicNames) {
                RequiredBundle[] candidateRequiredBundles = packageAdmin.getRequiredBundles(bundleSymbolicName);
                if (candidateRequiredBundles != null) {
                    for (RequiredBundle candidateRequiredBundle : candidateRequiredBundles) {
                        Bundle[] bundlesRequiring = candidateRequiredBundle.getRequiringBundles();
                        if (bundlesRequiring != null && arrayContains(bundlesRequiring, bundle)) {
                            dependencies.add(candidateRequiredBundle.getBundle());
                        }
                    }
                }
            }
        }
        // Handle fragment bundles
        Bundle[] hosts = packageAdmin.getHosts(bundle);
        if (hosts != null) {
            for (Bundle host : hosts) {
                dependencies.add(host);
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
        Set<Bundle> dependencies = new HashSet<Bundle>();
        // Handle imported packages (statically or dynamically)
        ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(bundle);
        if (exportedPackages != null) {
            for (ExportedPackage exportedPackage : exportedPackages) {
                Bundle[] importingBundles = exportedPackage.getImportingBundles();
                if (importingBundles != null) {
                    for (Bundle importingBundle : importingBundles) {
                        dependencies.add(importingBundle);
                    }
                }
            }
        }
        // Handle required bundles
        RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(bundle.getSymbolicName());
        if (requiredBundles != null) {
            for (RequiredBundle requiredBundle : requiredBundles) {
                Bundle required = requiredBundle.getBundle();
                if (required == bundle) {
                    Bundle[] requiringBundles = requiredBundle.getRequiringBundles();
                    if (requiringBundles != null) {
                        for (Bundle requiringBundle : requiringBundles) {
                            dependencies.add(requiringBundle);
                        }
                    }
                }
            }
        }
        // Handle fragment bundles
        Bundle[] fragments = packageAdmin.getFragments(bundle);
        if (fragments != null) {
            for (Bundle fragment : fragments) {
                dependencies.add(fragment);
            }
        }
        return getBundleIds(dependencies.toArray(new Bundle[dependencies.size()]));
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

    /*
     * Will parse a header value, strip out trailing attributes and return a list of declarations
     */
    public static List<String> extractHeaderDeclaration(String headerStatement) {
        List<String> result = new ArrayList<String>();
        
        for (String headerDeclaration : ManifestHeaderUtils.split(headerStatement, ",")) {
            String name = headerDeclaration.contains(";") ? headerDeclaration.substring(0, headerDeclaration
                    .indexOf(";")) : headerDeclaration;
            result.add(name);
        }
        
        return result;
    }
    
    
}

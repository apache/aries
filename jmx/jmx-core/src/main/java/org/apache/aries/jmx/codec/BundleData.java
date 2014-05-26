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
package org.apache.aries.jmx.codec;

import static org.apache.aries.jmx.util.FrameworkUtils.getBundleDependencies;
import static org.apache.aries.jmx.util.FrameworkUtils.getBundleExportedPackages;
import static org.apache.aries.jmx.util.FrameworkUtils.getBundleImportedPackages;
import static org.apache.aries.jmx.util.FrameworkUtils.getBundleState;
import static org.apache.aries.jmx.util.FrameworkUtils.getDependentBundles;
import static org.apache.aries.jmx.util.FrameworkUtils.getFragmentIds;
import static org.apache.aries.jmx.util.FrameworkUtils.getHostIds;
import static org.apache.aries.jmx.util.FrameworkUtils.getRegisteredServiceIds;
import static org.apache.aries.jmx.util.FrameworkUtils.getServicesInUseByBundle;
import static org.apache.aries.jmx.util.FrameworkUtils.isBundlePendingRemoval;
import static org.apache.aries.jmx.util.FrameworkUtils.isBundleRequiredByOthers;
import static org.apache.aries.jmx.util.TypeUtils.toLong;
import static org.apache.aries.jmx.util.TypeUtils.toPrimitive;
import static org.osgi.jmx.framework.BundleStateMBean.BUNDLE_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.EXPORTED_PACKAGES;
import static org.osgi.jmx.framework.BundleStateMBean.FRAGMENT;
import static org.osgi.jmx.framework.BundleStateMBean.FRAGMENTS;
import static org.osgi.jmx.framework.BundleStateMBean.HEADERS;
import static org.osgi.jmx.framework.BundleStateMBean.HEADERS_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.HEADER_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.HOSTS;
import static org.osgi.jmx.framework.BundleStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.BundleStateMBean.IMPORTED_PACKAGES;
import static org.osgi.jmx.framework.BundleStateMBean.KEY;
import static org.osgi.jmx.framework.BundleStateMBean.LAST_MODIFIED;
import static org.osgi.jmx.framework.BundleStateMBean.LOCATION;
import static org.osgi.jmx.framework.BundleStateMBean.PERSISTENTLY_STARTED;
import static org.osgi.jmx.framework.BundleStateMBean.REGISTERED_SERVICES;
import static org.osgi.jmx.framework.BundleStateMBean.REMOVAL_PENDING;
import static org.osgi.jmx.framework.BundleStateMBean.REQUIRED;
import static org.osgi.jmx.framework.BundleStateMBean.REQUIRED_BUNDLES;
import static org.osgi.jmx.framework.BundleStateMBean.REQUIRING_BUNDLES;
import static org.osgi.jmx.framework.BundleStateMBean.SERVICES_IN_USE;
import static org.osgi.jmx.framework.BundleStateMBean.START_LEVEL;
import static org.osgi.jmx.framework.BundleStateMBean.STATE;
import static org.osgi.jmx.framework.BundleStateMBean.SYMBOLIC_NAME;
import static org.osgi.jmx.framework.BundleStateMBean.VALUE;
import static org.osgi.jmx.framework.BundleStateMBean.VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMRuntimeException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * <p>
 * <tt>BundleData</tt> represents BundleData Type @see {@link BundleStateMBean#BUNDLE_TYPE}. It is a codec for the
 * <code>CompositeData</code> representing an OSGi BundleData.
 * </p>
 *
 * @version $Rev$ $Date$
 */
@SuppressWarnings("deprecation")
public class BundleData {

    /**
     * @see BundleStateMBean#EXPORTED_PACKAGES_ITEM
     */
    private String[] exportedPackages;

    /**
     * @see BundleStateMBean#FRAGMENT_ITEM
     */
    private boolean fragment;

    /**
     * @see BundleStateMBean#FRAGMENTS_ITEM
     */
    private long[] fragments;

    /**
     * @see BundleStateMBean#HEADER_TYPE
     */
    private List<Header> headers = new ArrayList<Header>();

    /**
     * @see BundleStateMBean#HOSTS_ITEM
     */
    private long[] hosts;

    /**
     * @see BundleStateMBean#IDENTIFIER_ITEM
     */
    private long identifier;

    /**
     * @see BundleStateMBean#IMPORTED_PACKAGES_ITEM
     */
    private String[] importedPackages;

    /**
     * @see BundleStateMBean#LAST_MODIFIED_ITEM
     */
    private long lastModified;

    /**
     * @see BundleStateMBean#LOCATION_ITEM
     */
    private String location;

    /**
     * @see BundleStateMBean#PERSISTENTLY_STARTED_ITEM
     */
    private boolean persistentlyStarted;

    /**
     * @see BundleStateMBean#REGISTERED_SERVICES_ITEM
     */
    private long[] registeredServices;

    /**
     * @see BundleStateMBean#REMOVAL_PENDING_ITEM
     */
    private boolean removalPending;

    /**
     * @see BundleStateMBean#REQUIRED_ITEM
     */
    private boolean required;

    /**
     * @see BundleStateMBean#REQUIRED_BUNDLES_ITEM
     */
    private long[] requiredBundles;

    /**
     * @see BundleStateMBean#REQUIRING_BUNDLES_ITEM
     */
    private long[] requiringBundles;

    /**
     * @see BundleStateMBean#SERVICES_IN_USE_ITEM
     */
    private long[] servicesInUse;

    /**
     * @see BundleStateMBean#START_LEVEL_ITEM
     */
    private int bundleStartLevel;

    /**
     * @see BundleStateMBean#STATE_ITEM
     */
    private String state;

    /**
     * @see BundleStateMBean#SYMBOLIC_NAME_ITEM
     */
    private String symbolicName;

    /**
     * @see BundleStateMBean#VERSION_ITEM
     */
    private String version;

    private BundleData() {
        super();
    }

    public BundleData(BundleContext localBundleContext, Bundle bundle, PackageAdmin packageAdmin, StartLevel startLevel) {
        if (bundle == null) {
            throw new IllegalArgumentException("Argument bundle cannot be null");
        }
        if (packageAdmin == null || startLevel == null) {
            throw new IllegalArgumentException("Arguments PackageAdmin / startLevel cannot be null");
        }
        this.exportedPackages = getBundleExportedPackages(bundle, packageAdmin);
        this.fragment = (PackageAdmin.BUNDLE_TYPE_FRAGMENT == packageAdmin.getBundleType(bundle));
        this.fragments = getFragmentIds(bundle, packageAdmin);
        Dictionary<String, String> bundleHeaders = bundle.getHeaders();
        Enumeration<String> keys = bundleHeaders.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            headers.add(new Header(key, bundleHeaders.get(key)));
        }
        this.hosts = getHostIds(bundle, packageAdmin);
        this.identifier = bundle.getBundleId();
        this.importedPackages = getBundleImportedPackages(localBundleContext, bundle, packageAdmin);
        this.lastModified = bundle.getLastModified();
        this.location = bundle.getLocation();
        this.persistentlyStarted = startLevel.isBundlePersistentlyStarted(bundle);
        this.registeredServices = getRegisteredServiceIds(bundle);
        this.removalPending = isBundlePendingRemoval(bundle, packageAdmin);
        this.required = isBundleRequiredByOthers(bundle, packageAdmin);
        this.requiredBundles = getBundleDependencies(localBundleContext, bundle, packageAdmin);
        this.requiringBundles = getDependentBundles(bundle, packageAdmin);
        this.servicesInUse = getServicesInUseByBundle(bundle);
        this.bundleStartLevel = startLevel.getBundleStartLevel(bundle);
        this.state = getBundleState(bundle);
        this.symbolicName = bundle.getSymbolicName();
        this.version = bundle.getVersion().toString();
    }

    /**
     * Returns CompositeData representing a BundleData complete state typed by {@link BundleStateMBean#BUNDLE_TYPE}
     *
     * @return
     */
    public CompositeData toCompositeData() {
        return toCompositeData(BundleStateMBean.BUNDLE_TYPE.keySet());
    }

    public CompositeData toCompositeData(Collection<String> itemNames) {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put(IDENTIFIER, this.identifier);

        if (itemNames.contains(EXPORTED_PACKAGES))
            items.put(EXPORTED_PACKAGES, this.exportedPackages);

        if (itemNames.contains(FRAGMENT))
            items.put(FRAGMENT, this.fragment);

        if (itemNames.contains(FRAGMENTS))
            items.put(FRAGMENTS, toLong(this.fragments));

        if (itemNames.contains(HOSTS))
            items.put(HOSTS, toLong(this.hosts));

        if (itemNames.contains(IMPORTED_PACKAGES))
            items.put(IMPORTED_PACKAGES, this.importedPackages);

        if (itemNames.contains(LAST_MODIFIED))
            items.put(LAST_MODIFIED, this.lastModified);

        if (itemNames.contains(LOCATION))
            items.put(LOCATION, this.location);

        if (itemNames.contains(PERSISTENTLY_STARTED))
            items.put(PERSISTENTLY_STARTED, this.persistentlyStarted);

        if (itemNames.contains(REGISTERED_SERVICES))
            items.put(REGISTERED_SERVICES, toLong(this.registeredServices));

        if (itemNames.contains(REMOVAL_PENDING))
            items.put(REMOVAL_PENDING, this.removalPending);

        if (itemNames.contains(REQUIRED))
            items.put(REQUIRED, this.required);

        if (itemNames.contains(REQUIRED_BUNDLES))
            items.put(REQUIRED_BUNDLES, toLong(this.requiredBundles));

        if (itemNames.contains(REQUIRING_BUNDLES))
            items.put(REQUIRING_BUNDLES, toLong(this.requiringBundles));

        if (itemNames.contains(SERVICES_IN_USE))
            items.put(SERVICES_IN_USE, toLong(this.servicesInUse));

        if (itemNames.contains(START_LEVEL))
            items.put(START_LEVEL, this.bundleStartLevel);

        if (itemNames.contains(STATE))
            items.put(STATE, this.state);

        if (itemNames.contains(SYMBOLIC_NAME))
            items.put(SYMBOLIC_NAME, this.symbolicName);

        if (itemNames.contains(VERSION))
            items.put(VERSION, this.version);

        if (itemNames.contains(HEADERS)) {
            TabularData headerTable = new TabularDataSupport(HEADERS_TYPE);
            for (Header header : this.headers) {
                headerTable.put(header.toCompositeData());
            }
            items.put(HEADERS, headerTable);
        }

        String[] allItemNames = BUNDLE_TYPE.keySet().toArray(new String [] {});
        Object[] itemValues = new Object[allItemNames.length];
        for (int i=0; i < allItemNames.length; i++) {
            itemValues[i] = items.get(allItemNames[i]);
        }

        try {
            return new CompositeDataSupport(BUNDLE_TYPE, allItemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Failed to create CompositeData for BundleData [" + this.identifier
                    + "]", e);
        }
    }

    /**
     * Constructs a <code>BundleData</code> object from the given <code>CompositeData</code>
     *
     * @param compositeData
     * @return
     * @throws IlleglArgumentException
     *             if compositeData is null or not of type {@link BundleStateMBean#BUNDLE_TYPE}
     */
    @SuppressWarnings("unchecked")
    public static BundleData from(CompositeData compositeData) throws IllegalArgumentException {
        if (compositeData == null) {
            throw new IllegalArgumentException("Argument compositeData cannot be null");
        }
        if (!compositeData.getCompositeType().equals(BUNDLE_TYPE)) {
            throw new IllegalArgumentException("Invalid CompositeType [" + compositeData.getCompositeType() + "]");
        }
        BundleData bundleData = new BundleData();
        bundleData.exportedPackages = (String[]) compositeData.get(EXPORTED_PACKAGES);
        bundleData.fragment = (Boolean) compositeData.get(FRAGMENT);
        bundleData.fragments = toPrimitive((Long[]) compositeData.get(FRAGMENTS));
        bundleData.hosts = toPrimitive((Long[]) compositeData.get(HOSTS));
        bundleData.identifier = (Long) compositeData.get(IDENTIFIER);
        bundleData.importedPackages = (String[]) compositeData.get(IMPORTED_PACKAGES);
        bundleData.lastModified = (Long) compositeData.get(LAST_MODIFIED);
        bundleData.location = (String) compositeData.get(LOCATION);
        bundleData.persistentlyStarted = (Boolean) compositeData.get(PERSISTENTLY_STARTED);
        bundleData.registeredServices = toPrimitive((Long[]) compositeData.get(REGISTERED_SERVICES));
        bundleData.removalPending = (Boolean) compositeData.get(REMOVAL_PENDING);
        bundleData.required = (Boolean) compositeData.get(REQUIRED);
        bundleData.requiredBundles = toPrimitive((Long[]) compositeData.get(REQUIRED_BUNDLES));
        bundleData.requiringBundles = toPrimitive((Long[]) compositeData.get(REQUIRING_BUNDLES));
        bundleData.servicesInUse = toPrimitive((Long[]) compositeData.get(SERVICES_IN_USE));
        bundleData.bundleStartLevel = (Integer) compositeData.get(START_LEVEL);
        bundleData.state = (String) compositeData.get(STATE);
        bundleData.symbolicName = (String) compositeData.get(SYMBOLIC_NAME);
        bundleData.version = (String) compositeData.get(VERSION);
        TabularData headerTable = (TabularData) compositeData.get(HEADERS);
        Collection<CompositeData> headerData = (Collection<CompositeData>) headerTable.values();
        for (CompositeData headerRow : headerData) {
            bundleData.headers.add(Header.from(headerRow));
        }
        return bundleData;
    }

    public String[] getExportedPackages() {
        return exportedPackages;
    }

    public boolean isFragment() {
        return fragment;
    }

    public long[] getFragments() {
        return fragments;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public long[] getHosts() {
        return hosts;
    }

    public long getIdentifier() {
        return identifier;
    }

    public String[] getImportedPackages() {
        return importedPackages;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getLocation() {
        return location;
    }

    public boolean isPersistentlyStarted() {
        return persistentlyStarted;
    }

    public long[] getRegisteredServices() {
        return registeredServices;
    }

    public boolean isRemovalPending() {
        return removalPending;
    }

    public boolean isRequired() {
        return required;
    }

    public long[] getRequiredBundles() {
        return requiredBundles;
    }

    public long[] getRequiringBundles() {
        return requiringBundles;
    }

    public long[] getServicesInUse() {
        return servicesInUse;
    }

    public int getBundleStartLevel() {
        return bundleStartLevel;
    }

    public String getState() {
        return state;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getVersion() {
        return version;
    }

    /*
     * Represents key/value pair in BundleData headers
     */
    public static class Header {

        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        private Header() {
            super();
        }

        public Header(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public CompositeData toCompositeData() throws JMRuntimeException {
            CompositeData result = null;
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(KEY, key);
            items.put(VALUE, value);
            try {
                result = new CompositeDataSupport(HEADER_TYPE, items);
            } catch (OpenDataException e) {
                throw new JMRuntimeException("Failed to create CompositeData for header [" + key + ":" + value + "] - "
                        + e.getMessage());
            }
            return result;
        }

        public static Header from(CompositeData compositeData) {
            if (compositeData == null) {
                throw new IllegalArgumentException("Argument compositeData cannot be null");
            }
            if (!compositeData.getCompositeType().equals(HEADER_TYPE)) {
                throw new IllegalArgumentException("Invalid CompositeType [" + compositeData.getCompositeType() + "]");
            }
            Header header = new Header();
            header.key = (String) compositeData.get(KEY);
            header.value = (String) compositeData.get(VALUE);
            return header;
        }
    }
}

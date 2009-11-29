package org.apache.aries.jmx.codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.osgi.framework.Bundle;
import org.osgi.jmx.framework.PackageStateMBean;
import org.osgi.service.packageadmin.ExportedPackage;

/**
 * <p>
 * <tt>Package</tt>represents PackageType @see {@link PackageStateMBean#PACKAGE_TYPE}.
 * It is a codec for the composite data representing an OSGi ExportedPackage.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public class Package {

    /**
     * {@link PackageStateMBean#EXPORTING_BUNDLE}
     */
    long exportingBundle;

    /**
     * {@link PackageStateMBean#IMPORTING_BUNDLES}
     */
    long[] importingBundles;

    /**
     * {@link PackageStateMBean#NAME}
     */
    String name;

    /**
     * {@link PackageStateMBean#REMOVAL_PENDING}
     */
    boolean removalPending;

    /**
     * {@link PackageStateMBean#VERSION}
     */
    String version;

    /**
     * Constructs new Package with provided ExportedPackage.
     * @param exportedPackage @see {@link ExportedPackage}.
     */
    public Package(ExportedPackage exportedPackage) {
        this(exportedPackage.getExportingBundle().getBundleId(), toBundleIds(exportedPackage.getImportingBundles()),
                exportedPackage.getName(), exportedPackage.isRemovalPending(), exportedPackage.getVersion().toString());

    }

    /**
     * Constructs new Package.
     * 
     * @param exportingBundle the bundle the package belongs to.
     * @param importingBundles the importing bundles of the package.
     * @param name the package name.
     * @param removalPending whether the package is pending removal.
     * @param version package version.
     */
    public Package(long exportingBundle, long[] importingBundles, String name, boolean removalPending, String version) {
        this.exportingBundle = exportingBundle;
        this.importingBundles = importingBundles;
        this.name = name;
        this.removalPending = removalPending;
        this.version = version;
    }
    
    /**
     * Translates Package to CompositeData represented by
     * compositeType {@link PackageStateMBean#PACKAGE_TYPE}.
     * 
     * @return translated Package to compositeData.
     */
    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(PackageStateMBean.EXPORTING_BUNDLE, exportingBundle);
            items.put(PackageStateMBean.IMPORTING_BUNDLES, toLongArray(importingBundles));
            items.put(PackageStateMBean.NAME, name);
            items.put(PackageStateMBean.REMOVAL_PENDING, removalPending);
            items.put(PackageStateMBean.VERSION, version);
            return new CompositeDataSupport(PackageStateMBean.PACKAGE_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData" + e);
        }
    }

    /**
     * Static factory method to create Package from CompositeData object.
     * 
     * @param data {@link CompositeData} instance.
     * @return Package instance.
     */
    public static Package from(CompositeData data) {
        if(data == null){
            return null;
        }
        long exportingBundle = (Long) data.get(PackageStateMBean.EXPORTING_BUNDLE);
        long[] importingBundles = toLongPrimitiveArray((Long[]) data.get(PackageStateMBean.IMPORTING_BUNDLES));
        String name = (String) data.get(PackageStateMBean.NAME);
        boolean removalPending = (Boolean) data.get(PackageStateMBean.REMOVAL_PENDING);
        String version = (String) data.get(PackageStateMBean.VERSION);
        return new Package(exportingBundle,importingBundles,name, removalPending,version);
    }

    /**
     * Creates {@link TabularData} for set of Package's.
     * 
     * @param packages set of Package's
     * @return {@link TabularData} instance.
     */
    public static TabularData tableFrom(Set<Package> packages){
        TabularData table = new TabularDataSupport(PackageStateMBean.PACKAGES_TYPE);
        for(Package pkg : packages){
            table.put(pkg.toCompositeData());
        }
        return table;
    }

    /**
     * Converts array of bundles to array of bundle id's.
     * 
     * @param bundles array of Bundle's.
     * @return array of bundle id's.
     */
    public static long[] toBundleIds(Bundle[] bundles) {
        if (bundles != null) {
            long[] importingBundles = new long[bundles.length];
            for (int i = 0; i < bundles.length; i++) {
                importingBundles[i] = bundles[i].getBundleId();
            }
            return importingBundles;
        }
        return null;
    }
    
    /**
     * Converts primitive array of strings to Long array.
     * 
     * @param primitiveArray primitive long array.
     * @return Long array.
     */
    protected Long[] toLongArray(long[] primitiveArray) {
        if (primitiveArray == null) {
            return null;
        }
        Long[] converted = new Long[primitiveArray.length];
        for (int i = 0; i < primitiveArray.length; i++) {
            converted[i] = primitiveArray[i];
        }

        return converted;
    }

    /**
     * Converts Long array to primitive array of long.
     * 
     * @param wrapperArray Long array.
     * @return primitive long array.
     */
    protected static long[] toLongPrimitiveArray(Long[] wrapperArray) {
        if (wrapperArray == null) {
            return null;
        }
        long[] converted = new long[wrapperArray.length];
        for (int i = 0; i < wrapperArray.length; i++) {
            converted[i] = wrapperArray[i];
        }

        return converted;
    }

    /**
     * @return the exportingBundle
     */
    public long getExportingBundle() {
        return exportingBundle;
    }

    /**
     * @return the importingBundles
     */
    public long[] getImportingBundles() {
        return importingBundles;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the removalPending
     */
    public boolean isRemovalPending() {
        return removalPending;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

}

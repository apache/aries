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

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.osgi.jmx.framework.FrameworkMBean;

/**
 * <p>
 * <tt>BatchInstallResult</tt> represents codec for resulting CompositeData of
 * FrameworkMBean installBundles methods.
 * It converting batch install results to CompositeData {@link #toCompositeData()}
 * and from CompositeData to this BatchInstallResult {@link #from(CompositeData)}.
 * It provides also constructors to build BatchInstallResult.  
 * Structure of compositeData as defined in compositeType @see {@link FrameworkMBean#BATCH_INSTALL_RESULT_TYPE}.
 * </p>
 * @see BatchResult
 *
 * @version $Rev$ $Date$
 */
public class BatchInstallResult extends BatchResult {

    /**
     * @see FrameworkMBean#REMAINING_LOCATION_ITEM
     * @see FrameworkMBean#REMAINING
     */
    private String[] remainingLocationItems;
    
    /**
     * @see FrameworkMBean#BUNDLE_IN_ERROR_LOCATION_ITEM
     * @see FrameworkMBean#BUNDLE_IN_ERROR
     */
    private String bundleInError;

    /**
     * Constructs new BatchInstallResult with completedItems array.
     * Newly created object represents successful batch result.
     * @param completedItems containing the list of bundles completing the batch operation.
     */
    public BatchInstallResult(long[] completedItems) {
        this.completed = completedItems;
        success = true;
    }

    /**
     * Constructs new BatchInstallResult with error message.
     * Newly created object represents failed batch result.
     * @param error containing the error message of the batch operation.
     */
    public BatchInstallResult(String error){
        this.error = error;
        success = false;
    }
    /**
     * Constructs new BatchInstallResult.
     * Newly created object represents failed batch result.
     * 
     * @param completedItems containing the list of bundles completing the batch operation.
     * @param error containing the error message of the batch operation.
     * @param remainingLocationItems remaining bundles unprocessed by the
     * failing batch operation.
     * @param bundleInError containing the bundle which caused the error during the batch
     * operation.
     */
    public BatchInstallResult(long[] completedItems, String error, String[] remainingLocationItems, String bundleInError) {
        this(completedItems, error, remainingLocationItems, false, bundleInError);
    }

    /**
     * Constructs new BatchInstallResult.
     * 
     * @param completedItems containing the list of bundles completing the batch operation.
     * @param error containing the error message of the batch operation.
     * @param remainingLocationItems remaining bundles unprocessed by the
     * failing batch operation.
     * @param success indicates if this operation was successful.
     * @param bundleInError containing the bundle which caused the error during the batch
     * operation.
     */
    public BatchInstallResult(long[] completedItems, String error, String[] remainingLocationItems, boolean success,
            String bundleInError) {
        this.bundleInError = bundleInError;
        this.completed = completedItems;
        this.error = error;
        this.remainingLocationItems = remainingLocationItems;
        this.success = success;
    }

    /**
     * Translates BatchInstallResult to CompositeData represented by
     * compositeType {@link FrameworkMBean#BATCH_INSTALL_RESULT_TYPE}.
     * 
     * @return translated BatchInstallResult to compositeData.
     */
    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(FrameworkMBean.BUNDLE_IN_ERROR, bundleInError);
            items.put(FrameworkMBean.COMPLETED, toLongArray(completed));
            items.put(FrameworkMBean.ERROR, error);
            items.put(FrameworkMBean.REMAINING, remainingLocationItems);
            items.put(FrameworkMBean.SUCCESS, success);
            return new CompositeDataSupport(FrameworkMBean.BATCH_INSTALL_RESULT_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData" + e);
        }
    }

    /**
     * Static factory method to create BatchInstallResult from CompositeData object.
     * 
     * @param data {@link CompositeData} instance.
     * @return BatchInstallResult instance.
     */
    public static BatchInstallResult from(CompositeData data) {
        if(data == null){
            return null;
        }
        String bundleInError = (String) data.get(FrameworkMBean.BUNDLE_IN_ERROR);
        long[] completedItems = toLongPrimitiveArray((Long[]) data.get(FrameworkMBean.COMPLETED));
        String[] remainingLocationItems = (String[]) data.get(FrameworkMBean.REMAINING);
        String error = (String) data.get(FrameworkMBean.ERROR);
        boolean success = (Boolean) data.get(FrameworkMBean.SUCCESS);
        return new BatchInstallResult(completedItems, error, remainingLocationItems, success, bundleInError);
    }

    /**
     * Gets remaining location items.
     * @return array of String with locations.
     */
    public String[] getRemainingLocationItems() {
        return remainingLocationItems;
    }

    /**
     * Gets bundle in error location.
     * @return the bundleInError.
     */
    public String getBundleInError() {
        return bundleInError;
    }

}

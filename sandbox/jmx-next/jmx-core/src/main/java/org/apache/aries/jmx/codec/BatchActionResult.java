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
 * <tt>BatchInstallResult</tt> represents codec for resulting CompositeData of batch operations
 * made on bundle via FrameworkMBean.
 * It's converting batch install results to CompositeData {@link #toCompositeData()}
 * and from CompositeData to this BatchActionResult {@link #from(CompositeData)}.
 * It provides also constructors to build BatchActionResult.  
 * Structure of compositeData is as defined in compositeType @see {@link FrameworkMBean#BATCH_ACTION_RESULT_TYPE}.
 * </p>
 * @see BatchResult
 *
 * @version $Rev$ $Date$
 */
public class BatchActionResult extends BatchResult{

    /**
     * @see FrameworkMBean#REMAINING_ID_ITEM
     * @see FrameworkMBean#REMAINING_LOCATION_ITEM
     */
	private long[] remainingItems;
	/**
	 * @see FrameworkMBean#BUNDLE_IN_ERROR_ID_ITEM
	 * @see FrameworkMBean#BUNDLE_IN_ERROR
	 */
	private long bundleInError;

	/**
	 * Constructs new BatchActionResult with completedItems array.
     * Newly created object represents successful batch result.
     * @param completedItems containing the list of bundles completing the batch operation.
	 */
	public BatchActionResult(long[] completedItems){
		this.completed = completedItems;
		success = true;
	}
	
	 /**
     * Constructs new BatchActionResult with error message.
     * Newly created object represents failed batch result.
     * @param error containing the error message of the batch operation.
     */
    public BatchActionResult(String error){
        this.error = error;
        success = false;
    }
	
	/**
	 * Constructs new BatchActionResult.
     * Newly created object represents failed batch result.
     * 
     * @param completedItems containing the list of bundles completing the batch operation.
     * @param error containing the error message of the batch operation.
     * @param remainingItems remaining bundles unprocessed by the
     * failing batch operation.
     * @param bundleInError containing the bundle which caused the error during the batch
     * operation.
	 */
	public BatchActionResult(long[] completedItems, String error, long[] remainingItems, long bundleInError){
		this(completedItems,error,remainingItems,false,bundleInError);
	}
	
	/**
	 * Constructs new BatchActionResult.
     * 
     * @param completedItems containing the list of bundles completing the batch operation.
     * @param error containing the error message of the batch operation.
     * @param remainingItems remaining bundles unprocessed by the
     * failing batch operation.
     * @param success indicates if this operation was successful.
     * @param bundleInError containing the bundle which caused the error during the batch
     * operation.
	 */
	public BatchActionResult(long[] completedItems, String error, long[] remainingItems, boolean success, long bundleInError){
		this.bundleInError = bundleInError;
		this.completed = completedItems;
		this.error = error;
		this.remainingItems = remainingItems;
		this.success = success;
	}
	
	/**
	 * Translates BatchActionResult to CompositeData represented by
     * compositeType {@link FrameworkMBean#BATCH_ACTION_RESULT_TYPE}.
     * 
	 * @return translated BatchActionResult  to compositeData.
	 */
	public CompositeData toCompositeData(){
		try {
			Map<String, Object> items = new HashMap<String, Object>();
			items.put(FrameworkMBean.BUNDLE_IN_ERROR, bundleInError);
			items.put(FrameworkMBean.COMPLETED, toLongArray(completed));
			items.put(FrameworkMBean.ERROR, error);
			items.put(FrameworkMBean.REMAINING, toLongArray(remainingItems));
			items.put(FrameworkMBean.SUCCESS, success);
			return new CompositeDataSupport(FrameworkMBean.BATCH_ACTION_RESULT_TYPE, items);
		} catch (OpenDataException e) {
			throw new IllegalStateException("Can't create CompositeData" + e);
		}
	}
	
	/**
	 * Static factory method to create BatchActionResult from CompositeData object.
	 * 
	 * @param data {@link CompositeData} instance.
	 * @return BatchActionResult instance.
	 */
	public static BatchActionResult from(CompositeData data){
	    if(data == null){
	        return null;
	    }
		long bundleInError = (Long) data.get(FrameworkMBean.BUNDLE_IN_ERROR);
		// need to convert primitive array to wrapper type array
		// compositeData accept only wrapper type array
		long[] completedItems = toLongPrimitiveArray((Long[])data.get(FrameworkMBean.COMPLETED));
		long[] remainingItems = toLongPrimitiveArray((Long[]) data.get(FrameworkMBean.REMAINING));
		String error = (String) data.get(FrameworkMBean.ERROR);
		Boolean success = (Boolean) data.get(FrameworkMBean.SUCCESS);
		return new BatchActionResult(completedItems, error, remainingItems, success, bundleInError);
	}

	/**
	 * Gets remaining items id's.
	 * @return the remainingItems.
	 */
	public long[] getRemainingItems() {
		return remainingItems;
	}

	/**
	 * Gets bundle in error id.
	 * @return the bundleInError.
	 */
	public long getBundleInError() {
		return bundleInError;
	}
	
}

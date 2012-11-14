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

import org.osgi.jmx.framework.FrameworkMBean;

/**
 * <p>
 * <tt>BatchResult</tt> represents abstract class for BatchResults.
 * It contains common data structure of batch result:
 * <ul>
 * <li>completed containing the list of bundles completing the batch operation.</li>
 * <li>error containing the error message of the batch operation.</li>
 * <li>success indicates if this operation was successful.</li>
 * </ul>
 * </p>
 * 
 * 
 * @version $Rev$ $Date$
 */
public abstract class BatchResult {

    /**
     * @see FrameworkMBean#COMPLETED_ITEM
     * @see FrameworkMBean#COMPLETED
     */
    protected long[] completed;
    /**
     * @see FrameworkMBean#ERROR_ITEM
     * @see FrameworkMBean#ERROR
     */
    protected String error;
    /**
     * @see FrameworkMBean#SUCCESS_ITEM
     * @see FrameworkMBean#SUCCESS
     */
    protected boolean success;

    /**
     * Gets completed item id's.
     * @return completed items id's.
     */
    public long[] getCompleted() {
        return completed;
    }

    /**
     * Gets error message.
     * @return error message.
     */
    public String getError() {
        return error;
    }

    /**
     * Gets success value.
     * @return true if success false if not.
     */
    public boolean isSuccess() {
        return success;
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

}
/**
 *  Licensed to4the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership. The ASF licenses this file to
 *  You under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the
 *  License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.samples.daytrader.api;

import org.apache.geronimo.samples.daytrader.util.Log;
import org.apache.geronimo.samples.daytrader.util.ServiceUtilities;


/**
 * TradeServiceUtilities provides servlet specific client side
 * utility functions.
 */
public class TradeServiceUtilities {

    /**
     * Lookup and return the TradeServices osgi service
     * 
     * @return TradeServices
     * 
     */
    public static final TradeServices getTradeServices() {
        if (Log.doTrace())
            Log.trace("TradeServiceUtilities:getTradeServices()");
        return getTradeServices(null);
    }

    /**
     * Lookup and return the TradeServices osgi service with filter
     * 
     * @return TradeServices
     * 
     */
    public static final TradeServices getTradeServices(String filter) {
        if (Log.doTrace())
            Log.trace("TradeServiceUtilities:getTradeServices()" , filter);
        return (TradeServices) ServiceUtilities.getOSGIService(TradeServices.class.getName(), filter);
    }

    /**
     * Lookup and return the TradeServicesManager osgi service
     * 
     * @return TradeServicesManager
     * 
     */
    public static final TradeServicesManager getTradeServicesManager() {
        if (Log.doTrace())
            Log.trace("TradeServiceUtilities:getTradeServicesManager()");
        return (TradeServicesManager) ServiceUtilities.getOSGIService(TradeServicesManager.class.getName());
    }

    /**
     * Lookup and return the TradeDBManager osgi service
     * 
     * @return TradeDBManager
     * 
     */
    public static final TradeDBManager getTradeDBManager() {
        if (Log.doTrace())
            Log.trace("TradeServiceUtilities:getTradeDBManager()");
        return (TradeDBManager) ServiceUtilities.getOSGIService(TradeDBManager.class.getName());
    }

}
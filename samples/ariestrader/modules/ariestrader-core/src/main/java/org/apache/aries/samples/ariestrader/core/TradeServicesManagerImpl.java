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
package org.apache.aries.samples.ariestrader.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.aries.samples.ariestrader.api.persistence.MarketSummaryDataBean;
import org.apache.aries.samples.ariestrader.util.Log;
import org.apache.aries.samples.ariestrader.util.TradeConfig;
import org.apache.aries.samples.ariestrader.api.TradeServicesManager;
import org.apache.aries.samples.ariestrader.api.TradeServices;


/**
 * TradeServicesManagerImpl coordinates access to the currently
 * selected TradeServices implementation and manages the list of
 * currently available TradeServices implementations.
 * 
 * @see
 *      org.apache.geronimo.samples.daytrader.api.TradeServicesManager
 * 
 */

public class TradeServicesManagerImpl implements TradeServicesManager {

    private static TradeServices[] tradeServicesList = new TradeServices[TradeConfig.runTimeModeNames.length] ;

    // This lock is used to serialize market summary operations.
    private static final Integer marketSummaryLock = new Integer(0);
    private static long nextMarketSummary = System.currentTimeMillis();
    private static MarketSummaryDataBean cachedMSDB = null; 
    
    /**
      * TradeServicesManagerImpl null constructor
      */
    public TradeServicesManagerImpl() {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl()");
    }

    /**
      * init
      */
    public void init() {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:init()");
    }


    /**
      * Get CurrentModes that are registered
      */
    public ArrayList<Integer> getCurrentModes() {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:getCurrentModes()");
        ArrayList<Integer> modes = new ArrayList<Integer>();
        for (int i=0; i<tradeServicesList.length; i++) {
            TradeServices tradeServicesRef = tradeServicesList[i];
            if (tradeServicesRef != null) {
                modes.add(i);
            }
        }
        return modes;
    }

    /**
      * Get TradeServices reference
      */
    public TradeServices getTradeServices() {
        if (Log.doTrace()) 
            Log.trace("TradeServicesManagerImpl:getTradeServices()");
        return tradeServicesList[TradeConfig.getRunTimeMode().ordinal()];
    }

    /**
      * Bind a new TradeServices implementation
      */
    public void bindService(TradeServices tradeServices, Map props) {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:bindService()", tradeServices, props);
        if (tradeServices != null) {
            String mode = (String) props.get("mode");
            tradeServicesList[Enum.valueOf(TradeConfig.ModeType.class, mode).ordinal()] = tradeServices;
        }
    }

    /**
      * Unbind a TradeServices implementation
      */
    public void unbindService(TradeServices tradeServices, Map props) {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:unbindService()", tradeServices, props);
        if (tradeServices != null) {
            String mode = (String) props.get("mode");
            tradeServicesList[Enum.valueOf(TradeConfig.ModeType.class, mode).ordinal()] = null;
        }
    }

    /**
     * Market Summary is inherently a heavy database operation.  For servers that have a caching
     * story this is a great place to cache data that is good for a period of time.  In order to
     * provide a flexible framework for this we allow the market summary operation to be
     * invoked on every transaction, time delayed or never.  This is configurable in the 
     * configuration panel.  
     *
     * @return An instance of the market summary
     */
    public MarketSummaryDataBean getMarketSummary() throws Exception {
    
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getMarketSummary()");
        }
    
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:getMarketSummary()");

        if (TradeConfig.getMarketSummaryInterval() == 0) return getMarketSummaryInternal();
        if (TradeConfig.getMarketSummaryInterval() < 0) return cachedMSDB;
    
        /**
         * This is a little funky.  If its time to fetch a new Market summary then we'll synchronize
         * access to make sure only one requester does it.  Others will merely return the old copy until
         * the new MarketSummary has been executed.
         */
         long currentTime = System.currentTimeMillis();
         
         if (currentTime > nextMarketSummary) {
             long oldNextMarketSummary = nextMarketSummary;
             boolean fetch = false;

             synchronized (marketSummaryLock) {
                 /**
                  * Is it still ahead or did we miss lose the race?  If we lost then let's get out
                  * of here as the work has already been done.
                  */
                 if (oldNextMarketSummary == nextMarketSummary) {
                     fetch = true;
                     nextMarketSummary += TradeConfig.getMarketSummaryInterval()*1000;
                     
                     /** 
                      * If the server has been idle for a while then its possible that nextMarketSummary
                      * could be way off.  Rather than try and play catch up we'll simply get in sync with the 
                      * current time + the interval.
                      */ 
                     if (nextMarketSummary < currentTime) {
                         nextMarketSummary = currentTime + TradeConfig.getMarketSummaryInterval()*1000;
                     }
                 }
             }

            /**
             * If we're the lucky one then let's update the MarketSummary
             */
            if (fetch) {
                cachedMSDB = getMarketSummaryInternal();
            }
        }
         
        return cachedMSDB;
    }

    /**
     * Compute and return a snapshot of the current market conditions This
     * includes the TSIA - an index of the price of the top 100 Trade stock
     * quotes The openTSIA ( the index at the open) The volume of shares traded,
     * Top Stocks gain and loss
     *
     * @return A snapshot of the current market summary
     */
    private MarketSummaryDataBean getMarketSummaryInternal() throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getMarketSummaryInternal()");
        }
        MarketSummaryDataBean marketSummaryData = null;
        marketSummaryData = tradeServicesList[TradeConfig.getRunTimeMode().ordinal()].getMarketSummary();
        return marketSummaryData;
    }


}

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
package org.apache.geronimo.samples.daytrader.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.geronimo.samples.daytrader.persistence.api.MarketSummaryDataBean;

/**
  * TradeServicesManager interface provides an interface to be
  * used for managing the implementations of TradeServices that
  * are available.
  * 
  */ 
public interface TradeServicesManager {


    /**
      * Get CurrentModes that are registered
      */
    public ArrayList<Integer> getCurrentModes();

    /**
      * Set TradeServicesList reference
      */
    public void setTradeServicesList(List tradeList);


    /**
      * Get the currently selected TradeServices
      */
    public TradeServices getTradeServices();

    /**
      * Compute and return a snapshot of the current market
      * conditions.  This includes the TSIA - and index of the prive
      * of the top 100 Trade stock quotes.  Ths openTSIA(the index
      * at the open), The volune of shares traded, Top Stocks gain
      * and loss.
      * 
      * This is a special version of this function which will cache
      * the results provided by the currently selected
      * TradeServices.
      * 
      * @return A snapshot of the current market summary
      */
    public MarketSummaryDataBean getMarketSummary() throws Exception;


}   


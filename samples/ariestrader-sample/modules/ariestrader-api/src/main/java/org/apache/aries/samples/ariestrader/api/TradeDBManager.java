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
package org.apache.aries.samples.ariestrader.api;

import org.apache.aries.samples.ariestrader.api.persistence.RunStatsDataBean;

/**
  * TradeDBManager interface centralizes and simplifies the DB
  * configuartion methods that are shared by some TradeServices
  * implementations.
  *
  */ 
public interface TradeDBManager {

    /**
     * Return a String containing the DBProductName configured for
     * the current DataSource
     * 
     * used by TradeBuildDB
     *
     * @return A String of the currently configured DataSource
     * 
     */
    public String checkDBProductName() throws Exception;

    /**
     * Recreate DataBase Tables for AriesTrader
     * 
     * used by TradeBuildDB
     *
     * @return boolean of success/failure in recreate of DB tables
     * 
     */
    public boolean recreateDBTables(Object[] sqlBuffer, java.io.PrintWriter out) throws Exception;

    /**
     * Reset the statistics for the Test AriesTrader Scenario
     * 
     * used by TradeConfigServlet
     *
     * @return the RunStatsDataBean
     * 
     */
    public RunStatsDataBean resetTrade(boolean deleteAll) throws Exception;
}   

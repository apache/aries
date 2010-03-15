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

import javax.naming.InitialContext;

import javax.sql.DataSource;

import org.apache.aries.samples.ariestrader.persistence.api.RunStatsDataBean;
import org.apache.aries.samples.ariestrader.util.Log;
import org.apache.aries.samples.ariestrader.util.MDBStats;
import org.apache.aries.samples.ariestrader.util.ServiceUtilities;
import org.apache.aries.samples.ariestrader.util.TradeConfig;
import org.apache.aries.samples.ariestrader.api.TradeDBManager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * TradeDBManagerImpl centralizes and simplifies the DB
 * configuartion methods that are shared by some TradeServices
 * implementations.
 * 
 * @see
 *      org.apache.aries.samples.ariestrader.api.TradeDBManager
 */

public class TradeDBManagerImpl implements TradeDBManager {

    private DataSource dataSource = null;

    private static InitialContext context;

    private static boolean initialized = false;

    private static int connCount = 0;

    private static Integer lock = new Integer(0);

    /**
     * Zero arg constructor for TradeDBManagerImpl
     */
    public TradeDBManagerImpl() {
    }

    /**
     * set data source
     */
    public void setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
    }


    /**
     * Return a String containing the DBProductName configured for
     * the current DataSource
     * 
     * used by TradeBuildDB
     *
     * @return A String of the currently configured DataSource
     * 
     */
    public String checkDBProductName() throws Exception {
        Connection conn = null;
        String dbProductName = null;

        try {
            if (Log.doTrace())
                Log.traceEnter("TradeDBManagerImpl:checkDBProductName");

            conn = getConn();
            DatabaseMetaData dbmd = conn.getMetaData();
            dbProductName = dbmd.getDatabaseProductName();
        }
        catch (SQLException e) {
            Log.error(e, "TradeDBManagerImpl:checkDBProductName() -- Error checking the AriesTrader Database Product Name");
        }
        finally {
            releaseConn(conn);
        }
        return dbProductName;
    }

    /**
     * Recreate DataBase Tables for AriesTrader
     * 
     * used by TradeBuildDB
     *
     * @return boolean of success/failure in recreate of DB tables
     * 
     */
    public boolean recreateDBTables(Object[] sqlBuffer, java.io.PrintWriter out) throws Exception {
        // Clear MDB Statistics
        MDBStats.getInstance().reset();

        Connection conn = null;
        boolean success = false;
        try {
            if (Log.doTrace())
                Log.traceEnter("TradeDBManagerImpl:recreateDBTables");

            conn = getConn();
            Statement stmt = conn.createStatement();
            int bufferLength = sqlBuffer.length;
            for (int i = 0; i < bufferLength; i++) {
                try {
                    stmt.executeUpdate((String) sqlBuffer[i]);
                }
                catch (SQLException ex) {
                    // Ignore DROP statements as tables won't always exist.
                    if (((String) sqlBuffer[i]).indexOf("DROP TABLE") < 0) {
                        Log.error("TradeDBManagerImpl:recreateDBTables SQL Exception thrown on executing the foll sql command: "
                                  + sqlBuffer[i], ex);
                        out.println("<BR>SQL Exception thrown on executing the foll sql command: <I>" + sqlBuffer[i]
                                    + "</I> . Check log for details.</BR>");
                    }
                }
            }
            stmt.close();
            commit(conn);
            success = true;
        }
        catch (Exception e) {
            Log.error(e, "TradeDBManagerImpl:recreateDBTables() -- Error dropping and recreating the database tables");
        }
        finally {
            releaseConn(conn);
        }
        return success;
    }

    /**
     * Reset the statistics for the Test AriesTrader Scenario
     * 
     * used by TradeConfigServlet
     *
     * @return the RunStatsDataBean
     * 
     */
    public RunStatsDataBean resetTrade(boolean deleteAll) throws Exception {
        // Clear MDB Statistics
        MDBStats.getInstance().reset();

        // Reset Trade

        RunStatsDataBean runStatsData = new RunStatsDataBean();
        Connection conn = null;
        try {
            if (Log.doTrace())
                Log.traceEnter("TradeDBManagerImpl:resetTrade deleteAll rows=" + deleteAll);

            conn = getConn();
            PreparedStatement stmt = null;
            ResultSet rs = null;

            if (deleteAll) {
                try {
                    stmt = getStatement(conn, "delete from quoteejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from accountejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from accountprofileejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from holdingejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from orderejb");
                    stmt.executeUpdate();
                    stmt.close();
                    // FUTURE: - DuplicateKeyException - For now, don't start at
                    // zero as KeySequenceDirect and KeySequenceBean will still
                    // give out
                    // the cached Block and then notice this change. Better
                    // solution is
                    // to signal both classes to drop their cached blocks
                    // stmt = getStatement(conn, "delete from keygenejb");
                    // stmt.executeUpdate();
                    // stmt.close();
                    commit(conn);
                }
                catch (Exception e) {
                    Log.error(e, "TradeDBManagerImpl:resetTrade(deleteAll) -- Error deleting Trade users and stock from the Trade database");
                }
                return runStatsData;
            }

            stmt = getStatement(conn, "delete from holdingejb where holdingejb.account_accountid is null");
            int x = stmt.executeUpdate();
            stmt.close();

            // Count and Delete newly registered users (users w/ id that start
            // "ru:%":
            stmt = getStatement(conn, "delete from accountprofileejb where userid like 'ru:%'");
            int rowCount = stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn,
                                "delete from orderejb where account_accountid in (select accountid from accountejb a where a.profile_userid like 'ru:%')");
            rowCount = stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn,
                                "delete from holdingejb where account_accountid in (select accountid from accountejb a where a.profile_userid like 'ru:%')");
            rowCount = stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn, "delete from accountejb where profile_userid like 'ru:%'");
            int newUserCount = stmt.executeUpdate();
            runStatsData.setNewUserCount(newUserCount);
            stmt.close();

            // Count of trade users
            stmt = getStatement(conn,
                                "select count(accountid) as \"tradeUserCount\" from accountejb a where a.profile_userid like 'uid:%'");
            rs = stmt.executeQuery();
            rs.next();
            int tradeUserCount = rs.getInt("tradeUserCount");
            runStatsData.setTradeUserCount(tradeUserCount);
            stmt.close();

            rs.close();
            // Count of trade stocks
            stmt = getStatement(conn,
                                "select count(symbol) as \"tradeStockCount\" from quoteejb a where a.symbol like 's:%'");
            rs = stmt.executeQuery();
            rs.next();
            int tradeStockCount = rs.getInt("tradeStockCount");
            runStatsData.setTradeStockCount(tradeStockCount);
            stmt.close();

            // Count of trade users login, logout
            stmt = getStatement(conn,
                                "select sum(loginCount) as \"sumLoginCount\", sum(logoutCount) as \"sumLogoutCount\" from accountejb a where  a.profile_userID like 'uid:%'");
            rs = stmt.executeQuery();
            rs.next();
            int sumLoginCount = rs.getInt("sumLoginCount");
            int sumLogoutCount = rs.getInt("sumLogoutCount");
            runStatsData.setSumLoginCount(sumLoginCount);
            runStatsData.setSumLogoutCount(sumLogoutCount);
            stmt.close();

            rs.close();
            // Update logoutcount and loginCount back to zero

            stmt =
            getStatement(conn, "update accountejb set logoutCount=0,loginCount=0 where profile_userID like 'uid:%'");
            rowCount = stmt.executeUpdate();
            stmt.close();

            // count holdings for trade users
            stmt = getStatement(conn,
                               "select count(holdingid) as \"holdingCount\" from holdingejb h where h.account_accountid in "
                               + "(select accountid from accountejb a where a.profile_userid like 'uid:%')");

            rs = stmt.executeQuery();
            rs.next();
            int holdingCount = rs.getInt("holdingCount");
            runStatsData.setHoldingCount(holdingCount);
            stmt.close();
            rs.close();

            // count orders for trade users
            stmt = getStatement(conn,
                                "select count(orderid) as \"orderCount\" from orderejb o where o.account_accountid in "
                                + "(select accountid from accountejb a where a.profile_userid like 'uid:%')");

            rs = stmt.executeQuery();
            rs.next();
            int orderCount = rs.getInt("orderCount");
            runStatsData.setOrderCount(orderCount);
            stmt.close();
            rs.close();

            // count orders by type for trade users
            stmt = getStatement(conn,
                                "select count(orderid) \"buyOrderCount\"from orderejb o where (o.account_accountid in "
                                + "(select accountid from accountejb a where a.profile_userid like 'uid:%')) AND "
                                + " (o.orderType='buy')");

            rs = stmt.executeQuery();
            rs.next();
            int buyOrderCount = rs.getInt("buyOrderCount");
            runStatsData.setBuyOrderCount(buyOrderCount);
            stmt.close();
            rs.close();

            // count orders by type for trade users
            stmt = getStatement(conn,
                                "select count(orderid) \"sellOrderCount\"from orderejb o where (o.account_accountid in "
                                + "(select accountid from accountejb a where a.profile_userid like 'uid:%')) AND "
                                + " (o.orderType='sell')");

            rs = stmt.executeQuery();
            rs.next();
            int sellOrderCount = rs.getInt("sellOrderCount");
            runStatsData.setSellOrderCount(sellOrderCount);
            stmt.close();
            rs.close();

            // Delete cancelled orders
            stmt = getStatement(conn, "delete from orderejb where orderStatus='cancelled'");
            int cancelledOrderCount = stmt.executeUpdate();
            runStatsData.setCancelledOrderCount(cancelledOrderCount);
            stmt.close();
            rs.close();

            // count open orders by type for trade users
            stmt = getStatement(conn,
                                "select count(orderid) \"openOrderCount\"from orderejb o where (o.account_accountid in "
                                + "(select accountid from accountejb a where a.profile_userid like 'uid:%')) AND "
                                + " (o.orderStatus='open')");

            rs = stmt.executeQuery();
            rs.next();
            int openOrderCount = rs.getInt("openOrderCount");
            runStatsData.setOpenOrderCount(openOrderCount);

            stmt.close();
            rs.close();
            // Delete orders for holding which have been purchased and sold
            stmt = getStatement(conn, "delete from orderejb where holding_holdingid is null");
            int deletedOrderCount = stmt.executeUpdate();
            runStatsData.setDeletedOrderCount(deletedOrderCount);
            stmt.close();
            rs.close();

            commit(conn);

            System.out.println("TradeDBManagerImpl:reset Run stats data\n\n" + runStatsData);
        }
        catch (Exception e) {
            Log.error(e, "Failed to reset Trade");
            rollBack(conn, e);
            throw e;
        }
        finally {
            releaseConn(conn);
        }
        return runStatsData;

    }

    private void releaseConn(Connection conn) throws Exception {
        try {
            if (conn != null) {
                conn.close();
                if (Log.doTrace()) {
                    synchronized (lock) {
                        connCount--;
                    }
                    Log.trace("TradeDBManagerImpl:releaseConn -- connection closed, connCount=" + connCount);
                }
            }
        }
        catch (Exception e) {
            Log.error("TradeDBManagerImpl:releaseConnection -- failed to close connection", e);
        }
    }

    /*
     * Lookup the TradeData DataSource
     */
    private void lookupDataSource() throws Exception {
        if (dataSource == null) {
            dataSource = (DataSource) ServiceUtilities.getOSGIService(DataSource.class.getName(),TradeConfig.OSGI_DS_NAME_FILTER);
        }
    }

    /*
     * Allocate a new connection to the datasource
     */
    private Connection getConn() throws Exception {

        Connection conn = null;
        lookupDataSource();
        conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        if (Log.doTrace()) {
            synchronized (lock) {
                connCount++;
            }
            Log.trace("TradeDBManagerImpl:getConn -- new connection allocated, IsolationLevel="
                      + conn.getTransactionIsolation() + " connectionCount = " + connCount);
        }

        return conn;
    }

    /*
     * Commit the provided connection 
     */
    private void commit(Connection conn) throws Exception {
        if (conn != null)
            conn.commit();
    }

    /*
     * Rollback the statement for the given connection
     */
    private void rollBack(Connection conn, Exception e) throws Exception {
        Log.log("TradeDBManagerImpl:rollBack -- rolling back conn due to previously caught exception");
        if (conn != null)
            conn.rollback();
        else
            throw e; // Throw the exception
    }

    /*
     * Allocate a new prepared statment for this connection
     */
    private PreparedStatement getStatement(Connection conn, String sql) throws Exception {
        return conn.prepareStatement(sql);
    }


    public void init() {
        if (initialized)
            return;
        if (Log.doTrace())
            Log.trace("TradeDBManagerImpl:init -- *** initializing");

        if (Log.doTrace())
            Log.trace("TradeDBManagerImpl:init -- +++ initialized");

        initialized = true;
    }

    public void destroy() {
        try {
            Log.trace("TradeDBManagerImpl:destroy");
            if (!initialized)
                return;
        }
        catch (Exception e) {
            Log.error("TradeDBManagerImpl:destroy", e);
        }
    }

}

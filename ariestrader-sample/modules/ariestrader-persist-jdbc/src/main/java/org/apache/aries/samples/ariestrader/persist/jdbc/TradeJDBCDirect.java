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
package org.apache.aries.samples.ariestrader.persist.jdbc;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.ArrayList;
import javax.naming.InitialContext;

import javax.sql.DataSource;

import org.apache.aries.samples.ariestrader.api.TradeServices;
import org.apache.aries.samples.ariestrader.beans.AccountDataBeanImpl;
import org.apache.aries.samples.ariestrader.beans.AccountProfileDataBeanImpl;
import org.apache.aries.samples.ariestrader.beans.HoldingDataBeanImpl;
import org.apache.aries.samples.ariestrader.beans.OrderDataBeanImpl;
import org.apache.aries.samples.ariestrader.beans.QuoteDataBeanImpl;
import org.apache.aries.samples.ariestrader.persistence.api.AccountDataBean;
import org.apache.aries.samples.ariestrader.persistence.api.AccountProfileDataBean;
import org.apache.aries.samples.ariestrader.persistence.api.HoldingDataBean;
import org.apache.aries.samples.ariestrader.persistence.api.MarketSummaryDataBean;
import org.apache.aries.samples.ariestrader.persistence.api.OrderDataBean;
import org.apache.aries.samples.ariestrader.persistence.api.QuoteDataBean;
import org.apache.aries.samples.ariestrader.util.FinancialUtils;
import org.apache.aries.samples.ariestrader.util.Log;
import org.apache.aries.samples.ariestrader.util.TradeConfig;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;


/**
 * TradeJDBCDirect uses direct JDBC access to a
 * <code>javax.sql.DataSource</code> to implement the business methods of the
 * Trade online broker application. These business methods represent the
 * features and operations that can be performed by customers of the brokerage
 * such as login, logout, get a stock quote, buy or sell a stock, etc. and are
 * specified in the {@link org.apache.aries.samples.ariestrader.TradeServices}
 * interface
 * 
 * Note: In order for this class to be thread-safe, a new TradeJDBC must be
 * created for each call to a method from the TradeInterface interface.
 * Otherwise, pooled connections may not be released.
 * 
 * @see org.apache.aries.samples.ariestrader.TradeServices
 * 
 */

public class TradeJDBCDirect implements TradeServices {

    private static String dsName = TradeConfig.DATASOURCE;

//    private static DataSource dataSource = null;
    private DataSource dataSource;

    private static BigDecimal ZERO = new BigDecimal(0.0);

    private boolean inGlobalTxn = false;

    private boolean inSession = false;

    private static InitialContext context;

    private static int connCount = 0;

    private static Integer lock = new Integer(0);

    private static boolean initialized = false;

    /**
     * Zero arg constructor for TradeJDBCDirect
     */
    public TradeJDBCDirect() {
    }

    public TradeJDBCDirect(boolean inSession) {
        this.inSession = inSession;
    }

    /**
     * set data source
     */
    public void setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
    }

    /**
     * setInSession
     */
    public void setInSession(boolean inSession) {
        this.inSession = inSession;
    }

    /**
     * @see TradeServices#getMarketSummary()
     */
    public MarketSummaryDataBean getMarketSummary() throws Exception {
        MarketSummaryDataBean marketSummaryData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getMarketSummary - inSession(" + this.inSession + ")");

            conn = getConn();
            PreparedStatement stmt =
                getStatement(conn, getTSIAQuotesOrderByChangeSQL, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            ArrayList topGainersData = new ArrayList(5);
            ArrayList topLosersData = new ArrayList(5);

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next() && (count++ < 5)) {
                QuoteDataBean quoteData = getQuoteDataFromResultSet(rs);
                topLosersData.add(quoteData);
            }

            stmt.close();
            stmt =
                getStatement(conn, "select * from quoteejb q where q.symbol like 's:1__' order by q.change1 DESC",
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery();

            count = 0;
            while (rs.next() && (count++ < 5)) {
                QuoteDataBean quoteData = getQuoteDataFromResultSet(rs);
                topGainersData.add(quoteData);
            }

            stmt.close();

            BigDecimal TSIA = ZERO;
            BigDecimal openTSIA = ZERO;
            double volume = 0.0;

            if ((topGainersData.size() > 0) || (topLosersData.size() > 0)) {

                stmt = getStatement(conn, getTSIASQL);
                rs = stmt.executeQuery();

                if (!rs.next())
                    Log.error("TradeJDBCDirect:getMarketSummary -- error w/ getTSIASQL -- no results");
                else
                    TSIA = rs.getBigDecimal("TSIA");
                stmt.close();

                stmt = getStatement(conn, getOpenTSIASQL);
                rs = stmt.executeQuery();

                if (!rs.next())
                    Log.error("TradeJDBCDirect:getMarketSummary -- error w/ getOpenTSIASQL -- no results");
                else
                    openTSIA = rs.getBigDecimal("openTSIA");
                stmt.close();

                stmt = getStatement(conn, getTSIATotalVolumeSQL);
                rs = stmt.executeQuery();

                if (!rs.next())
                    Log.error("TradeJDBCDirect:getMarketSummary -- error w/ getTSIATotalVolumeSQL -- no results");
                else
                    volume = rs.getDouble("totalVolume");
                stmt.close();
            }
            commit(conn);

            marketSummaryData = new MarketSummaryDataBean(TSIA, openTSIA, volume, topGainersData, topLosersData);

        }

        catch (Exception e) {
            Log.error("TradeJDBCDirect:getMarketSummary -- error getting summary", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return marketSummaryData;

    }

    /**
     * @see TradeServices#buy(String, String, double)
     */
    public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) throws Exception {

        Connection conn = null;
        OrderDataBean orderData = null;

        /*
         * total = (quantity * purchasePrice) + orderFee
         */
        BigDecimal total;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:buy - inSession(" + this.inSession + ")", userID, symbol, new Double(quantity));

            conn = getConn();

            AccountDataBean accountData = getAccountData(conn, userID);
            QuoteDataBean quoteData = getQuoteData(conn, symbol);
            HoldingDataBean holdingData = null; // the buy operation will create
            // the holding

            orderData = createOrder(conn, accountData, quoteData, holdingData, "buy", quantity);

            // Update -- account should be credited during completeOrder
            BigDecimal price = quoteData.getPrice();
            BigDecimal orderFee = orderData.getOrderFee();
            total = (new BigDecimal(quantity).multiply(price)).add(orderFee);
            // subtract total from account balance
            creditAccountBalance(conn, accountData, total.negate());

            completeOrder(conn, orderData.getOrderID());

            orderData = getOrderData(conn, orderData.getOrderID().intValue());

            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:buy error - rolling back", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }

        //after the purchase or sell of a stock, update the stocks volume and
        // price
        updateQuotePriceVolume(symbol, TradeConfig.getRandomPriceChangeFactor(), quantity);

        return orderData;
    }

    /**
     * @see TradeServices#sell(String, Integer)
     */
    public OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) throws Exception {

        Connection conn = null;
        OrderDataBean orderData = null;

        /*
         * total = (quantity * purchasePrice) + orderFee
         */
        BigDecimal total;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:sell - inSession(" + this.inSession + ")", userID, holdingID);

            conn = getConn();

            AccountDataBean accountData = getAccountData(conn, userID);
            HoldingDataBean holdingData = getHoldingData(conn, holdingID.intValue());
            QuoteDataBean quoteData = null;
            if (holdingData != null)
                quoteData = getQuoteData(conn, holdingData.getQuoteID());

            if ((accountData == null) || (holdingData == null) || (quoteData == null)) {
                String error =
                    "TradeJDBCDirect:sell -- error selling stock -- unable to find:  \n\taccount=" + accountData
                        + "\n\tholding=" + holdingData + "\n\tquote=" + quoteData + "\nfor user: " + userID
                        + " and holdingID: " + holdingID;
                Log.error(error);
                rollBack(conn, new Exception(error));

                return orderData;
            }

            double quantity = holdingData.getQuantity();

            orderData = createOrder(conn, accountData, quoteData, holdingData, "sell", quantity);

            // Set the holdingSymbol purchaseDate to selling to signify the sell
            // is "inflight"
            updateHoldingStatus(conn, holdingData.getHoldingID(), holdingData.getQuoteID());

            // UPDATE -- account should be credited during completeOrder
            BigDecimal price = quoteData.getPrice();
            BigDecimal orderFee = orderData.getOrderFee();
            total = (new BigDecimal(quantity).multiply(price)).subtract(orderFee);
            creditAccountBalance(conn, accountData, total);

            completeOrder(conn, orderData.getOrderID());

            orderData = getOrderData(conn, orderData.getOrderID().intValue());

            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:sell error", e);
            rollBack(conn, e);

        } finally {
            releaseConn(conn);
        }

        if (!(orderData.getOrderStatus().equalsIgnoreCase("cancelled")))
            //after the purchase or sell of a stock, update the stocks volume
            // and price
            updateQuotePriceVolume(orderData.getSymbol(), TradeConfig.getRandomPriceChangeFactor(), orderData.getQuantity());

        return orderData;
    }

    /**
     * @see TradeServices#queueOrder(Integer)
     */
    public void queueOrder(Integer orderID, boolean twoPhase) throws Exception {
        throw new RuntimeException("TradeServices#queueOrder(Integer) is not supported in this runtime mode");
    }

    /**
     * @see TradeServices#completeOrder(Integer)
     */
    public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception {
        OrderDataBean orderData = null;
        Connection conn = null;

        try { // twoPhase

            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:completeOrder - inSession(" + this.inSession + ")", orderID);
            setInGlobalTxn(!inSession && twoPhase);
            conn = getConn();
            orderData = completeOrder(conn, orderID);
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:completeOrder -- error completing order", e);
            rollBack(conn, e);
            cancelOrder(orderID, twoPhase);
        } finally {
            releaseConn(conn);
        }

        return orderData;

    }

    private OrderDataBean completeOrder(Connection conn, Integer orderID) throws Exception {

        OrderDataBean orderData = null;
        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:completeOrderInternal - inSession(" + this.inSession + ")", orderID);

        PreparedStatement stmt = getStatement(conn, getOrderSQL);
        stmt.setInt(1, orderID.intValue());

        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            Log.error("TradeJDBCDirect:completeOrder -- unable to find order: " + orderID);
            stmt.close();
            return orderData;
        }
        orderData = getOrderDataFromResultSet(rs);

        String orderType = orderData.getOrderType();
        String orderStatus = orderData.getOrderStatus();

        // if (order.isCompleted())
        if ((orderStatus.compareToIgnoreCase("completed") == 0)
            || (orderStatus.compareToIgnoreCase("alertcompleted") == 0)
            || (orderStatus.compareToIgnoreCase("cancelled") == 0))
            throw new Exception("TradeJDBCDirect:completeOrder -- attempt to complete Order that is already completed");

        int accountID = rs.getInt("account_accountID");
        String quoteID = rs.getString("quote_symbol");
        int holdingID = rs.getInt("holding_holdingID");

        BigDecimal price = orderData.getPrice();
        double quantity = orderData.getQuantity();
        BigDecimal orderFee = orderData.getOrderFee();

        // get the data for the account and quote
        // the holding will be created for a buy or extracted for a sell

        /*
         * Use the AccountID and Quote Symbol from the Order AccountDataBean accountData = getAccountData(accountID,
         * conn); QuoteDataBean quoteData = getQuoteData(conn, quoteID);
         */
        String userID = getAccountProfileData(conn, new Integer(accountID)).getUserID();

        HoldingDataBean holdingData = null;

        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:completeOrder--> Completing Order " + orderData.getOrderID() + "\n\t Order info: "
                + orderData + "\n\t Account info: " + accountID + "\n\t Quote info: " + quoteID);

        // if (order.isBuy())
        if (orderType.compareToIgnoreCase("buy") == 0) {
            /*
             * Complete a Buy operation - create a new Holding for the Account - deduct the Order cost from the Account
             * balance
             */

            holdingData = createHolding(conn, accountID, quoteID, quantity, price);
            updateOrderHolding(conn, orderID.intValue(), holdingData.getHoldingID().intValue());
        }

        // if (order.isSell()) {
        if (orderType.compareToIgnoreCase("sell") == 0) {
            /*
             * Complete a Sell operation - remove the Holding from the Account - deposit the Order proceeds to the
             * Account balance
             */
            holdingData = getHoldingData(conn, holdingID);
            if (holdingData == null)
                Log.debug("TradeJDBCDirect:completeOrder:sell -- user: " + userID + " already sold holding: " + holdingID);
            else
                removeHolding(conn, holdingID, orderID.intValue());

        }

        updateOrderStatus(conn, orderData.getOrderID(), "closed");

        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:completeOrder--> Completed Order " + orderData.getOrderID() + "\n\t Order info: "
                + orderData + "\n\t Account info: " + accountID + "\n\t Quote info: " + quoteID + "\n\t Holding info: "
                + holdingData);

        stmt.close();

        commit(conn);

        // signify this order for user userID is complete
        orderCompleted(userID, orderID);

        return orderData;
    }

    /**
     * @see TradeServices#cancelOrder(Integer, boolean)
     */
    public void cancelOrder(Integer orderID, boolean twoPhase) throws Exception {
        OrderDataBean orderData = null;
        Connection conn = null;
        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:cancelOrder - inSession(" + this.inSession + ")", orderID);
            setInGlobalTxn(!inSession && twoPhase);
            conn = getConn();
            cancelOrder(conn, orderID);
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:cancelOrder -- error cancelling order: " + orderID, e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
    }

    private void cancelOrder(Connection conn, Integer orderID) throws Exception {
        updateOrderStatus(conn, orderID, "cancelled");
    }

    public void orderCompleted(String userID, Integer orderID) throws Exception {
//        throw new UnsupportedOperationException("TradeJDBCDirect:orderCompleted method not supported");
        if (Log.doTrace())
            Log.trace("OrderCompleted", userID, orderID);
    }

    private HoldingDataBean createHolding(Connection conn, int accountID, String symbol, double quantity,
        BigDecimal purchasePrice) throws Exception {
        HoldingDataBean holdingData = null;

        Timestamp purchaseDate = new Timestamp(System.currentTimeMillis());
        PreparedStatement stmt = getStatement(conn, createHoldingSQL);

        Integer holdingID = KeySequenceDirect.getNextID(conn, "holding", inSession, getInGlobalTxn());
        stmt.setInt(1, holdingID.intValue());
        stmt.setTimestamp(2, purchaseDate);
        stmt.setBigDecimal(3, purchasePrice);
        stmt.setDouble(4, quantity);
        stmt.setString(5, symbol);
        stmt.setInt(6, accountID);
        int rowCount = stmt.executeUpdate();

        stmt.close();

        return getHoldingData(conn, holdingID.intValue());
    }

    private void removeHolding(Connection conn, int holdingID, int orderID) throws Exception {
        PreparedStatement stmt = getStatement(conn, removeHoldingSQL);

        stmt.setInt(1, holdingID);
        int rowCount = stmt.executeUpdate();
        stmt.close();

        // set the HoldingID to NULL for the purchase and sell order now that
        // the holding as been removed
        stmt = getStatement(conn, removeHoldingFromOrderSQL);

        stmt.setInt(1, holdingID);
        rowCount = stmt.executeUpdate();
        stmt.close();

    }

    private OrderDataBean createOrder(Connection conn, AccountDataBean accountData, QuoteDataBean quoteData,
        HoldingDataBean holdingData, String orderType, double quantity) throws Exception {
        OrderDataBean orderData = null;

        Timestamp currentDate = new Timestamp(System.currentTimeMillis());

        PreparedStatement stmt = getStatement(conn, createOrderSQL);

        Integer orderID = KeySequenceDirect.getNextID(conn, "order", inSession, getInGlobalTxn());
        stmt.setInt(1, orderID.intValue());
        stmt.setString(2, orderType);
        stmt.setString(3, "open");
        stmt.setTimestamp(4, currentDate);
        stmt.setDouble(5, quantity);
        stmt.setBigDecimal(6, quoteData.getPrice().setScale(FinancialUtils.SCALE, FinancialUtils.ROUND));
        stmt.setBigDecimal(7, TradeConfig.getOrderFee(orderType));
        stmt.setInt(8, accountData.getAccountID().intValue());
        if (holdingData == null)
            stmt.setNull(9, java.sql.Types.INTEGER);
        else
            stmt.setInt(9, holdingData.getHoldingID().intValue());
        stmt.setString(10, quoteData.getSymbol());
        int rowCount = stmt.executeUpdate();

        stmt.close();

        return getOrderData(conn, orderID.intValue());
    }

    /**
     * @see TradeServices#getOrders(String)
     */
    public Collection getOrders(String userID) throws Exception {

        Collection orderDataBeans = new ArrayList();
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getOrders - inSession(" + this.inSession + ")", userID);

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getOrdersByUserSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();

            // TODO: return top 5 orders for now -- next version will add a
            // getAllOrders method
            // also need to get orders sorted by order id descending
            int i = 0;
            while ((rs.next()) && (i++ < 5)) {
                OrderDataBean orderData = getOrderDataFromResultSet(rs);
                orderDataBeans.add(orderData);
            }

            stmt.close();
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getOrders -- error getting user orders", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return orderDataBeans;
    }

    /**
     * @see TradeServices#getClosedOrders(String)
     */
    public Collection getClosedOrders(String userID) throws Exception {

        Collection orderDataBeans = new ArrayList();
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getClosedOrders - inSession(" + this.inSession + ")", userID);

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getClosedOrdersSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OrderDataBean orderData = getOrderDataFromResultSet(rs);
                orderData.setOrderStatus("completed");
                updateOrderStatus(conn, orderData.getOrderID(), orderData.getOrderStatus());
                orderDataBeans.add(orderData);

            }

            stmt.close();
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getOrders -- error getting user orders", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return orderDataBeans;
    }

    /**
     * @see TradeServices#createQuote(String, String, BigDecimal)
     */
    public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws Exception {

        QuoteDataBean quoteData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.traceEnter("TradeJDBCDirect:createQuote - inSession(" + this.inSession + ")");

            price = price.setScale(FinancialUtils.SCALE, FinancialUtils.ROUND);
            double volume = 0.0, change = 0.0;

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, createQuoteSQL);
            stmt.setString(1, symbol); // symbol
            stmt.setString(2, companyName); // companyName
            stmt.setDouble(3, volume); // volume
            stmt.setBigDecimal(4, price); // price
            stmt.setBigDecimal(5, price); // open
            stmt.setBigDecimal(6, price); // low
            stmt.setBigDecimal(7, price); // high
            stmt.setDouble(8, change); // change

            stmt.executeUpdate();
            stmt.close();
            commit(conn);

            quoteData = new QuoteDataBeanImpl(symbol, companyName, volume, price, price, price, price, change);
            if (Log.doTrace())
                Log.traceExit("TradeJDBCDirect:createQuote");
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:createQuote -- error creating quote", e);
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }

    /**
     * @see TradeServices#getQuote(String)
     */

    public QuoteDataBean getQuote(String symbol) throws Exception {

        QuoteDataBean quoteData = null;
        Connection conn = null;

        if ((symbol == null) || (symbol.length() == 0) || (symbol.length() > 10)) {
            if (Log.doTrace()) {
                Log.trace("TradeJDBCDirect:getQuote   ---  primitive workload");
            }
            return new QuoteDataBeanImpl("Invalid symbol", "", 0.0, FinancialUtils.ZERO, FinancialUtils.ZERO, FinancialUtils.ZERO, FinancialUtils.ZERO, 0.0);
        }

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getQuote - inSession(" + this.inSession + ")", symbol);

            conn = getConn();
            quoteData = getQuote(conn, symbol);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getQuote -- error getting quote", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }

    private QuoteDataBean getQuote(Connection conn, String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        PreparedStatement stmt = getStatement(conn, getQuoteSQL);
        stmt.setString(1, symbol); // symbol

        ResultSet rs = stmt.executeQuery();

        if (!rs.next())
            Log.error("TradeJDBCDirect:getQuote -- failure no result.next() for symbol: " + symbol);

        else
            quoteData = getQuoteDataFromResultSet(rs);

        stmt.close();

        return quoteData;
    }

    private QuoteDataBean getQuoteForUpdate(Connection conn, String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        PreparedStatement stmt = getStatement(conn, getQuoteForUpdateSQL);
        stmt.setString(1, symbol); // symbol

        ResultSet rs = stmt.executeQuery();

        if (!rs.next())
            Log.error("TradeJDBCDirect:getQuote -- failure no result.next()");

        else
            quoteData = getQuoteDataFromResultSet(rs);

        stmt.close();

        return quoteData;
    }

    /**
     * @see TradeServices#getAllQuotes(String)
     */
    public Collection getAllQuotes() throws Exception {

        Collection quotes = new ArrayList();
        QuoteDataBean quoteData = null;
        Connection conn = null;

        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:getAllQuotes");

        try {
            conn = getConn();

            PreparedStatement stmt = getStatement(conn, getAllQuotesSQL);

            ResultSet rs = stmt.executeQuery();

            while (!rs.next()) {
                quoteData = getQuoteDataFromResultSet(rs);
                quotes.add(quoteData);
            }

            stmt.close();
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getAllQuotes", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }

        return quotes;
    }

    /**
     * @see TradeServices#getHoldings(String)
     */
    public Collection getHoldings(String userID) throws Exception {

        Collection holdingDataBeans = new ArrayList();
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getHoldings - inSession(" + this.inSession + ")", userID);

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getHoldingsForUserSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                HoldingDataBean holdingData = getHoldingDataFromResultSet(rs);
                holdingDataBeans.add(holdingData);
            }

            stmt.close();
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getHoldings -- error getting user holings", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return holdingDataBeans;
    }

    /**
     * @see TradeServices#getHolding(Integer)
     */
    public HoldingDataBean getHolding(Integer holdingID) throws Exception {

        HoldingDataBean holdingData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getHolding - inSession(" + this.inSession + ")", holdingID);

            conn = getConn();
            holdingData = getHoldingData(holdingID.intValue());

            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getHolding -- error getting holding " + holdingID + "", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return holdingData;
    }

    /**
     * @see TradeServices#getAccountData(String)
     */
    public AccountDataBean getAccountData(String userID) throws Exception {

        try {
            AccountDataBean accountData = null;
            Connection conn = null;
            try {
                if (Log.doTrace())
                    Log.trace("TradeJDBCDirect:getAccountData - inSession(" + this.inSession + ")", userID);

                conn = getConn();
                accountData = getAccountData(conn, userID);
                commit(conn);

            } catch (Exception e) {
                Log.error("TradeJDBCDirect:getAccountData -- error getting account data", e);
                rollBack(conn, e);
            } finally {
                releaseConn(conn);
            }
            return accountData;
        } catch (Exception e) {
            throw new Exception(e.getMessage(), e);
        }
    }

    private AccountDataBean getAccountData(Connection conn, String userID) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountForUserSQL);
        stmt.setString(1, userID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }

    private AccountDataBean getAccountDataForUpdate(Connection conn, String userID) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountForUserForUpdateSQL);
        stmt.setString(1, userID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }

    /**
     * @see TradeServices#getAccountData(String)
     */
    public AccountDataBean getAccountData(int accountID) throws Exception {
        AccountDataBean accountData = null;
        Connection conn = null;
        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getAccountData - inSession(" + this.inSession + ")", new Integer(accountID));

            conn = getConn();
            accountData = getAccountData(accountID, conn);
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getAccountData -- error getting account data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountData;
    }

    private AccountDataBean getAccountData(int accountID, Connection conn) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountSQL);
        stmt.setInt(1, accountID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }

    private AccountDataBean getAccountDataForUpdate(int accountID, Connection conn) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountForUpdateSQL);
        stmt.setInt(1, accountID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }

    private QuoteDataBean getQuoteData(String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        Connection conn = null;
        try {
            conn = getConn();
            quoteData = getQuoteData(conn, symbol);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getQuoteData -- error getting data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }

    private QuoteDataBean getQuoteData(Connection conn, String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        PreparedStatement stmt = getStatement(conn, getQuoteSQL);
        stmt.setString(1, symbol);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next())
            Log.error("TradeJDBCDirect:getQuoteData -- could not find quote for symbol=" + symbol);
        else
            quoteData = getQuoteDataFromResultSet(rs);
        stmt.close();
        return quoteData;
    }

    private HoldingDataBean getHoldingData(int holdingID) throws Exception {
        HoldingDataBean holdingData = null;
        Connection conn = null;
        try {
            conn = getConn();
            holdingData = getHoldingData(conn, holdingID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getHoldingData -- error getting data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return holdingData;
    }

    private HoldingDataBean getHoldingData(Connection conn, int holdingID) throws Exception {
        HoldingDataBean holdingData = null;
        PreparedStatement stmt = getStatement(conn, getHoldingSQL);
        stmt.setInt(1, holdingID);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next())
            Log.error("TradeJDBCDirect:getHoldingData -- no results -- holdingID=" + holdingID);
        else
            holdingData = getHoldingDataFromResultSet(rs);

        stmt.close();
        return holdingData;
    }

    private OrderDataBean getOrderData(int orderID) throws Exception {
        OrderDataBean orderData = null;
        Connection conn = null;
        try {
            conn = getConn();
            orderData = getOrderData(conn, orderID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getOrderData -- error getting data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return orderData;
    }

    private OrderDataBean getOrderData(Connection conn, int orderID) throws Exception {
        OrderDataBean orderData = null;
        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:getOrderData(conn, " + orderID + ")");
        PreparedStatement stmt = getStatement(conn, getOrderSQL);
        stmt.setInt(1, orderID);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next())
            Log.error("TradeJDBCDirect:getOrderData -- no results for orderID:" + orderID);
        else
            orderData = getOrderDataFromResultSet(rs);
        stmt.close();
        return orderData;
    }

    /**
     * @see TradeServices#getAccountProfileData(String)
     */
    public AccountProfileDataBean getAccountProfileData(String userID) throws Exception {

        AccountProfileDataBean accountProfileData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getAccountProfileData - inSession(" + this.inSession + ")", userID);

            conn = getConn();
            accountProfileData = getAccountProfileData(conn, userID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getAccountProfileData -- error getting profile data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountProfileData;
    }

    private AccountProfileDataBean getAccountProfileData(Connection conn, String userID) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountProfileSQL);
        stmt.setString(1, userID);

        ResultSet rs = stmt.executeQuery();

        AccountProfileDataBean accountProfileData = getAccountProfileDataFromResultSet(rs);
        stmt.close();
        return accountProfileData;
    }

    private AccountProfileDataBean getAccountProfileData(Integer accountID) throws Exception {
        AccountProfileDataBean accountProfileData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:getAccountProfileData", accountID);

            conn = getConn();
            accountProfileData = getAccountProfileData(conn, accountID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getAccountProfileData -- error getting profile data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountProfileData;
    }

    private AccountProfileDataBean getAccountProfileData(Connection conn, Integer accountID) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountProfileForAccountSQL);
        stmt.setInt(1, accountID.intValue());

        ResultSet rs = stmt.executeQuery();

        AccountProfileDataBean accountProfileData = getAccountProfileDataFromResultSet(rs);
        stmt.close();
        return accountProfileData;
    }

    /**
     * @see TradeServices#updateAccountProfile(AccountProfileDataBean)
     */
    public AccountProfileDataBean updateAccountProfile(String userID, String password, String fullName, String address, String email, String creditcard) throws Exception {                              

        AccountProfileDataBean accountProfileData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:updateAccountProfileData - inSession(" + this.inSession + ")", userID);

            conn = getConn();
            updateAccountProfile(conn, userID, password, fullName, address, email, creditcard);

            accountProfileData = getAccountProfileData(conn, userID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:getAccountProfileData -- error getting profile data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountProfileData;
    }

    private void creditAccountBalance(Connection conn, AccountDataBean accountData, BigDecimal credit) throws Exception {
        PreparedStatement stmt = getStatement(conn, creditAccountBalanceSQL);

        stmt.setBigDecimal(1, credit);
        stmt.setInt(2, accountData.getAccountID().intValue());

        int count = stmt.executeUpdate();
        stmt.close();

    }

    // Set Timestamp to zero to denote sell is inflight
    // UPDATE -- could add a "status" attribute to holding
    private void updateHoldingStatus(Connection conn, Integer holdingID, String symbol) throws Exception {
        Timestamp ts = new Timestamp(0);
        PreparedStatement stmt = getStatement(conn, "update holdingejb set purchasedate= ? where holdingid = ?");

        stmt.setTimestamp(1, ts);
        stmt.setInt(2, holdingID.intValue());
        int count = stmt.executeUpdate();
        stmt.close();
    }

    private void updateOrderStatus(Connection conn, Integer orderID, String status) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateOrderStatusSQL);

        stmt.setString(1, status);
        stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        stmt.setInt(3, orderID.intValue());
        int count = stmt.executeUpdate();
        stmt.close();
    }

    private void updateOrderHolding(Connection conn, int orderID, int holdingID) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateOrderHoldingSQL);

        stmt.setInt(1, holdingID);
        stmt.setInt(2, orderID);
        int count = stmt.executeUpdate();
        stmt.close();
    }

    private void updateAccountProfile(Connection conn, String userID, String password, String fullName, String address, String email, String creditcard) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateAccountProfileSQL);

        stmt.setString(1, password);
        stmt.setString(2, fullName);
        stmt.setString(3, address);
        stmt.setString(4, email);
        stmt.setString(5, creditcard);
        stmt.setString(6, userID);

        int count = stmt.executeUpdate();
        stmt.close();
    }

    private void updateQuoteVolume(Connection conn, QuoteDataBean quoteData, double quantity) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateQuoteVolumeSQL);

        stmt.setDouble(1, quantity);
        stmt.setString(2, quoteData.getSymbol());

        int count = stmt.executeUpdate();
        stmt.close();
    }

    public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal changeFactor, double sharesTraded)
        throws Exception {

        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:updateQuotePriceVolume", symbol, changeFactor, new Double(sharesTraded));

        return updateQuotePriceVolumeInt(symbol, changeFactor, sharesTraded, TradeConfig.getPublishQuotePriceChange());
    }

    /**
     * Update a quote's price and volume
     * 
     * @param symbol
     *            The PK of the quote
     * @param changeFactor
     *            the percent to change the old price by (between 50% and 150%)
     * @param sharedTraded
     *            the ammount to add to the current volume
     * @param publishQuotePriceChange
     *            used by the PingJDBCWrite Primitive to ensure no JMS is used, should be true for all normal calls to
     *            this API
     */
    public QuoteDataBean updateQuotePriceVolumeInt(String symbol, BigDecimal changeFactor, double sharesTraded,
        boolean publishQuotePriceChange) throws Exception {

        if (TradeConfig.getUpdateQuotePrices() == false)
            return new QuoteDataBeanImpl();

        QuoteDataBean quoteData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:updateQuotePriceVolume - inSession(" + this.inSession + ")", symbol,
                    changeFactor, new Double(sharesTraded));

            conn = getConn();

            quoteData = getQuoteForUpdate(conn, symbol);
            BigDecimal oldPrice = quoteData.getPrice();
            double newVolume = quoteData.getVolume() + sharesTraded;

            if (oldPrice.equals(TradeConfig.PENNY_STOCK_PRICE)) {
                changeFactor = TradeConfig.PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER;
            } else if (oldPrice.compareTo(TradeConfig.MAXIMUM_STOCK_PRICE) > 0) {
                changeFactor = TradeConfig.MAXIMUM_STOCK_SPLIT_MULTIPLIER;
            }

            BigDecimal newPrice = changeFactor.multiply(oldPrice).setScale(2, BigDecimal.ROUND_HALF_UP);

            updateQuotePriceVolume(conn, quoteData.getSymbol(), newPrice, newVolume);
            quoteData = getQuote(conn, symbol);

            commit(conn);

        } catch (Exception e) {
            Log.error("TradeJDBCDirect:updateQuotePriceVolume -- error updating quote price/volume for symbol:" + symbol);
            rollBack(conn, e);
            throw e;
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }

    private void updateQuotePriceVolume(Connection conn, String symbol, BigDecimal newPrice, double newVolume)
        throws Exception {

        PreparedStatement stmt = getStatement(conn, updateQuotePriceVolumeSQL);

        stmt.setBigDecimal(1, newPrice);
        stmt.setBigDecimal(2, newPrice);
        stmt.setDouble(3, newVolume);
        stmt.setString(4, symbol);

        int count = stmt.executeUpdate();
        stmt.close();
    }

    private void publishQuotePriceChange(QuoteDataBean quoteData, BigDecimal oldPrice, BigDecimal changeFactor,
        double sharesTraded) throws Exception {
        if (!TradeConfig.getPublishQuotePriceChange())
            return;
        Log.error("TradeJDBCDirect:publishQuotePriceChange - is not implemented for this runtime mode");
        throw new UnsupportedOperationException("TradeJDBCDirect:publishQuotePriceChange-  is not implemented for this runtime mode");
    }

    /**
     * @see TradeServices#login(String, String)
     */

    public AccountDataBean login(String userID, String password) throws Exception {

        AccountDataBean accountData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.trace("TradeJDBCDirect:login - inSession(" + this.inSession + ")", userID, password);

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getAccountProfileSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                Log.error("TradeJDBCDirect:login -- failure to find account for" + userID);
                throw new RuntimeException("Cannot find account for" + userID);
            }

            String pw = rs.getString("passwd");
            stmt.close();
            if ((pw == null) || (pw.equals(password) == false)) {
                String error =
                    "TradeJDBCDirect:Login failure for user: " + userID + "\n\tIncorrect password-->" + userID + ":"
                        + password;
                Log.error(error);
                throw new Exception(error);
            }

            stmt = getStatement(conn, loginSQL);
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, userID);

            int rows = stmt.executeUpdate();
            // ?assert rows==1?
            stmt.close();

            stmt = getStatement(conn, getAccountForUserSQL);
            stmt.setString(1, userID);
            rs = stmt.executeQuery();

            accountData = getAccountDataFromResultSet(rs);

            stmt.close();

            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:login -- error logging in user", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountData;

        /*
         * setLastLogin( new Timestamp(System.currentTimeMillis()) ); setLoginCount( getLoginCount() + 1 );
         */
    }

    /**
     * @see TradeServices#logout(String)
     */
    public void logout(String userID) throws Exception {

        Connection conn = null;

        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:logout - inSession(" + this.inSession + ")", userID);
        try {
            conn = getConn();
            PreparedStatement stmt = getStatement(conn, logoutSQL);
            stmt.setString(1, userID);
            stmt.executeUpdate();
            stmt.close();

            commit(conn);
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:logout -- error logging out user", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
    }

    /**
     * @see TradeServices#register(String, String, String, String, String, String, BigDecimal, boolean)
     */

    public AccountDataBean register(String userID, String password, String fullname, String address, String email,
        String creditCard, BigDecimal openBalance) throws Exception {

        AccountDataBean accountData = null;
        Connection conn = null;

        try {
            if (Log.doTrace())
                Log.traceEnter("TradeJDBCDirect:register - inSession(" + this.inSession + ")");

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, createAccountSQL);

            Integer accountID = KeySequenceDirect.getNextID(conn, "account", inSession, getInGlobalTxn());
            BigDecimal balance = openBalance;
            Timestamp creationDate = new Timestamp(System.currentTimeMillis());
            Timestamp lastLogin = creationDate;
            int loginCount = 0;
            int logoutCount = 0;

            stmt.setInt(1, accountID.intValue());
            stmt.setTimestamp(2, creationDate);
            stmt.setBigDecimal(3, openBalance);
            stmt.setBigDecimal(4, balance);
            stmt.setTimestamp(5, lastLogin);
            stmt.setInt(6, loginCount);
            stmt.setInt(7, logoutCount);
            stmt.setString(8, userID);
            stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn, createAccountProfileSQL);
            stmt.setString(1, userID);
            stmt.setString(2, password);
            stmt.setString(3, fullname);
            stmt.setString(4, address);
            stmt.setString(5, email);
            stmt.setString(6, creditCard);
            stmt.executeUpdate();
            stmt.close();

            commit(conn);

            accountData =
                new AccountDataBeanImpl(accountID, loginCount, logoutCount, lastLogin, creationDate, balance, openBalance,
                    userID);
            if (Log.doTrace())
                Log.traceExit("TradeJDBCDirect:register");
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:register -- error registering new user", e);
        } finally {
            releaseConn(conn);
        }
        return accountData;
    }

    private AccountDataBean getAccountDataFromResultSet(ResultSet rs) throws Exception {
        AccountDataBean accountData = null;

        if (!rs.next())
            Log.error("TradeJDBCDirect:getAccountDataFromResultSet -- cannot find account data");

        else
            accountData =
                new AccountDataBeanImpl(new Integer(rs.getInt("accountID")), rs.getInt("loginCount"), rs
                    .getInt("logoutCount"), rs.getTimestamp("lastLogin"), rs.getTimestamp("creationDate"), rs
                    .getBigDecimal("balance"), rs.getBigDecimal("openBalance"), rs.getString("profile_userID"));
        return accountData;
    }

    private AccountProfileDataBean getAccountProfileDataFromResultSet(ResultSet rs) throws Exception {
        AccountProfileDataBean accountProfileData = null;

        if (!rs.next())
            Log.error("TradeJDBCDirect:getAccountProfileDataFromResultSet -- cannot find accountprofile data");
        else
            accountProfileData =
                new AccountProfileDataBeanImpl(rs.getString("userID"), rs.getString("passwd"), rs.getString("fullName"), rs
                    .getString("address"), rs.getString("email"), rs.getString("creditCard"));

        return accountProfileData;
    }

    private HoldingDataBean getHoldingDataFromResultSet(ResultSet rs) throws Exception {
        HoldingDataBean holdingData = null;

        holdingData =
            new HoldingDataBeanImpl(new Integer(rs.getInt("holdingID")), rs.getDouble("quantity"), rs
                .getBigDecimal("purchasePrice"), rs.getTimestamp("purchaseDate"), rs.getString("quote_symbol"));
        return holdingData;
    }

    private QuoteDataBean getQuoteDataFromResultSet(ResultSet rs) throws Exception {
        QuoteDataBean quoteData = null;

        quoteData =
            new QuoteDataBeanImpl(rs.getString("symbol"), rs.getString("companyName"), rs.getDouble("volume"), rs
                .getBigDecimal("price"), rs.getBigDecimal("open1"), rs.getBigDecimal("low"), rs.getBigDecimal("high"),
                rs.getDouble("change1"));
        return quoteData;
    }

    private OrderDataBean getOrderDataFromResultSet(ResultSet rs) throws Exception {
        OrderDataBean orderData = null;

        orderData =
            new OrderDataBeanImpl(new Integer(rs.getInt("orderID")), rs.getString("orderType"),
                rs.getString("orderStatus"), rs.getTimestamp("openDate"), rs.getTimestamp("completionDate"), rs
                    .getDouble("quantity"), rs.getBigDecimal("price"), rs.getBigDecimal("orderFee"), rs
                    .getString("quote_symbol"));
        return orderData;
    }


    private void releaseConn(Connection conn) throws Exception {
        try {
            if (conn != null) {
                conn.close();
                if (Log.doTrace()) {
                    synchronized (lock) {
                        connCount--;
                    }
                    Log.trace("TradeJDBCDirect:releaseConn -- connection closed, connCount=" + connCount);
                }
            }
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:releaseConnection -- failed to close connection", e);
        }
    }

    /*
     * Lookup the TradeData DataSource
     */
//  private void getDataSource() throws Exception {
//      if (dataSource == null) {
//          context = new InitialContext();
//          dataSource = (DataSource) context.lookup(dsName);
//      }
//  }

    /*
     * Allocate a new connection to the datasource
     */
    private Connection getConn() throws Exception {

        Connection conn = null;
//        getDataSource();
        conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        if (Log.doTrace()) {
            synchronized (lock) {
                connCount++;
            }
            Log.trace("TradeJDBCDirect:getConn -- new connection allocated, IsolationLevel="
                + conn.getTransactionIsolation() + " connectionCount = " + connCount);
        }

        return conn;
    }

    /*
     * Commit the provided connection if not under Global Transaction scope - conn.commit() is not allowed in a global
     * transaction. the txn manager will perform the commit
     */
    private void commit(Connection conn) throws Exception {
        if (!inSession) {
            if ((getInGlobalTxn() == false) && (conn != null))
                conn.commit();
        }
    }

    /*
     * Rollback the statement for the given connection
     */
    private void rollBack(Connection conn, Exception e) throws Exception {
        if (!inSession) {
            Log.log("TradeJDBCDirect:rollBack -- rolling back conn due to previously caught exception -- inGlobalTxn="
                + getInGlobalTxn());
            if ((getInGlobalTxn() == false) && (conn != null))
                conn.rollback();
            else
                throw e; // Throw the exception
            // so the Global txn manager will rollBack
        }
    }

    /*
     * Allocate a new prepared statment for this connection
     */
    private PreparedStatement getStatement(Connection conn, String sql) throws Exception {
        return conn.prepareStatement(sql);
    }

    private PreparedStatement getStatement(Connection conn, String sql, int type, int concurrency) throws Exception {
        return conn.prepareStatement(sql, type, concurrency);
    }

    private static final String createQuoteSQL =
        "insert into quoteejb " + "( symbol, companyName, volume, price, open1, low, high, change1 ) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  )";

    private static final String createAccountSQL =
        "insert into accountejb "
            + "( accountid, creationDate, openBalance, balance, lastLogin, loginCount, logoutCount, profile_userid) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  )";

    private static final String createAccountProfileSQL =
        "insert into accountprofileejb " + "( userid, passwd, fullname, address, email, creditcard ) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  )";

    private static final String createHoldingSQL =
        "insert into holdingejb "
            + "( holdingid, purchaseDate, purchasePrice, quantity, quote_symbol, account_accountid ) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ? )";

    private static final String createOrderSQL =
        "insert into orderejb "
            + "( orderid, ordertype, orderstatus, opendate, quantity, price, orderfee, account_accountid,  holding_holdingid, quote_symbol) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  , ? , ? , ?)";

    private static final String removeHoldingSQL = "delete from holdingejb where holdingid = ?";

    private static final String removeHoldingFromOrderSQL =
        "update orderejb set holding_holdingid=null where holding_holdingid = ?";

    private final static String updateAccountProfileSQL =
        "update accountprofileejb set " + "passwd = ?, fullname = ?, address = ?, email = ?, creditcard = ? "
            + "where userid = (select profile_userid from accountejb a " + "where a.profile_userid=?)";

    private final static String loginSQL =
        "update accountejb set lastLogin=?, logincount=logincount+1 " + "where profile_userid=?";

    private static final String logoutSQL =
        "update accountejb set logoutcount=logoutcount+1 " + "where profile_userid=?";

    private static final String getAccountSQL = "select * from accountejb a where a.accountid = ?";

    private static final String getAccountForUpdateSQL = "select * from accountejb a where a.accountid = ? for update";

    private final static String getAccountProfileSQL =
        "select * from accountprofileejb ap where ap.userid = "
            + "(select profile_userid from accountejb a where a.profile_userid=?)";

    private final static String getAccountProfileForAccountSQL =
        "select * from accountprofileejb ap where ap.userid = "
            + "(select profile_userid from accountejb a where a.accountid=?)";

    private static final String getAccountForUserSQL =
        "select * from accountejb a where a.profile_userid = "
            + "( select userid from accountprofileejb ap where ap.userid = ?)";

    private static final String getAccountForUserForUpdateSQL =
        "select * from accountejb a where a.profile_userid = "
            + "( select userid from accountprofileejb ap where ap.userid = ?) for update";

    private static final String getHoldingSQL = "select * from holdingejb h where h.holdingid = ?";

    private static final String getHoldingsForUserSQL =
        "select * from holdingejb h where h.account_accountid = "
            + "(select a.accountid from accountejb a where a.profile_userid = ?)";

    private static final String getOrderSQL = "select * from orderejb o where o.orderid = ?";

    private static final String getOrdersByUserSQL =
        "select * from orderejb o where o.account_accountid = "
            + "(select a.accountid from accountejb a where a.profile_userid = ?)";

    private static final String getClosedOrdersSQL =
        "select * from orderejb o " + "where o.orderstatus = 'closed' AND o.account_accountid = "
            + "(select a.accountid from accountejb a where a.profile_userid = ?)";

    private static final String getQuoteSQL = "select * from quoteejb q where q.symbol=?";

    private static final String getAllQuotesSQL = "select * from quoteejb q";

    private static final String getQuoteForUpdateSQL = "select * from quoteejb q where q.symbol=? For Update";

    private static final String getTSIAQuotesOrderByChangeSQL =
        "select * from quoteejb q " + "where q.symbol like 's:1__' order by q.change1";

    private static final String getTSIASQL =
        "select SUM(price)/count(*) as TSIA from quoteejb q " + "where q.symbol like 's:1__'";

    private static final String getOpenTSIASQL =
        "select SUM(open1)/count(*) as openTSIA from quoteejb q " + "where q.symbol like 's:1__'";

    private static final String getTSIATotalVolumeSQL =
        "select SUM(volume) as totalVolume from quoteejb q " + "where q.symbol like 's:1__'";

    private static final String creditAccountBalanceSQL =
        "update accountejb set " + "balance = balance + ? " + "where accountid = ?";

    private static final String updateOrderStatusSQL =
        "update orderejb set " + "orderstatus = ?, completiondate = ? " + "where orderid = ?";

    private static final String updateOrderHoldingSQL =
        "update orderejb set " + "holding_holdingID = ? " + "where orderid = ?";

    private static final String updateQuoteVolumeSQL =
        "update quoteejb set " + "volume = volume + ? " + "where symbol = ?";

    private static final String updateQuotePriceVolumeSQL =
        "update quoteejb set " + "price = ?, change1 = ? - open1, volume = ? " + "where symbol = ?";

    public void init() {
        if (initialized)
            return;
        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:init -- *** initializing");

        if (Log.doTrace())
            Log.trace("TradeJDBCDirect:init -- +++ initialized");

        initialized = true;
    }

    public void destroy() {
        try {
            if (!initialized)
                return;
            Log.trace("TradeJDBCDirect:destroy");
        } catch (Exception e) {
            Log.error("TradeJDBCDirect:destroy", e);
        }
    }

    /**
     * Gets the inGlobalTxn
     * 
     * @return Returns a boolean
     */
    private boolean getInGlobalTxn() {
        return inGlobalTxn;
    }

    /**
     * Sets the inGlobalTxn
     * 
     * @param inGlobalTxn
     *            The inGlobalTxn to set
     */
    private void setInGlobalTxn(boolean inGlobalTxn) {
        this.inGlobalTxn = inGlobalTxn;
    }

    /**
     * Get mode - returns the persistence mode (TradeConfig.JDBC)
     * 
     * @return int mode
     */
    public int getMode() {
        return TradeConfig.JDBC;
    }

}

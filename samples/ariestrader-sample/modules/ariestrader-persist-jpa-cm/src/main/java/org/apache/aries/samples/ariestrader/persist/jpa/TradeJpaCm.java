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
package org.apache.aries.samples.ariestrader.persist.jpa;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.aries.samples.ariestrader.api.TradeServices;
import org.apache.aries.samples.ariestrader.entities.AccountDataBeanImpl;
import org.apache.aries.samples.ariestrader.entities.AccountProfileDataBeanImpl;
import org.apache.aries.samples.ariestrader.entities.HoldingDataBeanImpl;
import org.apache.aries.samples.ariestrader.entities.OrderDataBeanImpl;
import org.apache.aries.samples.ariestrader.entities.QuoteDataBeanImpl;
import org.apache.aries.samples.ariestrader.api.persistence.AccountDataBean;
import org.apache.aries.samples.ariestrader.api.persistence.AccountProfileDataBean;
import org.apache.aries.samples.ariestrader.api.persistence.HoldingDataBean;
import org.apache.aries.samples.ariestrader.api.persistence.MarketSummaryDataBean;
import org.apache.aries.samples.ariestrader.api.persistence.OrderDataBean;
import org.apache.aries.samples.ariestrader.api.persistence.QuoteDataBean;
import org.apache.aries.samples.ariestrader.util.FinancialUtils;
import org.apache.aries.samples.ariestrader.util.Log;
import org.apache.aries.samples.ariestrader.util.TradeConfig;

/**
 * TradeJpaCm uses JPA via Container Managed (CM) Entity
 * Managers to implement the business methods of the Trade
 * online broker application. These business methods represent
 * the features and operations that can be performed by
 * customers of the brokerage such as login, logout, get a stock
 * quote, buy or sell a stock, etc. and are specified in the
 * {@link org.apache.aries.samples.ariestrader.TradeServices}
 * interface
 * 
 * @see org.apache.aries.samples.ariestrader.TradeServices
 * 
 */

public class TradeJpaCm implements TradeServices {

    private EntityManager entityManager;

    private static BigDecimal ZERO = new BigDecimal(0.0);

    private static boolean initialized = false;

//    @PersistenceContext(unitName="ariestrader-cm")
    public void setEntityManager (EntityManager em) { 
        entityManager = em;
    }

    /**
     * Zero arg constructor for TradeJpaCm
     */
    public TradeJpaCm() {
    }

    public void init() {
        if (initialized)
            return;
        if (Log.doTrace())
            Log.trace("TradeJpaCm:init -- *** initializing");

        if (Log.doTrace())
            Log.trace("TradeJpaCm:init -- +++ initialized");

        initialized = true;
    }

    public void destroy() {
        try {
            if (!initialized)
                return;
            Log.trace("TradeJpaCm:destroy");
        }
        catch (Exception e) {
            Log.error("TradeJpaCm:destroy", e);
        }

    }

    public MarketSummaryDataBean getMarketSummary() {
        MarketSummaryDataBean marketSummaryData;

        try {
            if (Log.doTrace())
                Log.trace("TradeJpaCm:getMarketSummary -- getting market summary");

            // Find Trade Stock Index Quotes (Top 100 quotes)
            // ordered by their change in value
            Collection<QuoteDataBean> quotes;

            Query query = entityManager.createNamedQuery("quoteejb.quotesByChange");
            quotes = query.getResultList();

            QuoteDataBean[] quoteArray = (QuoteDataBean[]) quotes.toArray(new QuoteDataBean[quotes.size()]);
            ArrayList<QuoteDataBean> topGainers = new ArrayList<QuoteDataBean>(
                                                                              5);
            ArrayList<QuoteDataBean> topLosers = new ArrayList<QuoteDataBean>(5);
            BigDecimal TSIA = FinancialUtils.ZERO;
            BigDecimal openTSIA = FinancialUtils.ZERO;
            double totalVolume = 0.0;

            if (quoteArray.length > 5) {
                for (int i = 0; i < 5; i++)
                    topGainers.add(quoteArray[i]);
                for (int i = quoteArray.length - 1; i >= quoteArray.length - 5; i--)
                    topLosers.add(quoteArray[i]);

                for (QuoteDataBean quote : quoteArray) {
                    BigDecimal price = quote.getPrice();
                    BigDecimal open = quote.getOpen();
                    double volume = quote.getVolume();
                    TSIA = TSIA.add(price);
                    openTSIA = openTSIA.add(open);
                    totalVolume += volume;
                }
                TSIA = TSIA.divide(new BigDecimal(quoteArray.length),
                                   FinancialUtils.ROUND);
                openTSIA = openTSIA.divide(new BigDecimal(quoteArray.length),
                                           FinancialUtils.ROUND);
            }

            marketSummaryData = new MarketSummaryDataBean(TSIA, openTSIA,
                                                          totalVolume, topGainers, topLosers);
        }
        catch (Exception e) {
            Log.error("TradeJpaCm:getMarketSummary", e);
            throw new RuntimeException("TradeJpaCm:getMarketSummary -- error ", e);
        }

        return marketSummaryData;
    }

    public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) throws Exception {
        OrderDataBean order = null;
        BigDecimal total;

        try {
            if (Log.doTrace())
                Log.trace("TradeJpaCm:buy", userID, symbol, quantity, orderProcessingMode);

            AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
            AccountDataBean account = profile.getAccount();

            QuoteDataBeanImpl quote = entityManager.find(QuoteDataBeanImpl.class, symbol);

            HoldingDataBeanImpl holding = null; // The holding will be created by this buy order

            order = createOrder( account, (QuoteDataBean) quote, (HoldingDataBean) holding, "buy", quantity);

            // order = createOrder(account, quote, holding, "buy", quantity);
            // UPDATE - account should be credited during completeOrder

            BigDecimal price = quote.getPrice();
            BigDecimal orderFee = order.getOrderFee();
            BigDecimal balance = account.getBalance();
            total = (new BigDecimal(quantity).multiply(price)).add(orderFee);
            account.setBalance(balance.subtract(total));

            if (orderProcessingMode == TradeConfig.SYNCH)
                completeOrder(order.getOrderID(), false);
            else if (orderProcessingMode == TradeConfig.ASYNCH_2PHASE)
                queueOrder(order.getOrderID(), true);
        }
        catch (Exception e) {
            Log.error("TradeJpaCm:buy(" + userID + "," + symbol + "," + quantity + ") --> failed", e);
            /* On exception - cancel the order */
            // TODO figure out how to do this with JPA
            if (order != null)
                order.cancel();

            throw new RuntimeException(e);
        }

        // after the purchase or sell of a stock, update the stocks volume and
        // price
        updateQuotePriceVolume(symbol, TradeConfig.getRandomPriceChangeFactor(), quantity);

        return order;
    }

    public OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) throws Exception {

        OrderDataBean order = null;
        BigDecimal total;
        try {
            if (Log.doTrace())
                Log.trace("TradeJpaCm:sell", userID, holdingID, orderProcessingMode);

            AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);

            AccountDataBean account = profile.getAccount();
            HoldingDataBeanImpl holding = entityManager.find(HoldingDataBeanImpl.class, holdingID);

            if (holding == null) {
                Log.error("TradeJpaCm:sell User " + userID
                          + " attempted to sell holding " + holdingID
                          + " which has already been sold");

                OrderDataBean orderData = new OrderDataBeanImpl();
                orderData.setOrderStatus("cancelled");

                entityManager.persist(orderData);

                return orderData;
            }

            QuoteDataBean quote = holding.getQuote();
            double quantity = holding.getQuantity();

            order = createOrder(account, quote, holding, "sell", quantity);
            // UPDATE the holding purchase data to signify this holding is
            // "inflight" to be sold
            // -- could add a new holdingStatus attribute to holdingEJB
            holding.setPurchaseDate(new java.sql.Timestamp(0));

            // UPDATE - account should be credited during completeOrder
            BigDecimal price = quote.getPrice();
            BigDecimal orderFee = order.getOrderFee();
            BigDecimal balance = account.getBalance();
            total = (new BigDecimal(quantity).multiply(price)).subtract(orderFee);

            account.setBalance(balance.add(total));

            if (orderProcessingMode == TradeConfig.SYNCH)
                completeOrder(order.getOrderID(), false);
            else if (orderProcessingMode == TradeConfig.ASYNCH_2PHASE)
                queueOrder(order.getOrderID(), true);

        }
        catch (Exception e) {
            Log.error("TradeJpaCm:sell(" + userID + "," + holdingID + ") --> failed", e);
            // TODO figure out JPA cancel
            if (order != null)
                order.cancel();

            throw new RuntimeException("TradeJpaCm:sell(" + userID + "," + holdingID + ")", e);
        }

        if (!(order.getOrderStatus().equalsIgnoreCase("cancelled")))
            //after the purchase or sell of a stock, update the stocks volume and price
            updateQuotePriceVolume(order.getSymbol(), TradeConfig.getRandomPriceChangeFactor(), order.getQuantity());

        return order;
    }

    public void queueOrder(Integer orderID, boolean twoPhase) {
        Log
        .error("TradeJpaCm:queueOrder() not implemented for this runtime mode");
        throw new UnsupportedOperationException(
                                               "TradeJpaCm:queueOrder() not implemented for this runtime mode");
    }

    public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception {

        OrderDataBeanImpl order = null;

        if (Log.doTrace())
            Log.trace("TradeJpaCm:completeOrder", orderID + " twoPhase=" + twoPhase);

        order = entityManager.find(OrderDataBeanImpl.class, orderID);
        order.getQuote();

        if (order == null) {
            Log.error("TradeJpaCm:completeOrder -- Unable to find Order " + orderID + " FBPK returned " + order);
            return null;
        }

        if (order.isCompleted()) {
            throw new RuntimeException("Error: attempt to complete Order that is already completed\n" + order);
        }

        AccountDataBean account = order.getAccount();
        QuoteDataBean quote = order.getQuote();
        HoldingDataBean holding = order.getHolding();
        BigDecimal price = order.getPrice();
        double quantity = order.getQuantity();

        String userID = account.getProfile().getUserID();

        if (Log.doTrace())
            Log.trace("TradeJpaCm:completeOrder--> Completing Order "
                      + order.getOrderID() + "\n\t Order info: " + order
                      + "\n\t Account info: " + account + "\n\t Quote info: "
                      + quote + "\n\t Holding info: " + holding);

        HoldingDataBean newHolding = null;
        if (order.isBuy()) {
            /*
             * Complete a Buy operation - create a new Holding for the Account -
             * deduct the Order cost from the Account balance
             */

            newHolding = createHolding(account, quote, quantity, price);
        }

        try {

            if (newHolding != null) {
                order.setHolding(newHolding);
            }

            if (order.isSell()) {
                /*
                 * Complete a Sell operation - remove the Holding from the Account -
                 * deposit the Order proceeds to the Account balance
                 */
                if (holding == null) {
                    Log.error("TradeJpaCm:completeOrder -- Unable to sell order " + order.getOrderID() + " holding already sold");
                    order.cancel();
                    return order;
                }
                else {
                    entityManager.remove(holding);
                    order.setHolding(null);
                }
            }

            order.setOrderStatus("closed");

            order.setCompletionDate(new java.sql.Timestamp(System.currentTimeMillis()));

            if (Log.doTrace())
                Log.trace("TradeJpaCm:completeOrder--> Completed Order "
                          + order.getOrderID() + "\n\t Order info: " + order
                          + "\n\t Account info: " + account + "\n\t Quote info: "
                          + quote + "\n\t Holding info: " + holding);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return order;
    }

    public void cancelOrder(Integer orderID, boolean twoPhase) throws Exception {

        if (Log.doTrace())
            Log.trace("TradeJpaCm:cancelOrder", orderID + " twoPhase=" + twoPhase);

        OrderDataBeanImpl order = entityManager.find(OrderDataBeanImpl.class, orderID);
        order.cancel();
    }

    public void orderCompleted(String userID, Integer orderID) {
        if (Log.doActionTrace())
            Log.trace("TradeAction:orderCompleted", userID, orderID);
        if (Log.doTrace())
            Log.trace("OrderCompleted", userID, orderID);
    }

    public Collection<OrderDataBean> getOrders(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:getOrders", userID);
        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        AccountDataBean account = profile.getAccount();
        return account.getOrders();
    }

    public Collection<OrderDataBean> getClosedOrders(String userID) throws Exception {

        if (Log.doTrace())
            Log.trace("TradeJpaCm:getClosedOrders", userID);

        try {

            // Get the primary keys for all the closed Orders for this
            // account.
            Query query = entityManager.createNamedQuery("orderejb.closedOrders");
            query.setParameter("userID", userID);

            Collection results = query.getResultList();
            Iterator itr = results.iterator();
            // Spin through the orders to populate the lazy quote fields
            while (itr.hasNext()) {
                OrderDataBeanImpl thisOrder = (OrderDataBeanImpl) itr.next();
                thisOrder.getQuote();
            }

            if (TradeConfig.jpaLayer == TradeConfig.OPENJPA) {
                Query updateStatus = entityManager.createNamedQuery("orderejb.completeClosedOrders");
                updateStatus.setParameter("userID", userID);
                updateStatus.executeUpdate();
            }
            else if (TradeConfig.jpaLayer == TradeConfig.HIBERNATE) {
                /*
                 * Add logic to do update orders operation, because JBoss5'
                 * Hibernate 3.3.1GA DB2Dialect and MySQL5Dialect do not work
                 * with annotated query "orderejb.completeClosedOrders" defined
                 * in OrderDatabean
                 */
                Query findaccountid = entityManager.createNativeQuery(
                                                        "select "
                                                        + "a.ACCOUNTID, "
                                                        + "a.LOGINCOUNT, "
                                                        + "a.LOGOUTCOUNT, "
                                                        + "a.LASTLOGIN, "
                                                        + "a.CREATIONDATE, "
                                                        + "a.BALANCE, "
                                                        + "a.OPENBALANCE, "
                                                        + "a.PROFILE_USERID "
                                                        + "from accountejb a where a.profile_userid = ?",
                                                        org.apache.aries.samples.ariestrader.entities.AccountDataBeanImpl.class);
                findaccountid.setParameter(1, userID);
                AccountDataBeanImpl account = (AccountDataBeanImpl) findaccountid.getSingleResult();
                Integer accountid = account.getAccountID();
                Query updateStatus = entityManager.createNativeQuery("UPDATE orderejb o SET o.orderStatus = 'completed' WHERE "
                                                                     + "o.orderStatus = 'closed' AND o.ACCOUNT_ACCOUNTID  = ?");
                updateStatus.setParameter(1, accountid.intValue());
                updateStatus.executeUpdate();
            }
            return results;
        }
        catch (Exception e) {
            Log.error("TradeJpaCm.getClosedOrders", e);
            throw new RuntimeException(
                                      "TradeJpaCm.getClosedOrders - error", e);

        }

    }

    public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws Exception {

        try {
            QuoteDataBeanImpl quote = new QuoteDataBeanImpl(symbol, companyName, 0, price, price, price, price, 0);
            entityManager.persist(quote);

            if (Log.doTrace())
                Log.trace("TradeJpaCm:createQuote-->" + quote);

            return quote;
        }
        catch (Exception e) {
            Log.error("TradeJpaCm:createQuote -- exception creating Quote", e);
            throw new RuntimeException(e);
        }
    }

    public QuoteDataBean getQuote(String symbol) {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:getQuote", symbol);

        QuoteDataBeanImpl qdb = entityManager.find(QuoteDataBeanImpl.class, symbol);

        return qdb;
    }

    public Collection<QuoteDataBean> getAllQuotes() {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:getAllQuotes");

        Query query = entityManager.createNamedQuery("quoteejb.allQuotes");

        return query.getResultList();
    }

    public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal changeFactor, double sharesTraded) throws Exception {
        if (!TradeConfig.getUpdateQuotePrices()) {
            return new QuoteDataBeanImpl();
        }

        if (Log.doTrace())
            Log.trace("TradeJpaCm:updateQuote", symbol, changeFactor);

        /*
         * Add logic to determine JPA layer, because JBoss5' Hibernate 3.3.1GA
         * DB2Dialect and MySQL5Dialect do not work with annotated query
         * "quoteejb.quoteForUpdate" defined in QuoteDataBeanImpl
         */
        QuoteDataBeanImpl quote = null;
        if (TradeConfig.jpaLayer == TradeConfig.HIBERNATE) {
            quote = entityManager.find(QuoteDataBeanImpl.class, symbol);
        } else if (TradeConfig.jpaLayer == TradeConfig.OPENJPA) {
  
            Query q = entityManager.createNamedQuery("quoteejb.quoteForUpdate");
            q.setParameter(1, symbol);
  
            quote = (QuoteDataBeanImpl) q.getSingleResult();
        }

        BigDecimal oldPrice = quote.getPrice();

        if (quote.getPrice().equals(TradeConfig.PENNY_STOCK_PRICE)) {
            changeFactor = TradeConfig.PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER;
        }

        BigDecimal newPrice = changeFactor.multiply(oldPrice).setScale(2, BigDecimal.ROUND_HALF_UP);

        quote.setPrice(newPrice);
        quote.setVolume(quote.getVolume() + sharesTraded);
        quote.setChange((newPrice.subtract(quote.getOpen()).doubleValue()));

        entityManager.merge(quote);

        this.publishQuotePriceChange(quote, oldPrice, changeFactor, sharesTraded);

        return quote;
    }

    public Collection<HoldingDataBean> getHoldings(String userID) throws Exception {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:getHoldings", userID);

        Collection<HoldingDataBean> holdings = null;

        Query query = entityManager.createNamedQuery("holdingejb.holdingsByUserID");
        query.setParameter("userID", userID);

        holdings = query.getResultList();
        /*
         * Inflate the lazy data memebers
         */
        Iterator itr = holdings.iterator();
        while (itr.hasNext()) {
            ((HoldingDataBean) itr.next()).getQuote();
        }

        return holdings;
    }

    public HoldingDataBean getHolding(Integer holdingID) {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:getHolding", holdingID);
        return entityManager.find(HoldingDataBeanImpl.class, holdingID);
    }

    public AccountDataBean getAccountData(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:getAccountData", userID);

        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        /*
         * Inflate the lazy data memebers
         */
        AccountDataBean account = profile.getAccount();
        account.getProfile();

        // Added to populate transient field for account
        account.setProfileID(profile.getUserID());

        return account;
    }

    public AccountProfileDataBean getAccountProfileData(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:getProfileData", userID);

        AccountProfileDataBeanImpl apb = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        return apb;
    }

    public AccountProfileDataBean updateAccountProfile( String userID, 
                                                        String password, 
                                                        String fullName, 
                                                        String address, 
                                                        String email, 
                                                        String creditcard) throws Exception {


        if (Log.doTrace())
            Log.trace("TradeJpaCm:updateAccountProfileData", userID);
        /*
         * // Retrieve the previous account profile in order to get account
         * data... hook it into new object AccountProfileDataBean temp =
         * entityManager.find(AccountProfileDataBean.class,
         * profileData.getUserID()); // In order for the object to merge
         * correctly, the account has to be hooked into the temp object... // -
         * may need to reverse this and obtain the full object first
         * 
         * profileData.setAccount(temp.getAccount());
         * 
         * //TODO this might not be correct temp =
         * entityManager.merge(profileData); //System.out.println(temp);
         */

        AccountProfileDataBeanImpl temp = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        temp.setAddress(address);
        temp.setPassword(password);
        temp.setFullName(fullName);
        temp.setCreditCard(creditcard);
        temp.setEmail(email);
        entityManager.merge(temp);

        return temp;
    }

    public AccountDataBean login(String userID, String password)
    throws Exception {

        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);

        if (profile == null) {
            throw new RuntimeException("No such user: " + userID);
        }
        entityManager.merge(profile);

        AccountDataBean account = profile.getAccount();

        if (Log.doTrace())
            Log.trace("TradeJpaCm:login", userID, password);

        account.login(password);

        if (Log.doTrace())
            Log.trace("TradeJpaCm:login(" + userID + "," + password + ") success" + account);
        return account;
    }

    public void logout(String userID) throws Exception {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:logout", userID);

        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        AccountDataBean account = profile.getAccount();

        account.logout();

        if (Log.doTrace())
            Log.trace("TradeJpaCm:logout(" + userID + ") success");
    }

    public AccountDataBean register(String userID, 
                                    String password, 
                                    String fullname, 
                                    String address, 
                                    String email, 
                                    String creditcard,
                                    BigDecimal openBalance) throws Exception {
        AccountDataBeanImpl account = null;
        AccountProfileDataBeanImpl profile = null;

        if (Log.doTrace())
            Log.trace("TradeJpaCm:register", userID, password, fullname, address, email, creditcard, openBalance);

        // Check to see if a profile with the desired userID already exists

        profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);

        if (profile != null) {
            Log.error("Failed to register new Account - AccountProfile with userID(" + userID + ") already exists");
            return null;
        }
        else {
            profile = new AccountProfileDataBeanImpl(userID, password, fullname,
                                                 address, email, creditcard);
            account = new AccountDataBeanImpl(0, 0, null, new Timestamp(System.currentTimeMillis()), openBalance, openBalance, userID);
            profile.setAccount((AccountDataBean)account);
            account.setProfile((AccountProfileDataBean)profile);
            entityManager.persist(profile);
            entityManager.persist(account);
            // Uncomment this line to verify that datasources has been enlisted.  After rebuild attempt to register a user with
            // a user id "fail".  After the exception is thrown the database should not contain the user "fail" even though 
            // the profile and account have already been persisted.
            // if (userID.equals("fail")) throw new RuntimeException("**** enlisted datasource validated via rollback test ****");
        }

        return account;
    }

    /*
     * NO LONGER USE
     */

    private void publishQuotePriceChange(QuoteDataBean quote,
                                         BigDecimal oldPrice, BigDecimal changeFactor, double sharesTraded) {
        if (!TradeConfig.getPublishQuotePriceChange())
            return;
        Log.error("TradeJpaCm:publishQuotePriceChange - is not implemented for this runtime mode");
        throw new UnsupportedOperationException("TradeJpaCm:publishQuotePriceChange - is not implemented for this runtime mode");
    }

    private OrderDataBean createOrder(AccountDataBean account,
                                      QuoteDataBean quote, HoldingDataBean holding, String orderType,
                                      double quantity) {
        OrderDataBeanImpl order;
        if (Log.doTrace())
            Log.trace("TradeJpaCm:createOrder(orderID=" + " account="
                      + ((account == null) ? null : account.getAccountID())
                      + " quote=" + ((quote == null) ? null : quote.getSymbol())
                      + " orderType=" + orderType + " quantity=" + quantity);
        try {
            order = new OrderDataBeanImpl(orderType, 
                                      "open", 
                                      new Timestamp(System.currentTimeMillis()), 
                                      null, 
                                      quantity, 
                                      quote.getPrice().setScale(FinancialUtils.SCALE, FinancialUtils.ROUND),
                                      TradeConfig.getOrderFee(orderType), 
                                      account, 
                                      quote, 
                                      holding);
                entityManager.persist(order);
        }
        catch (Exception e) {
            Log.error("TradeJpaCm:createOrder -- failed to create Order", e);
            throw new RuntimeException("TradeJpaCm:createOrder -- failed to create Order", e);
        }
        return order;
    }

    private HoldingDataBean createHolding(AccountDataBean account,
                                          QuoteDataBean quote, 
                                          double quantity, 
                                          BigDecimal purchasePrice) throws Exception {
        HoldingDataBeanImpl newHolding = new HoldingDataBeanImpl(quantity,
                                                         purchasePrice, new Timestamp(System.currentTimeMillis()),
                                                         account, quote);
        entityManager.persist(newHolding);
        return newHolding;
    }

    public double investmentReturn(double investment, double NetValue)
    throws Exception {
        if (Log.doTrace())
            Log.trace("TradeJpaCm:investmentReturn");

        double diff = NetValue - investment;
        double ir = diff / investment;
        return ir;
    }

    public QuoteDataBean pingTwoPhase(String symbol) throws Exception {
        Log.error("TradeJpaCm:pingTwoPhase - is not implemented for this runtime mode");
        throw new UnsupportedOperationException("TradeJpaCm:pingTwoPhase - is not implemented for this runtime mode");
    }

    class quotePriceComparator implements java.util.Comparator {
        public int compare(Object quote1, Object quote2) {
            double change1 = ((QuoteDataBean) quote1).getChange();
            double change2 = ((QuoteDataBean) quote2).getChange();
            return new Double(change2).compareTo(change1);
        }
    }

    /**
     * Get mode - returns the persistence mode (TradeConfig.JPA)
     * 
     * @return int mode
     */
    public int getMode() {
        return TradeConfig.JPA_CM;
    }

}

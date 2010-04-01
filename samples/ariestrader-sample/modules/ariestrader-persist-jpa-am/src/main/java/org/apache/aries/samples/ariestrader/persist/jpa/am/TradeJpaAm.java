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
package org.apache.aries.samples.ariestrader.persist.jpa.am;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
 * TradeJpaAm uses JPA via a Application Managed (AM)
 * entity managers to implement the business methods of the
 * Trade online broker application. These business methods
 * represent the features and operations that can be performed
 * by customers of the brokerage such as login, logout, get a
 * stock quote, buy or sell a stock, etc. and are specified in
 * the {@link
 * org.apache.aries.samples.ariestrader.TradeServices}
 * interface
 * 
 * @see org.apache.aries.samples.ariestrader.TradeServices
 * 
 */

public class TradeJpaAm implements TradeServices {

//    @PersistenceUnit(unitName="ariestrader-am")
    private static EntityManagerFactory emf;

    private static boolean initialized = false;

    /**
     * Zero arg constructor for TradeJpaAm
     */
    public TradeJpaAm() {
    }

    public void setEmf (EntityManagerFactory emf) { 
        this.emf = emf;
    }

    public void init() {
        if (initialized)
            return;
        if (Log.doTrace())
            Log.trace("TradeJpaAm:init -- *** initializing");

        if (Log.doTrace())
            Log.trace("TradeJpaAm:init -- +++ initialized");

        initialized = true;
    }

    public void destroy() {
        try {
            if (!initialized)
                return;
            Log.trace("TradeJpaAm:destroy");
        }
        catch (Exception e) {
            Log.error("TradeJpaAm:destroy", e);
        }

    }

    public MarketSummaryDataBean getMarketSummary() {
        MarketSummaryDataBean marketSummaryData;

        /*
         * Creating entiManager
         */
        EntityManager entityManager = emf.createEntityManager();

        try {
            if (Log.doTrace())
                Log.trace("TradeJpaAm:getMarketSummary -- getting market summary");

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
            Log.error("TradeJpaAm:getMarketSummary", e);
            throw new RuntimeException("TradeJpaAm:getMarketSummary -- error ", e);
        }
        /*
         * closing entitymanager
         */
        entityManager.close();

        return marketSummaryData;
    }

    public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) {
        OrderDataBean order = null;
        BigDecimal total;
        /*
         * creating entitymanager
         */
        EntityManager entityManager = emf.createEntityManager();

        try {
            if (Log.doTrace())
                Log.trace("TradeJpaAm:buy", userID, symbol, quantity, orderProcessingMode);

            entityManager.getTransaction().begin();

            AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
            AccountDataBean account = profile.getAccount();

            QuoteDataBeanImpl quote = entityManager.find(QuoteDataBeanImpl.class, symbol);

            HoldingDataBeanImpl holding = null; // The holding will be created by this buy order

            order = createOrder( account, (QuoteDataBean) quote, (HoldingDataBean) holding, "buy", quantity, entityManager);

            // order = createOrder(account, quote, holding, "buy", quantity);
            // UPDATE - account should be credited during completeOrder

            BigDecimal price = quote.getPrice();
            BigDecimal orderFee = order.getOrderFee();
            BigDecimal balance = account.getBalance();
            total = (new BigDecimal(quantity).multiply(price)).add(orderFee);
            account.setBalance(balance.subtract(total));

            // commit the transaction before calling completeOrder
            entityManager.getTransaction().commit();

            if (orderProcessingMode == TradeConfig.SYNCH)
                completeOrder(order.getOrderID(), false);
            else if (orderProcessingMode == TradeConfig.ASYNCH_2PHASE)
                queueOrder(order.getOrderID(), true);
        }
        catch (Exception e) {
            Log.error("TradeJpaAm:buy(" + userID + "," + symbol + "," + quantity + ") --> failed", e);
            /* On exception - cancel the order */
            // TODO figure out how to do this with JPA
            if (order != null)
                order.cancel();

            entityManager.getTransaction().rollback();

            // throw new EJBException(e);
            throw new RuntimeException(e);
        }
        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }

        // after the purchase or sale of a stock, update the stocks volume and
        // price
        updateQuotePriceVolume(symbol, TradeConfig.getRandomPriceChangeFactor(), quantity);

        return order;
    }

    public OrderDataBean sell(String userID, Integer holdingID,
                              int orderProcessingMode) {
        EntityManager entityManager = emf.createEntityManager();

        OrderDataBean order = null;
        BigDecimal total;
        try {
            entityManager.getTransaction().begin();
            if (Log.doTrace())
                Log.trace("TradeJpaAm:sell", userID, holdingID, orderProcessingMode);

            AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);

            AccountDataBean account = profile.getAccount();
            HoldingDataBeanImpl holding = entityManager.find(HoldingDataBeanImpl.class, holdingID);

            if (holding == null) {
                Log.error("TradeJpaAm:sell User " + userID
                          + " attempted to sell holding " + holdingID
                          + " which has already been sold");

                OrderDataBean orderData = new OrderDataBeanImpl();
                orderData.setOrderStatus("cancelled");

                entityManager.persist(orderData);

                entityManager.getTransaction().commit();

                if (entityManager != null) {
                    entityManager.close();
                    entityManager = null;

                }

                return orderData;
            }

            QuoteDataBean quote = holding.getQuote();
            double quantity = holding.getQuantity();

            order = createOrder(account, quote, holding, "sell", quantity,
                                entityManager);
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

            // commit the transaction before calling completeOrder
            entityManager.getTransaction().commit();

            if (orderProcessingMode == TradeConfig.SYNCH)
                completeOrder(order.getOrderID(), false);
            else if (orderProcessingMode == TradeConfig.ASYNCH_2PHASE)
                queueOrder(order.getOrderID(), true);

        }
        catch (Exception e) {
            Log.error("TradeJpaAm:sell(" + userID + "," + holdingID + ") --> failed", e);
            // TODO figure out JPA cancel
            if (order != null)
                order.cancel();

            entityManager.getTransaction().rollback();

            throw new RuntimeException("TradeJpaAm:sell(" + userID + "," + holdingID + ")", e);
        }

        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }

        if (!(order.getOrderStatus().equalsIgnoreCase("cancelled")))
            //after the purchase or sell of a stock, update the stocks volume and price
            updateQuotePriceVolume(order.getSymbol(), TradeConfig.getRandomPriceChangeFactor(), order.getQuantity());

        return order;
    }

    public void queueOrder(Integer orderID, boolean twoPhase) {
        Log
        .error("TradeJpaAm:queueOrder() not implemented for this runtime mode");
        throw new UnsupportedOperationException(
                                               "TradeJpaAm:queueOrder() not implemented for this runtime mode");
    }

    public OrderDataBean completeOrder(Integer orderID, boolean twoPhase)
    throws Exception {
        EntityManager entityManager = emf.createEntityManager();
        OrderDataBeanImpl order = null;

        if (Log.doTrace())
            Log.trace("TradeJpaAm:completeOrder", orderID + " twoPhase=" + twoPhase);

        order = entityManager.find(OrderDataBeanImpl.class, orderID);
        order.getQuote();

        if (order == null) {
            Log.error("TradeJpaAm:completeOrder -- Unable to find Order " + orderID + " FBPK returned " + order);
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

        if (Log.doTrace())
            Log.trace("TradeJpaAm:completeOrder--> Completing Order "
                      + order.getOrderID() + "\n\t Order info: " + order
                      + "\n\t Account info: " + account + "\n\t Quote info: "
                      + quote + "\n\t Holding info: " + holding);

        HoldingDataBean newHolding = null;
        if (order.isBuy()) {
            /*
             * Complete a Buy operation - create a new Holding for the Account -
             * deduct the Order cost from the Account balance
             */

            newHolding = createHolding(account, quote, quantity, price, entityManager);
        }

        try {
            entityManager.getTransaction().begin();

            if (newHolding != null) {
                order.setHolding(newHolding);
            }

            if (order.isSell()) {
                /*
                 * Complete a Sell operation - remove the Holding from the Account -
                 * deposit the Order proceeds to the Account balance
                 */
                if (holding == null) {
                    Log.error("TradeJpaAm:completeOrder -- Unable to sell order " + order.getOrderID() + " holding already sold");
                    order.cancel();
                    entityManager.getTransaction().commit();
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
                Log.trace("TradeJpaAm:completeOrder--> Completed Order "
                          + order.getOrderID() + "\n\t Order info: " + order
                          + "\n\t Account info: " + account + "\n\t Quote info: "
                          + quote + "\n\t Holding info: " + holding);

            entityManager.getTransaction().commit();
        }
        catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
        }

        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }

        return order;
    }

    public void cancelOrder(Integer orderID, boolean twoPhase) {
        EntityManager entityManager = emf.createEntityManager();

        if (Log.doTrace())
            Log.trace("TradeJpaAm:cancelOrder", orderID + " twoPhase=" + twoPhase);

        OrderDataBeanImpl order = entityManager.find(OrderDataBeanImpl.class, orderID);
        /*
         * managed transaction
         */
        try {
            entityManager.getTransaction().begin();
            order.cancel();
            entityManager.getTransaction().commit();
        }
        catch (Exception e) {
            entityManager.getTransaction().rollback();
            entityManager.close();
            entityManager = null;
        }
        entityManager.close();
    }

    public void orderCompleted(String userID, Integer orderID) {
        if (Log.doActionTrace())
            Log.trace("TradeAction:orderCompleted", userID, orderID);
        if (Log.doTrace())
            Log.trace("OrderCompleted", userID, orderID);
    }

    public Collection<OrderDataBean> getOrders(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:getOrders", userID);
        EntityManager entityManager = emf.createEntityManager();
        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        AccountDataBean account = profile.getAccount();
        entityManager.close();
        return account.getOrders();
    }

    public Collection<OrderDataBean> getClosedOrders(String userID) {

        if (Log.doTrace())
            Log.trace("TradeJpaAm:getClosedOrders", userID);
        EntityManager entityManager = emf.createEntityManager();

        try {

            // Get the primary keys for all the closed Orders for this
            // account.
            /*
             * managed transaction
             */
            entityManager.getTransaction().begin();
            Query query = entityManager.createNamedQuery("orderejb.closedOrders");
            query.setParameter("userID", userID);

            entityManager.getTransaction().commit();
            Collection results = query.getResultList();
            Iterator itr = results.iterator();
            // Spin through the orders to populate the lazy quote fields
            while (itr.hasNext()) {
                OrderDataBeanImpl thisOrder = (OrderDataBeanImpl) itr.next();
                thisOrder.getQuote();
            }

            if (TradeConfig.jpaLayer == TradeConfig.OPENJPA) {
                Query updateStatus = entityManager.createNamedQuery("orderejb.completeClosedOrders");
                /*
                 * managed transaction
                 */
                try {
                    entityManager.getTransaction().begin();
                    updateStatus.setParameter("userID", userID);

                    updateStatus.executeUpdate();
                    entityManager.getTransaction().commit();
                }
                catch (Exception e) {
                    entityManager.getTransaction().rollback();
                    entityManager.close();
                    entityManager = null;
                }
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
            if (entityManager != null) {
                entityManager.close();
                entityManager = null;
            }
            return results;
        }
        catch (Exception e) {
            Log.error("TradeJpaAm.getClosedOrders", e);
            entityManager.close();
            entityManager = null;
            throw new RuntimeException(
                                      "TradeJpaAm.getClosedOrders - error", e);

        }

    }

    public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) {

        EntityManager entityManager = emf.createEntityManager();
        try {
            QuoteDataBeanImpl quote = new QuoteDataBeanImpl(symbol, companyName, 0, price, price, price, price, 0);
            /*
             * managed transaction
             */
            try {
                entityManager.getTransaction().begin();
                entityManager.persist(quote);
                entityManager.getTransaction().commit();
            }
            catch (Exception e) {
                entityManager.getTransaction().rollback();
            }

            if (Log.doTrace())
                Log.trace("TradeJpaAm:createQuote-->" + quote);

            if (entityManager != null) {
                entityManager.close();
                entityManager = null;
            }
            return quote;
        }
        catch (Exception e) {
            Log.error("TradeJpaAm:createQuote -- exception creating Quote", e);
            entityManager.close();
            entityManager = null;
            throw new RuntimeException(e);
        }
    }

    public QuoteDataBean getQuote(String symbol) {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:getQuote", symbol);
        EntityManager entityManager = emf.createEntityManager();

        QuoteDataBeanImpl qdb = entityManager.find(QuoteDataBeanImpl.class, symbol);

        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }
        return qdb;
    }

    public Collection<QuoteDataBean> getAllQuotes() {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:getAllQuotes");
        EntityManager entityManager = emf.createEntityManager();

        Query query = entityManager.createNamedQuery("quoteejb.allQuotes");

        if (entityManager != null) {
            entityManager.close();
            entityManager = null;

        }
        return query.getResultList();
    }

    public QuoteDataBean updateQuotePriceVolume(String symbol,
                                                BigDecimal changeFactor, double sharesTraded) {
        if (!TradeConfig.getUpdateQuotePrices()) {
            return new QuoteDataBeanImpl();
        }

        if (Log.doTrace())
            Log.trace("TradeJpaAm:updateQuote", symbol, changeFactor);

        /*
         * Add logic to determine JPA layer, because JBoss5' Hibernate 3.3.1GA
         * DB2Dialect and MySQL5Dialect do not work with annotated query
         * "quoteejb.quoteForUpdate" defined in QuoteDataBeanImpl
         */
        EntityManager entityManager = emf.createEntityManager();
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

        /*
         * managed transaction
         */

        try {

            quote.setPrice(newPrice);
            quote.setVolume(quote.getVolume() + sharesTraded);
            quote.setChange((newPrice.subtract(quote.getOpen()).doubleValue()));

            entityManager.getTransaction().begin();
            entityManager.merge(quote);
            entityManager.getTransaction().commit();
        }
        catch (Exception e) {
            entityManager.getTransaction().rollback();
        }

        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }

        this.publishQuotePriceChange(quote, oldPrice, changeFactor, sharesTraded);

        return quote;
    }

    public Collection<HoldingDataBean> getHoldings(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:getHoldings", userID);
        EntityManager entityManager = emf.createEntityManager();
        /*
         * managed transaction
         */
        entityManager.getTransaction().begin();

        Query query = entityManager.createNamedQuery("holdingejb.holdingsByUserID");
        query.setParameter("userID", userID);

        entityManager.getTransaction().commit();
        Collection<HoldingDataBean> holdings = query.getResultList();
        /*
         * Inflate the lazy data members
         */
        Iterator itr = holdings.iterator();
        while (itr.hasNext()) {
            ((HoldingDataBean) itr.next()).getQuote();
        }

        entityManager.close();
        entityManager = null;
        return holdings;
    }

    public HoldingDataBean getHolding(Integer holdingID) {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:getHolding", holdingID);
        EntityManager entityManager = emf.createEntityManager();
        return entityManager.find(HoldingDataBeanImpl.class, holdingID);
    }

    public AccountDataBean getAccountData(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:getAccountData", userID);

        EntityManager entityManager = emf.createEntityManager();

        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        /*
         * Inflate the lazy data memebers
         */
        AccountDataBean account = profile.getAccount();
        account.getProfile();

        // Added to populate transient field for account
        account.setProfileID(profile.getUserID());
        entityManager.close();
        entityManager = null;

        return account;
    }

    public AccountProfileDataBean getAccountProfileData(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:getProfileData", userID);
        EntityManager entityManager = emf.createEntityManager();

        AccountProfileDataBeanImpl apb = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        entityManager.close();
        entityManager = null;
        return apb;
    }

    public AccountProfileDataBean updateAccountProfile(String userID, String password, String fullName, String address, String email, String creditcard) throws Exception {

        EntityManager entityManager = emf.createEntityManager();

        if (Log.doTrace())
            Log.trace("TradeJpaAm:updateAccountProfileData", userID);
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
        /*
         * Managed Transaction
         */
        try {

            entityManager.getTransaction().begin();
            entityManager.merge(temp);
            entityManager.getTransaction().commit();
            entityManager.close();
        }
        catch (Exception e) {
            entityManager.getTransaction().rollback();
            entityManager.close();
            entityManager = null;
        }

        return temp;
    }

    public AccountDataBean login(String userID, String password)
    throws Exception {

        EntityManager entityManager = emf.createEntityManager();

        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);

        if (profile == null) {
            throw new RuntimeException("No such user: " + userID);
        }
        /*
         * Managed Transaction
         */
        entityManager.getTransaction().begin();
        entityManager.merge(profile);

        AccountDataBean account = profile.getAccount();

        if (Log.doTrace())
            Log.trace("TradeJpaAm:login", userID, password);

        account.login(password);
        entityManager.getTransaction().commit();
        if (Log.doTrace())
            Log.trace("TradeJpaAm:login(" + userID + "," + password + ") success" + account);
        entityManager.close();
        return account;
    }

    public void logout(String userID) {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:logout", userID);
        EntityManager entityManager = emf.createEntityManager();

        AccountProfileDataBeanImpl profile = entityManager.find(AccountProfileDataBeanImpl.class, userID);
        AccountDataBean account = profile.getAccount();

        /*
         * Managed Transaction
         */
        try {
            entityManager.getTransaction().begin();
            account.logout();
            entityManager.getTransaction().commit();
            entityManager.close();
        }
        catch (Exception e) {
            entityManager.getTransaction().rollback();
            entityManager.close();
            entityManager = null;
        }

        if (Log.doTrace())
            Log.trace("TradeJpaAm:logout(" + userID + ") success");
    }

    public AccountDataBean register(String userID, String password, String fullname, 
                                    String address, String email, String creditcard,
                                    BigDecimal openBalance) {
        AccountDataBeanImpl account = null;
        AccountProfileDataBeanImpl profile = null;
        EntityManager entityManager = emf.createEntityManager();

        if (Log.doTrace())
            Log.trace("TradeJpaAm:register", userID, password, fullname, address, email, creditcard, openBalance);

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
            /*
             * managed Transaction
             */
            try {
                entityManager.getTransaction().begin();
                entityManager.persist(profile);
                entityManager.persist(account);
                entityManager.getTransaction().commit();
            }
            catch (Exception e) {
                entityManager.getTransaction().rollback();
                entityManager.close();
                entityManager = null;
            }
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
        Log.error("TradeJpaAm:publishQuotePriceChange - is not implemented for this runtime mode");
        throw new UnsupportedOperationException("TradeJpaAm:publishQuotePriceChange - is not implemented for this runtime mode");
    }

    /*
     * new Method() that takes EntityManager as a parameter
     */
    private OrderDataBean createOrder(AccountDataBean account,
                                      QuoteDataBean quote, HoldingDataBean holding, String orderType,
                                      double quantity, EntityManager entityManager) {
        OrderDataBeanImpl order;
        if (Log.doTrace())
            Log.trace("TradeJpaAm:createOrder(orderID=" + " account="
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
            Log.error("TradeJpaAm:createOrder -- failed to create Order", e);
            throw new RuntimeException("TradeJpaAm:createOrder -- failed to create Order", e);
        }
        return order;
    }

    private HoldingDataBean createHolding(AccountDataBean account,
                                          QuoteDataBean quote, double quantity, BigDecimal purchasePrice,
                                          EntityManager entityManager) throws Exception {
        HoldingDataBeanImpl newHolding = new HoldingDataBeanImpl(quantity,
                                                         purchasePrice, new Timestamp(System.currentTimeMillis()),
                                                         account, quote);
        try {
            /*
             * manage transactions
             */
            entityManager.getTransaction().begin();
            entityManager.persist(newHolding);
            entityManager.getTransaction().commit();
        }
        catch (Exception e) {
            entityManager.getTransaction().rollback();
            entityManager.close();
            entityManager = null;
        }
        return newHolding;
    }

    public double investmentReturn(double investment, double NetValue)
    throws Exception {
        if (Log.doTrace())
            Log.trace("TradeJpaAm:investmentReturn");

        double diff = NetValue - investment;
        double ir = diff / investment;
        return ir;
    }

    public QuoteDataBean pingTwoPhase(String symbol) throws Exception {
        Log
        .error("TradeJpaAm:pingTwoPhase - is not implemented for this runtime mode");
        throw new UnsupportedOperationException("TradeJpaAm:pingTwoPhase - is not implemented for this runtime mode");
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
        return TradeConfig.JPA_AM;
    }

}

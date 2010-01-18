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
package org.apache.aries.samples.daytrader.api;


import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import org.apache.aries.samples.daytrader.persistence.api.AccountDataBean;
import org.apache.aries.samples.daytrader.persistence.api.AccountProfileDataBean;
import org.apache.aries.samples.daytrader.persistence.api.HoldingDataBean;
import org.apache.aries.samples.daytrader.persistence.api.MarketSummaryDataBean;
import org.apache.aries.samples.daytrader.persistence.api.OrderDataBean;
import org.apache.aries.samples.daytrader.persistence.api.QuoteDataBean;

/**
  * TradeServices interface specifies the business methods provided by the Trade online broker application.
  * These business methods represent the features and operations that can be performed by customers of 
  * the brokerage such as login, logout, get a stock quote, buy or sell a stock, etc.
  * This interface is implemented by {@link Trade} providing an EJB implementation of these
  * business methods and also by {@link TradeDirect} providing a JDBC implementation.
  *
  * @see Trade
  * @see TradeDirect
  *
  */ 
public interface TradeServices extends Remote {

   /**
	 * Compute and return a snapshot of the current market conditions
	 * This includes the TSIA - an index of the price of the top 100 Trade stock quotes
	 * The openTSIA ( the index at the open)
	 * The volume of shares traded,
	 * Top Stocks gain and loss
	 *
	 * @return A snapshot of the current market summary
	 */
	public MarketSummaryDataBean getMarketSummary() throws Exception, RemoteException;


   /**
	 * Purchase a stock and create a new holding for the given user.
	 * Given a stock symbol and quantity to purchase, retrieve the current quote price,
	 * debit the user's account balance, and add holdings to user's portfolio.
	 * buy/sell are asynchronous, using J2EE messaging, 
	 * A new order is created and submitted for processing to the TradeBroker
	 *
	 * @param userID the customer requesting the stock purchase
	 * @param symbol the symbol of the stock being purchased
	 * @param quantity the quantity of shares to purchase	 
	 * @return OrderDataBean providing the status of the newly created buy order
	 */


	public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) throws Exception, RemoteException;

   /**
	 * Sell a stock holding and removed the holding for the given user.
	 * Given a Holding, retrieve current quote, credit user's account,
	 * and reduce holdings in user's portfolio.
	 *
	 * @param userID the customer requesting the sell
 	 * @param holdingID the users holding to be sold
	 * @return OrderDataBean providing the status of the newly created sell order
	 */
	public OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) throws Exception, RemoteException;


   /**
	 * Queue the Order identified by orderID to be processed 
	 * 
	 * Orders are submitted through JMS to a Trading Broker
	 * and completed asynchronously. This method queues the order for processing
	 * 
	 * The boolean twoPhase specifies to the server implementation whether or not the
	 * method is to participate in a global transaction
	 *
	 * @param orderID the Order being queued for processing
	 * @return OrderDataBean providing the status of the completed order
	 */
	public void queueOrder(Integer orderID, boolean twoPhase) throws Exception, RemoteException;

   /**
	 * Complete the Order identefied by orderID
	 * Orders are submitted through JMS to a Trading agent
	 * and completed asynchronously. This method completes the order
	 * For a buy, the stock is purchased creating a holding and the users account is debited
	 * For a sell, the stock holding is removed and the users account is credited with the proceeds
	 * 
	 * The boolean twoPhase specifies to the server implementation whether or not the
	 * method is to participate in a global transaction
	 *
	 * @param orderID the Order to complete
	 * @return OrderDataBean providing the status of the completed order
	 */
	public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception, RemoteException;
	
   /**
	 * Cancel the Order identefied by orderID
	 * 
	 * The boolean twoPhase specifies to the server implementation whether or not the
	 * method is to participate in a global transaction
	 *
	 * @param orderID the Order to complete
	 * @return OrderDataBean providing the status of the completed order
	 */
	public void cancelOrder(Integer orderID, boolean twoPhase) throws Exception, RemoteException;


   /**
	 * Signify an order has been completed for the given userID
	 * 
	 * @param userID the user for which an order has completed
	 * @param orderID the order which has completed
	 * 
	 */
	public void orderCompleted(String userID, Integer orderID) throws Exception, RemoteException;
	

   /**
	 * Get the collection of all orders for a given account
	 *
	 * @param userID the customer account to retrieve orders for
	 * @return Collection OrderDataBeans providing detailed order information
	 */
	public Collection getOrders(String userID) throws Exception, RemoteException;

   /**
	 * Get the collection of completed orders for a given account that need to be alerted to the user
	 *
	 * @param userID the customer account to retrieve orders for
	 * @return Collection OrderDataBeans providing detailed order information
	 */
	public Collection getClosedOrders(String userID) throws Exception, RemoteException;


	/**
	 * Given a market symbol, price, and details, create and return a new {@link QuoteDataBean}
	 *
	 * @param symbol the symbol of the stock
	 * @param price the current stock price
	 * @param details a short description of the stock or company
	 * @return a new QuoteDataBean or null if Quote could not be created
	 */
	public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws Exception, RemoteException;

   /**
	 * Return a {@link QuoteDataBean} describing a current quote for the given stock symbol
	 *
	 * @param symbol the stock symbol to retrieve the current Quote
	 * @return the QuoteDataBean
	 */
	public QuoteDataBean getQuote(String symbol) throws Exception, RemoteException;

   /**
	 * Return a {@link java.util.Collection} of {@link QuoteDataBean} 
	 * describing all current quotes
	 * @return A collection of  QuoteDataBean
	 */
	public Collection getAllQuotes() throws Exception, RemoteException;

   /**
	 * Update the stock quote price and volume for the specified stock symbol
	 *
	 * @param symbol for stock quote to update
	 * @param price the updated quote price
	 * @return the QuoteDataBean describing the stock
	 */
	public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal newPrice, double sharesTraded) throws Exception, RemoteException;

		
   /**
	 * Return the portfolio of stock holdings for the specified customer
	 * as a collection of HoldingDataBeans
	 *
	 * @param userID the customer requesting the portfolio	 
	 * @return Collection of the users portfolio of stock holdings
	 */
	public Collection getHoldings(String userID) throws Exception, RemoteException;

   /**
	 * Return a specific user stock holding identifed by the holdingID
	 *
	 * @param holdingID the holdingID to return	 
	 * @return a HoldingDataBean describing the holding
	 */
	public HoldingDataBean getHolding(Integer holdingID) throws Exception, RemoteException;

	/**
	 * Return an AccountDataBean object for userID describing the account
	 *
	 * @param userID the account userID to lookup
	 * @return User account data in AccountDataBean
	 */	
   public AccountDataBean getAccountData(String userID) 
   		throws Exception, RemoteException;                              

	/**
	 * Return an AccountProfileDataBean for userID providing the users profile
	 *
	 * @param userID the account userID to lookup
	 * @param User account profile data in AccountProfileDataBean
	 */
   public AccountProfileDataBean getAccountProfileData(String userID) throws Exception, RemoteException;                              

	/**
	 * Update userID's account profile information using the provided AccountProfileDataBean object
	 *
	 * @param userID the account userID to lookup
         * @param password the updated password
         * @param fullName the updated fullName
         * @param address the updated address
         * @param address the updated email
         * @param the updated creditcard
	 */
   public AccountProfileDataBean updateAccountProfile(String userID, String password, String fullName, String address, String email, String creditcard) throws Exception, RemoteException;                              


	/**
	 * Attempt to authenticate and login a user with the given password
	 *
	 * @param userID the customer to login
	 * @param password the password entered by the customer for authentication
	 * @return User account data in AccountDataBean
	 */
   public AccountDataBean login(String userID, String password) throws Exception, RemoteException;                              

	/**
	 * Logout the given user
	 *
	 * @param userID the customer to logout 
	 * @return the login status
	 */

   public void logout(String userID) throws Exception, RemoteException; 
                                            
	/**
	 * Register a new Trade customer.
	 * Create a new user profile, user registry entry, account with initial balance,
	 * and empty portfolio.
	 *
	 * @param userID the new customer to register
	 * @param password the customers password
	 * @param fullname the customers fullname
	 * @param address  the customers street address
	 * @param email    the customers email address
	 * @param creditcard the customers creditcard number
	 * @param initialBalance the amount to charge to the customers credit to open the account and set the initial balance
	 * @return the userID if successful, null otherwise
	 */
	public AccountDataBean register(String userID,
								  String password,
								  String fullname,
								  String address,
								  String email,
								  String creditcard,
								  BigDecimal openBalance) throws Exception, RemoteException;  

   /**
    * Get mode - returns the persistence mode
    * (TradeConfig.JDBC, JPA, etc...)
    *
    * @return int mode
    */
    public int getMode();

}   


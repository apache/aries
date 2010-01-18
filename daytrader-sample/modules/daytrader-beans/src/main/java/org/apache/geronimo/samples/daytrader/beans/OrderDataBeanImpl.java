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
package org.apache.geronimo.samples.daytrader.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.geronimo.samples.daytrader.util.Log;
import org.apache.geronimo.samples.daytrader.util.TradeConfig;
import org.apache.geronimo.samples.daytrader.persistence.api.AccountDataBean;
import org.apache.geronimo.samples.daytrader.persistence.api.HoldingDataBean;
import org.apache.geronimo.samples.daytrader.persistence.api.OrderDataBean;
import org.apache.geronimo.samples.daytrader.persistence.api.QuoteDataBean;


public class OrderDataBeanImpl implements OrderDataBean, Serializable {

          
    private Integer orderID;            /* orderID */
    
    
    private String orderType;           /* orderType (buy, sell, etc.) */
    
    
    private String orderStatus;         /* orderStatus (open, processing, completed, closed, cancelled) */
    
    
    private Date openDate;              /* openDate (when the order was entered) */
    
    
    private Date completionDate;		/* completionDate */
    
    
    private double quantity;			/* quantity */
    
    
    private BigDecimal price;				/* price */
    
    
    private BigDecimal orderFee;			/* price */
    
    
    private AccountDataBean account;
    
    
    private QuoteDataBean quote;
    
   
    private HoldingDataBean holding;

    private String symbol;

    public OrderDataBeanImpl() {        
    }

    public OrderDataBeanImpl(Integer orderID,
                            String orderType,
                            String orderStatus,
                            Date openDate,
                            Date completionDate,
                            double quantity,
                            BigDecimal price,
                            BigDecimal orderFee,
                            String symbol
                            ) {
        setOrderID(orderID);
        setOrderType(orderType);
        setOrderStatus(orderStatus);
        setOpenDate(openDate);
        setCompletionDate(completionDate);
        setQuantity(quantity);
        setPrice(price);
        setOrderFee(orderFee);
        setSymbol(symbol);
    }
    
    public OrderDataBeanImpl(String orderType,
            String orderStatus,
            Date openDate,
            Date completionDate,
            double quantity,
            BigDecimal price,
            BigDecimal orderFee,
            AccountDataBean account,
            QuoteDataBean quote, HoldingDataBean holding) {
        setOrderType(orderType);
        setOrderStatus(orderStatus);
        setOpenDate(openDate);
        setCompletionDate(completionDate);
        setQuantity(quantity);
        setPrice(price);
        setOrderFee(orderFee);
        setAccount(account);
        setQuote(quote);
        setHolding(holding);
    }

    public static OrderDataBean getRandomInstance() {
        return new OrderDataBeanImpl(
            new Integer(TradeConfig.rndInt(100000)),
            TradeConfig.rndBoolean() ? "buy" : "sell",
            "open",
            new java.util.Date(TradeConfig.rndInt(Integer.MAX_VALUE)),
            new java.util.Date(TradeConfig.rndInt(Integer.MAX_VALUE)),
            TradeConfig.rndQuantity(),
            TradeConfig.rndBigDecimal(1000.0f),
            TradeConfig.rndBigDecimal(1000.0f),
            TradeConfig.rndSymbol()
        );
    }

    public String toString()
    {
        return "Order " + getOrderID()
                + "\n\t      orderType: " + getOrderType()
                + "\n\t    orderStatus: " +	getOrderStatus()
                + "\n\t       openDate: " +	getOpenDate()
                + "\n\t completionDate: " +	getCompletionDate()
                + "\n\t       quantity: " +	getQuantity()
                + "\n\t          price: " +	getPrice()
                + "\n\t       orderFee: " +	getOrderFee()
                + "\n\t         symbol: " +	getSymbol()
                ;
    }
    public String toHTML()
    {
        return "<BR>Order <B>" + getOrderID() + "</B>"
                + "<LI>      orderType: " + getOrderType() + "</LI>"
                + "<LI>    orderStatus: " +	getOrderStatus() + "</LI>"
                + "<LI>       openDate: " +	getOpenDate() + "</LI>"
                + "<LI> completionDate: " +	getCompletionDate() + "</LI>"
                + "<LI>       quantity: " +	getQuantity() + "</LI>"
                + "<LI>          price: " +	getPrice() + "</LI>"
                + "<LI>       orderFee: " +	getOrderFee() + "</LI>"
                + "<LI>         symbol: " +	getSymbol() + "</LI>"
                ;
    }

    public void print()
    {
        Log.log( this.toString() );
    }

    public Integer getOrderID() {
        return orderID;
    }

    public void setOrderID(Integer orderID) {
        this.orderID = orderID;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }


    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOrderFee() {
        return orderFee;
    }

    public void setOrderFee(BigDecimal orderFee) {
        this.orderFee = orderFee;
    }

    public String getSymbol() {
        if (quote != null) {
            return quote.getSymbol();
        }
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public AccountDataBean getAccount() {
        return account;
    }

    public void setAccount(AccountDataBean account) {
        this.account = account;
    }

    public QuoteDataBean getQuote() {
        return quote;
    }

    public void setQuote(QuoteDataBean quote) {
        this.quote = quote;
    }

    public HoldingDataBean getHolding() {
        return holding;
    }

    public void setHolding(HoldingDataBean holding) {
        this.holding = holding;
    }

    public boolean isBuy()
    {
    	String orderType = getOrderType();
    	if ( orderType.compareToIgnoreCase("buy") == 0 )
    		return true;
    	return false;
    }

    public boolean isSell()
    {
    	String orderType = getOrderType();
    	if ( orderType.compareToIgnoreCase("sell") == 0 )
    		return true;
    	return false;
    }

    public boolean isOpen()
    {
    	String orderStatus = getOrderStatus();
    	if ( (orderStatus.compareToIgnoreCase("open") == 0) ||
	         (orderStatus.compareToIgnoreCase("processing") == 0) )
	    		return true;
    	return false;
    }

    public boolean isCompleted()
    {
    	String orderStatus = getOrderStatus();
    	if ( (orderStatus.compareToIgnoreCase("completed") == 0) ||
	         (orderStatus.compareToIgnoreCase("alertcompleted") == 0)    ||
	         (orderStatus.compareToIgnoreCase("cancelled") == 0) )
	    		return true;
    	return false;
    }

    public boolean isCancelled()
    {
    	String orderStatus = getOrderStatus();
    	if (orderStatus.compareToIgnoreCase("cancelled") == 0)
	    		return true;
    	return false;
    }


	public void cancel()
	{
		setOrderStatus("cancelled");
	}

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.orderID != null ? this.orderID.hashCode() : 0);
        return hash;
    }
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof OrderDataBeanImpl)) {
            return false;
        }
        OrderDataBeanImpl other = (OrderDataBeanImpl)object;
        if (this.orderID != other.orderID && (this.orderID == null || !this.orderID.equals(other.orderID))) return false;
        return true;
    }
}


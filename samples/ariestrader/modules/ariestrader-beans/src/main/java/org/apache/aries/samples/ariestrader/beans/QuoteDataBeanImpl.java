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
package org.apache.aries.samples.ariestrader.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import org.apache.aries.samples.ariestrader.util.Log;
import org.apache.aries.samples.ariestrader.util.TradeConfig;
import org.apache.aries.samples.ariestrader.api.persistence.QuoteDataBean;


public class QuoteDataBeanImpl implements QuoteDataBean, Serializable {
    
    private String symbol;          /* symbol */
    
   
    private String companyName;     /* companyName */
    
    
    private double volume;          /* volume */
    
    
    private BigDecimal price;       /* price */
    
    
    private BigDecimal open1;       /* open1 price */
    
    
    private BigDecimal low;         /* low price */
    
    
    private BigDecimal high;        /* high price */
    
    
    private double change1;         /* price change */
    
    
    
    public QuoteDataBeanImpl() {
    }

    public QuoteDataBeanImpl(String symbol, String companyName, double volume,
            BigDecimal price, BigDecimal open, BigDecimal low,
            BigDecimal high, double change) {
        setSymbol(symbol);
        setCompanyName(companyName);
        setVolume(volume);
        setPrice(price);
        setOpen(open);
        setLow(low);
        setHigh(high);
        setChange(change);
    }

    public static QuoteDataBean getRandomInstance() {
        return new QuoteDataBeanImpl(
                TradeConfig.rndSymbol(),                 //symbol
                TradeConfig.rndSymbol() + " Incorporated",         //Company Name
                TradeConfig.rndFloat(100000),            //volume
                TradeConfig.rndBigDecimal(1000.0f),     //price
                TradeConfig.rndBigDecimal(1000.0f),     //open1
                TradeConfig.rndBigDecimal(1000.0f),     //low
                TradeConfig.rndBigDecimal(1000.0f),     //high
                TradeConfig.rndFloat(100000)            //volume
        );
    }

    //Create a "zero" value QuoteDataBeanImpl for the given symbol
    public QuoteDataBeanImpl(String symbol) {
        setSymbol(symbol);
    }

    public String toString() {
        return "\n\tQuote Data for: " + getSymbol()
                + "\n\t\t companyName: " + getCompanyName()
                + "\n\t\t      volume: " + getVolume()
                + "\n\t\t       price: " + getPrice()
                + "\n\t\t        open1: " + getOpen()
                + "\n\t\t         low: " + getLow()
                + "\n\t\t        high: " + getHigh()
                + "\n\t\t      change1: " + getChange()
                ;
    }

    public String toHTML() {
        return "<BR>Quote Data for: " + getSymbol()
                + "<LI> companyName: " + getCompanyName() + "</LI>"
                + "<LI>      volume: " + getVolume() + "</LI>"
                + "<LI>       price: " + getPrice() + "</LI>"
                + "<LI>        open1: " + getOpen() + "</LI>"
                + "<LI>         low: " + getLow() + "</LI>"
                + "<LI>        high: " + getHigh() + "</LI>"
                + "<LI>      change1: " + getChange() + "</LI>"
                ;
    }

    public void print() {
        Log.log(this.toString());
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOpen() {
        return open1;
    }

    public void setOpen(BigDecimal open) {
        this.open1 = open;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public double getChange() {
        return change1;
    }

    public void setChange(double change) {
        this.change1 = change;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.symbol != null ? this.symbol.hashCode() : 0);
        return hash;
    }
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof QuoteDataBeanImpl)) {
            return false;
        }
        QuoteDataBeanImpl other = (QuoteDataBeanImpl)object;
        if (this.symbol != other.symbol && (this.symbol == null || !this.symbol.equals(other.symbol))) return false;
        return true;
    }
}

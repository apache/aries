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
package org.apache.aries.samples.ariestrader.api.persistence;

import java.math.BigDecimal;
import java.util.Date;

public interface HoldingDataBean {

    public String toString();

    public String toHTML();

    public Integer getHoldingID();

    public void setHoldingID(Integer holdingID);

    public double getQuantity();

    public void setQuantity(double quantity);

    public BigDecimal getPurchasePrice();

    public void setPurchasePrice(BigDecimal purchasePrice);

    public Date getPurchaseDate();

    public void setPurchaseDate(Date purchaseDate);

    public String getQuoteID();

    public void setQuoteID(String quoteID);

    public AccountDataBean getAccount();

    public void setAccount(AccountDataBean account);
    
    public QuoteDataBean getQuote();

    public void setQuote(QuoteDataBean quote);

}

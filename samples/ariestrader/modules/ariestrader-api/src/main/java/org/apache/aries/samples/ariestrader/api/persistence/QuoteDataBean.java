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

public interface QuoteDataBean {
    
    public String toString();

    public String toHTML();

    public String getSymbol();

    public void setSymbol(String symbol);

    public String getCompanyName();

    public void setCompanyName(String companyName);

    public BigDecimal getPrice();

    public void setPrice(BigDecimal price);

    public BigDecimal getOpen();

    public void setOpen(BigDecimal open);

    public BigDecimal getLow();

    public void setLow(BigDecimal low);

    public BigDecimal getHigh();

    public void setHigh(BigDecimal high);

    public double getChange();

    public void setChange(double change);

    public double getVolume();

    public void setVolume(double volume);

}

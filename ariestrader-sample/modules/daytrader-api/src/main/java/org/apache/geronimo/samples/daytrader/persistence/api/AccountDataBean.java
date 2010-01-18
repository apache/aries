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
package org.apache.geronimo.samples.daytrader.persistence.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

public interface AccountDataBean {
    
    public String toString();

    public String toHTML();

    public Integer getAccountID();

    public void setAccountID(Integer accountID);

    public int getLoginCount();

    public void setLoginCount(int loginCount);

    public int getLogoutCount();

    public void setLogoutCount(int logoutCount);

    public Date getLastLogin();

    public void setLastLogin(Date lastLogin);

    public Date getCreationDate();

    public void setCreationDate(Date creationDate);

    public BigDecimal getBalance();

    public void setBalance(BigDecimal balance);

    public BigDecimal getOpenBalance();

    public void setOpenBalance(BigDecimal openBalance);

    public String getProfileID();

    public void setProfileID(String profileID);

    public Collection<OrderDataBean> getOrders();

    public Collection<HoldingDataBean> getHoldings();

    public AccountProfileDataBean getProfile();

    public void setProfile(AccountProfileDataBean profile);

    public void login(String password);

    public void logout();

}
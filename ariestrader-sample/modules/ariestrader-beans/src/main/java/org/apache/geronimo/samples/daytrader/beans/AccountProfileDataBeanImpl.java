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

import org.apache.geronimo.samples.daytrader.util.Log;
import org.apache.geronimo.samples.daytrader.util.TradeConfig;
import org.apache.geronimo.samples.daytrader.persistence.api.AccountDataBean;
import org.apache.geronimo.samples.daytrader.persistence.api.AccountProfileDataBean;


public class AccountProfileDataBeanImpl implements AccountProfileDataBean, java.io.Serializable {

    
    private String userID;              /* userID */
    
   
    private String passwd;              /* password */
    
    
    private String fullName;            /* fullName */
    
    
    private String address;             /* address */
    
    
    private String email;               /* email */
    
    
    private String creditCard;          /* creditCard */
    
    
    private AccountDataBean account;

//    @Version
//    private Integer optLock;

    public AccountProfileDataBeanImpl() {
    }

    public AccountProfileDataBeanImpl(String userID,
            String password,
            String fullName,
            String address,
            String email,
            String creditCard) {
        setUserID(userID);
        setPassword(password);
        setFullName(fullName);
        setAddress(address);
        setEmail(email);
        setCreditCard(creditCard);
    }

    public static AccountProfileDataBean getRandomInstance() {
        return new AccountProfileDataBeanImpl(
                TradeConfig.rndUserID(),                        // userID
                TradeConfig.rndUserID(),                        // passwd
                TradeConfig.rndFullName(),                      // fullname
                TradeConfig.rndAddress(),                       // address
                TradeConfig.rndEmail(TradeConfig.rndUserID()),  //email
                TradeConfig.rndCreditCard()                     // creditCard
        );
    }

    public String toString() {
        return "\n\tAccount Profile Data for userID:" + getUserID()
                + "\n\t\t   passwd:" + getPassword()
                + "\n\t\t   fullName:" + getFullName()
                + "\n\t\t    address:" + getAddress()
                + "\n\t\t      email:" + getEmail()
                + "\n\t\t creditCard:" + getCreditCard()
                ;
    }

    public String toHTML() {
        return "<BR>Account Profile Data for userID: <B>" + getUserID() + "</B>"
                + "<LI>   passwd:" + getPassword() + "</LI>"
                + "<LI>   fullName:" + getFullName() + "</LI>"
                + "<LI>    address:" + getAddress() + "</LI>"
                + "<LI>      email:" + getEmail() + "</LI>"
                + "<LI> creditCard:" + getCreditCard() + "</LI>"
                ;
    }

    public void print() {
        Log.log(this.toString());
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPassword() {
        return passwd;
    }

    public void setPassword(String password) {
        this.passwd = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(String creditCard) {
        this.creditCard = creditCard;
    }

    public AccountDataBean getAccount() {
        return account;
    }

    public void setAccount(AccountDataBean account) {
        this.account = account;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.userID != null ? this.userID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AccountProfileDataBeanImpl)) {
            return false;
        }
        AccountProfileDataBeanImpl other = (AccountProfileDataBeanImpl)object;
        if (this.userID != other.userID && (this.userID == null || !this.userID.equals(other.userID))) return false;
        return true;
    }
}

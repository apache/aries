/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.bank.loc;

import org.apache.aries.samples.bank.api.Constants;
import org.apache.aries.samples.bank.api.LineOfCreditAccountService;

// We could make an Abstract class to base this and ChequingAccountServiceImpl from if we decide
// to keep those two classes around. 
public class LineOfCreditAccountServiceImpl implements LineOfCreditAccountService {

  class AccountRecord { 
    String name;
    public String getName() {
      return name;
    }
    int balance;
    public int getBalance() {
      return balance;
    }
    public void setBalance(int balance) {
      this.balance = balance;
    }
    public AccountRecord (String name) {
      this.name = name;
      balance = 0;
    }
  }
  
  private static final int BASE = Constants.LINEOFCREDIT_ACCOUNT_BASE;
  private static int nextAccount_ = BASE;
  private static AccountRecord[] _accounts = new AccountRecord[10];

  @Override
  public int open(String name) {
    int accountNumber = nextAccount_++;
    _accounts[accountNumber-BASE] = new AccountRecord (name);
    System.out.println("LineOfCreditAccountServiceImpl.open() = "+accountNumber);
    return accountNumber;
  }

  @Override
  public int balance(int accountNumber) {
    int balance = _accounts[accountNumber-BASE].getBalance();
    System.out.println("LineOfCreditAccountServiceImpl.balance("+accountNumber+") = "+balance);
    return balance;
  }

  @Override
  public void deposit(int accountNumber, int funds) {
    AccountRecord record = _accounts[accountNumber-BASE];
    record.setBalance(record.getBalance() + funds);
    System.out.println("LineOfCreditAccountServiceImpl.deposit("+accountNumber+","+funds+")");
  }

  @Override
  public void withdraw(int accountNumber, int funds) {
    AccountRecord record = _accounts[accountNumber-BASE];
    record.setBalance(record.getBalance() - funds);
    System.out.println("LineOfCreditAccountServiceImpl.withdraw("+accountNumber+","+funds+")");
  }

  @Override
  public String name(int accountNumber) {
    String name =_accounts[accountNumber-BASE].getName();
    System.out.println ("LineOfCreditAccountServiceImpl.getName("+accountNumber+" = " + name);
    return name;
  }

}

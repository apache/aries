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

package org.apache.aries.samples.bank.biz;

import org.apache.aries.samples.bank.api.AccountService;
import org.apache.aries.samples.bank.api.AccountServicesToOutsideWorld;
import org.apache.aries.samples.bank.api.ChequingAccountService;
import org.apache.aries.samples.bank.api.Constants;
import org.apache.aries.samples.bank.api.CreditCheckService;
import org.apache.aries.samples.bank.api.LineOfCreditAccountService;

public class AccountServicesToOutsideWorldImpl implements AccountServicesToOutsideWorld {

  private ChequingAccountService _chequingAccountService;
  private LineOfCreditAccountService _lineOfCreditAccountService;
  private CreditCheckService _creditCheckService;
  private double _riskThreshold;

  public void setChequingAccountService(ChequingAccountService c) {
    _chequingAccountService = c;
  }

  public void setLineOfCreditAccountService(LineOfCreditAccountService l) {
    _lineOfCreditAccountService = l;
  }

  public void setCreditCheckService(CreditCheckService c) {
    _creditCheckService = c;
  }

  public void setRiskThreshold(double r) {
    _riskThreshold = r;
  }

  private static final int NO_ACCOUNT = -1; 

  @Override
  public int openChequingAccount(String name, int assets, int liabilities) {
    int accountNumber = _chequingAccountService.open(name);
    System.out.println("AccountAccessServiceImpl.openChequingAccount(" + name + "," + assets + ","
        + liabilities + ") = " + accountNumber);
    return accountNumber;
  }

  @Override
  public int openLineOfCreditAccount(String name, int assets, int liabilities) {
    System.out.println("AccountAccessServiceImpl.openLineOfCreditAccount(" + name + "," + assets
        + "," + liabilities + ") riskThreshold = " + _riskThreshold);
    double risk = _creditCheckService.risk(name, assets, liabilities);
    int accountNumber = NO_ACCOUNT;
    if (risk < _riskThreshold)
      accountNumber = _lineOfCreditAccountService.open(name);
    System.out.println("AccountAccessServiceImpl.openLineOfCreditAccount(" + name + "," + assets
        + "," + liabilities + ") = " + accountNumber);
    return accountNumber;
  }

  @Override
  public int balance(int accountNumber) {
    int balance = accountServiceFor(accountNumber).balance(accountNumber);
    System.out.println("AccountAccessServiceImpl.balance(" + accountNumber + ") = " + balance);
    return balance;
  }

  @Override
  public void deposit(int accountNumber, int funds) {
    accountServiceFor(accountNumber).deposit(accountNumber, funds);
    System.out.println("AccountAccessServiceImpl.deposit(" + accountNumber + "," + funds + ")");
  }

  @Override
  public void withdraw(int accountNumber, int funds) {
    accountServiceFor(accountNumber).withdraw(accountNumber, funds);
    System.out.println("AccountAccessServiceImpl.withdraw(" + accountNumber + "," + funds + ")");
  }

  @Override
  public void transfer(int fromAccountNumber, int toAccountNumber, int funds) {
    withdraw(fromAccountNumber, funds);
    deposit(toAccountNumber, funds);
    System.out.println("AccountAccessServiceImpl.transfer(" + fromAccountNumber + ","
        + toAccountNumber + "," + funds + ")");
  }

  @Override
  public String name(int accountNumber) {
    String result = accountServiceFor(accountNumber).name(accountNumber);
    System.out.println("AccountServicesToOutsideWorldImpl.name(" + accountNumber + ") = " + result);
    return result;
  }

  private AccountService accountServiceFor(int accountNumber) {
    if (accountNumber >= Constants.CHEQUING_ACCOUNT_BASE
        && accountNumber <= Constants.CHEQUING_ACCOUNT_MAX)
      return _chequingAccountService;
    else if (accountNumber >= Constants.LINEOFCREDIT_ACCOUNT_BASE
        && accountNumber <= Constants.LINEOFCREDIT_ACCOUNT_MAX)
      return _lineOfCreditAccountService;
    else
      return null;
  }
}

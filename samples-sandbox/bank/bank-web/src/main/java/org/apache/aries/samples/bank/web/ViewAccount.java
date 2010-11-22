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
package org.apache.aries.samples.bank.web;

import java.io.IOException;
import java.io.Writer;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.aries.samples.bank.api.AccountServicesToOutsideWorld;

/**
 * Servlet implementation class ViewAccount
 */
public class ViewAccount extends HttpServlet {
  private static final long serialVersionUID = 1L;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    process(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    process(request, response);
  }

  private void process(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    int accountNumber = Integer.parseInt(request.getParameter("accountNumber"));
    Writer writer = response.getWriter();
    writer.write("<html><head></head><body>");

    AccountServicesToOutsideWorld accAccess;
    try {
      InitialContext ic = new InitialContext();
      accAccess = (AccountServicesToOutsideWorld) ic.lookup("osgi:service/"
          + AccountServicesToOutsideWorld.class.getName());
    } catch (NamingException nx) {
      throw new ServletException(nx);
    }

    if (accAccess != null) {
      String operation = request.getParameter("operation");
      if (operation != null) {
        int amount = Integer.parseInt(request.getParameter("amount"));
        if (operation.equals("deposit")) {
          accAccess.deposit(accountNumber, amount);
        } else if (operation.equals("withdraw")) {
          accAccess.withdraw(accountNumber, amount);
        } else {
          System.out.println("Unknown operation " + operation
              + " in ViewAccount");
        }
      }

      String name = accAccess.name(accountNumber);
      int balance = accAccess.balance(accountNumber);
      writer.write("<br/>Account " + accountNumber + " name `"
          + name + "` balance: " + balance);
      
      // Deposit or withdraw
      writer.write("<form action=\"ViewAccount\" method=\"POST\">");
      writer.write("<input type=\"hidden\" name=\"accountNumber\" value=\""
          + accountNumber + "\"/>");
      writer.write("<select name=\"operation\"><option value=\"deposit\">deposit</option>");
      writer.write("<option value=\"withdraw\">withdraw</option></select>");
      writer.write("<input name=\"amount\" type=\"text\"/>");
      writer.write("<input type=\"submit\" value=\"submit request\" /></form>");

      //TODO: transfer
      writer.write("<br/>TODO: Form to make a transfer goes here<br/>");
      writer.write("<a href=\"index.html\">back to main menu</a>");
    } else {
      writer.write("<br/>ERROR: Unable to find AccountAccessService");
    }
   
    writer.write("</body></html>");
    writer.close();
  }
}

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
 * Servlet implementation class CreateAccount
 */
public class CreateAccount extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
 	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String name = request.getParameter("name");
		int assets = Integer.parseInt(request.getParameter("assets"));
		int liabilities = Integer.parseInt(request.getParameter("liabilities"));
		String accountType = request.getParameter("accountType");
		Writer writer = response.getWriter();
		
		AccountServicesToOutsideWorld accAccess;
		try { 
			InitialContext ic = new InitialContext();
			accAccess = (AccountServicesToOutsideWorld) ic.lookup
			  ("osgi:service/" + AccountServicesToOutsideWorld.class.getName());
		} catch (NamingException nx) { 
			throw new ServletException (nx);
		}
		
		int newAccountNumber;
		if (accAccess != null) { 
			if (accountType.equals("Chequing")) { 
				newAccountNumber = accAccess.openChequingAccount(name, assets, liabilities);
			} else { 
				newAccountNumber = accAccess.openLineOfCreditAccount(name, assets, liabilities);
			}
			
			writer.write("<html><head></head><body>");
			if (newAccountNumber >= 0) { 
				writer.write ("Successfully opened <a href=\"ViewAccount?accountNumber=" + newAccountNumber 
						+ "\">Account number " + newAccountNumber + "</a>");
			} else { 
				writer.write ("New account request denied: computer says no.");
			}
		} else { 
			writer.write("<br/>INTERNAL ERROR: Unable to find AccountAccessService");
		}
		writer.write("<br/><br/><a href=\"index.html\">back to main menu</a></body></html>");
	}

}

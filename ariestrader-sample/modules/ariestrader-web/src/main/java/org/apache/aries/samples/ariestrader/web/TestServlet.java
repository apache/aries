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
package org.apache.aries.samples.ariestrader.web;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.aries.samples.ariestrader.api.TradeServicesManager;
import org.apache.aries.samples.ariestrader.api.TradeServiceUtilities;
import org.apache.aries.samples.ariestrader.api.TradeServices;
import org.apache.aries.samples.ariestrader.util.*;

import java.io.IOException;
import java.math.BigDecimal;

public class TestServlet extends HttpServlet {

    private static TradeServicesManager tradeServicesManager = null;

	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
	}
	
	
   /**
	* Process incoming HTTP GET requests
	*
	* @param request Object that encapsulates the request to the servlet
	* @param response Object that encapsulates the response from the servlet
	*/
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		performTask(request,response);
	}

   /**
	* Process incoming HTTP POST requests
	*
	* @param request Object that encapsulates the request to the servlet
	* @param response Object that encapsulates the response from the servlet
	*/
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		performTask(request,response);
	}	

   /**
	* Main service method for TradeAppServlet
	*
	* @param request Object that encapsulates the request to the servlet
	* @param response Object that encapsulates the response from the servlet
	*/    	
	public void performTask(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException 
	{
            if (tradeServicesManager == null) {
                tradeServicesManager = TradeServiceUtilities.getTradeServicesManager();
            }
            TradeServices tradeServices = tradeServicesManager.getTradeServices();
		try {
			Log.debug("Enter TestServlet doGet");
			TradeConfig.runTimeMode = TradeConfig.JDBC;
			for (int i=0; i<10; i++) 
			{
				tradeServices.createQuote("s:"+i, "Company " + i, new BigDecimal(i*1.1));
			}
			/*
			
			AccountDataBean accountData = new TradeAction().register("user1", "password", "fullname", "address", 
											"email", "creditCard", new BigDecimal(123.45), false);

			OrderDataBean orderData = new TradeAction().buy("user1", "s:1", 100.0);
			orderData = new TradeAction().buy("user1", "s:2", 200.0);
			Thread.sleep(5000);
			accountData = new TradeAction().getAccountData("user1");
			Collection holdingDataBeans = new TradeAction().getHoldings("user1");
			PrintWriter out = resp.getWriter();
			resp.setContentType("text/html");
			out.write("<HEAD></HEAD><BODY><BR><BR>");
			out.write(accountData.toString());
			Log.printCollection("user1 Holdings", holdingDataBeans);
			ServletContext sc  = getServletContext();
			req.setAttribute("results", "Success");
			req.setAttribute("accountData", accountData);
			req.setAttribute("holdingDataBeans", holdingDataBeans);
			getServletContext().getRequestDispatcher("/tradehome.jsp").include(req, resp);
			out.write("<BR><BR>done.</BODY>");
			*/
		}
		catch (Exception e)
		{
			Log.error("TestServletException", e);
		}
	}
}


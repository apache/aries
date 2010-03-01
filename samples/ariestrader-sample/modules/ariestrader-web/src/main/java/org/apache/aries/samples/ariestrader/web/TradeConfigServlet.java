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

import org.apache.aries.samples.ariestrader.api.TradeDBManager;
import org.apache.aries.samples.ariestrader.api.TradeServiceUtilities;
import org.apache.aries.samples.ariestrader.persistence.api.RunStatsDataBean;
import org.apache.aries.samples.ariestrader.util.Log;
import org.apache.aries.samples.ariestrader.util.TradeConfig;

import java.io.IOException;

/**
 * TradeConfigServlet provides a servlet interface to adjust AriesTrader runtime parameters.
 * TradeConfigServlet updates values in the {@link org.apache.aries.samples.ariestrader.web.TradeConfig} JavaBean holding 
 * all configuration and runtime parameters for the Trade application
 *
 */
public class TradeConfigServlet extends HttpServlet {
	
    private static TradeDBManager tradeDBManager = null;

   /**
	* Servlet initialization method.
	*/
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
	}
	/**
	 * Create the TradeConfig bean and pass it the config.jsp page 
	 * to display the current Trade runtime configuration
	 * Creation date: (2/8/2000 3:43:59 PM)
	 */
	void doConfigDisplay(
		HttpServletRequest req, 
		HttpServletResponse resp, 
		String results)
		throws Exception {

		TradeConfig currentConfig = new TradeConfig();

		req.setAttribute("tradeConfig", currentConfig);
		req.setAttribute("status", results);
		getServletConfig()
			.getServletContext()
			.getRequestDispatcher(TradeConfig.getPage(TradeConfig.CONFIG_PAGE))
			.include(req, resp); 
	}
	
	void doResetTrade(
		HttpServletRequest req, 
		HttpServletResponse resp, 
		String results)
		throws Exception
	{
		RunStatsDataBean runStatsData = new RunStatsDataBean();
		TradeConfig currentConfig = new TradeConfig();		

                if (tradeDBManager == null) {
                    tradeDBManager = TradeServiceUtilities.getTradeDBManager();
                }

		try
		{
			runStatsData = tradeDBManager.resetTrade(false);
			
			req.setAttribute("runStatsData", runStatsData);
			req.setAttribute("tradeConfig", currentConfig);
			results += "Trade Reset completed successfully";						
			req.setAttribute("status", results);

		}
		catch (Exception e)
		{
			results += "Trade Reset Error  - see log for details";
			Log.error(e, 	results);
			throw e;
		}
		getServletConfig()
				.getServletContext()
				.getRequestDispatcher(TradeConfig.getPage(TradeConfig.STATS_PAGE))
				.include(req, resp); 			
		
	}
	
	
	/**
	 * Update Trade runtime configuration paramaters
	 * Creation date: (2/8/2000 3:44:24 PM)
	 */
	void doConfigUpdate(HttpServletRequest req, HttpServletResponse resp)
		throws Exception {

		TradeConfig currentConfig = new TradeConfig();

		String currentConfigStr = "\n\n########## Trade configuration update. Current config:\n\n";
		String runTimeModeStr = req.getParameter("RunTimeMode");
		if (runTimeModeStr != null)
		{
			try
			{
				int i = Integer.parseInt(runTimeModeStr);
				if ((i >= 0) && (i < TradeConfig.runTimeModeNames.length)) //Input validation
                                {
                                    TradeConfig.setRunTimeMode(i);
                                }
			}
			catch (Exception e)
			{
				//>>rjm
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"trying to set runtimemode to " + runTimeModeStr, 
					"reverting to current value");

			} // If the value is bad, simply revert to current
		}
		currentConfigStr += "\t\tRunTimeMode:\t\t" + TradeConfig.runTimeModeNames[TradeConfig.getRunTimeMode()] + "\n";
		
		/* Add JPA layer choice to avoid some ugly Hibernate bugs */
		String jpaLayerStr = req.getParameter("JPALayer");
		if (jpaLayerStr != null)
		{
			try
			{
				int i = Integer.parseInt(jpaLayerStr);
				if ((i >= 0)
					&& (i < TradeConfig.jpaLayerNames.length)) //Input validation
					TradeConfig.jpaLayer = i;
			}
			catch (Exception e)
			{				
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"trying to set JPALayer to " + jpaLayerStr, 
					"reverting to current value");

			} // If the value is bad, simply revert to current
		}
		currentConfigStr += "\t\tJPALayer:\t\t" + TradeConfig.jpaLayerNames[TradeConfig.jpaLayer] + "\n";

		String orderProcessingModeStr = req.getParameter("OrderProcessingMode");
		if (orderProcessingModeStr != null)
		{
			try
			{
				int i = Integer.parseInt(orderProcessingModeStr);
				if ((i >= 0)
					&& (i < TradeConfig.orderProcessingModeNames.length)) //Input validation
					TradeConfig.orderProcessingMode = i;
			}
			catch (Exception e)
			{
				//>>rjm
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"trying to set orderProcessing to " + orderProcessingModeStr, 
					"reverting to current value");

			} // If the value is bad, simply revert to current
		}
		currentConfigStr += "\t\tOrderProcessingMode:\t" + TradeConfig.orderProcessingModeNames[TradeConfig.orderProcessingMode]  + "\n";		
		
		String accessModeStr = req.getParameter("AcessMode");
		if (accessModeStr != null)
		{
			try
			{
				int i = Integer.parseInt(accessModeStr);
				if ((i >= 0)
					&& (i < TradeConfig.accessModeNames.length) && (i != TradeConfig.getAccessMode())) //Input validation
					TradeConfig.setAccessMode(i);
			}
			catch (Exception e)
			{
				//>>rjm
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"trying to set orderProcessing to " + orderProcessingModeStr, 
					"reverting to current value");

			} // If the value is bad, simply revert to current
		}		
		currentConfigStr += "\t\tAcessMode:\t\t" + TradeConfig.accessModeNames[TradeConfig.getAccessMode()]  + "\n";		
		
			
		String workloadMixStr = req.getParameter("WorkloadMix");
		if (workloadMixStr != null)
		{
			try
			{
				int i = Integer.parseInt(workloadMixStr);
				if ((i >= 0)
					&& (i < TradeConfig.workloadMixNames.length)) //Input validation
					TradeConfig.workloadMix = i;
			}
			catch (Exception e)
			{
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"trying to set workloadMix to " + workloadMixStr, 
					"reverting to current value");
			} // If the value is bad, simply revert to current
		}
		currentConfigStr += "\t\tWorkload Mix:\t\t" + TradeConfig.workloadMixNames[TradeConfig.workloadMix]  + "\n";		
		
		
		
		String webInterfaceStr = req.getParameter("WebInterface");
		if (webInterfaceStr != null)
		{
			try
			{
				int i = Integer.parseInt(webInterfaceStr);
				if ((i >= 0)
					&& (i < TradeConfig.webInterfaceNames.length)) //Input validation
					TradeConfig.webInterface = i;
			}
			catch (Exception e)
			{
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"trying to set WebInterface to " + webInterfaceStr, 
					"reverting to current value");


			} // If the value is bad, simply revert to current
		}
		currentConfigStr += "\t\tWeb Interface:\t\t" + TradeConfig.webInterfaceNames[TradeConfig.webInterface]  + "\n";		
		
		String cachingTypeStr = req.getParameter("CachingType");
		if (cachingTypeStr != null)
		{
			try
			{
				int i = Integer.parseInt(cachingTypeStr);
				if ((i >= 0)
					&& (i < TradeConfig.cachingTypeNames.length)) //Input validation
					TradeConfig.cachingType = i;
			}
			catch (Exception e)
			{
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"trying to set CachingType to " + cachingTypeStr, 
					"reverting to current value");
				} // If the value is bad, simply revert to current
		}
		currentConfigStr += "\t\tCachingType:\t\t" + TradeConfig.cachingTypeNames[TradeConfig.cachingType]  + "\n";		

		String parm = req.getParameter("SOAP_URL");
		if ((parm != null) && (parm.length() > 0))
		{
			if (!TradeConfig.getSoapURL().equals(parm)) {
				TradeConfig.setSoapURL(parm);
			}
		}
		else
		{
			TradeConfig.setSoapURL(null);
		}

		parm = req.getParameter("MaxUsers");
		if ((parm != null) && (parm.length() > 0))
		{
			try
			{
				TradeConfig.setMAX_USERS(Integer.parseInt(parm));
			}
			catch (Exception e)
			{
				Log.error(
					e, 
					"TradeConfigServlet.doConfigUpdate(..): minor exception caught", 
					"Setting maxusers, probably error parsing string to int:" + parm, 
					"revertying to current value: " + TradeConfig.getMAX_USERS());

			} //On error, revert to saved
		}
		parm = req.getParameter("MaxQuotes");
		if ((parm != null) && (parm.length() > 0))
		{
			try
			{
				TradeConfig.setMAX_QUOTES(Integer.parseInt(parm));
			}
			catch (Exception e)
			{
				//>>rjm
				Log.error(
					e, 
					"TradeConfigServlet: minor exception caught", 
					"trying to set max_quotes, error on parsing int " + parm, 
					"reverting to current value " + TradeConfig.getMAX_QUOTES());
				//<<rjm

			} //On error, revert to saved
		}
		currentConfigStr += "\t\t#Trade  Users:\t\t" + TradeConfig.getMAX_USERS()  + "\n";		
		currentConfigStr += "\t\t#Trade Quotes:\t\t" + TradeConfig.getMAX_QUOTES()  + "\n";		
		
                parm = req.getParameter("marketSummaryInterval");
                if ((parm != null) && (parm.length() > 0)) {
                        try {
                                TradeConfig.setMarketSummaryInterval(Integer.parseInt(parm));
                        }
                        catch (Exception e) {
                                Log.error(
                                        e, 
                                        "TradeConfigServlet: minor exception caught", 
                                        "trying to set marketSummaryInterval, error on parsing int " + parm, 
                                        "reverting to current value " + TradeConfig.getMarketSummaryInterval());

                        }
                }
                currentConfigStr += "\t\tMarket Summary Interval:\t\t" + TradeConfig.getMarketSummaryInterval()  + "\n";
		
		parm = req.getParameter("primIterations");
		if ((parm != null) && (parm.length() > 0)) {
			try {
				TradeConfig.setPrimIterations(Integer.parseInt(parm));
			}
			catch (Exception e) {
				Log.error(
					e, 
					"TradeConfigServlet: minor exception caught", 
					"trying to set primIterations, error on parsing int " + parm, 
					"reverting to current value " + TradeConfig.getPrimIterations());

			}
		}
		currentConfigStr += "\t\tPrimitive Iterations:\t\t" + TradeConfig.getPrimIterations()  + "\n";
                
		String enablePublishQuotePriceChange = req.getParameter("EnablePublishQuotePriceChange");
		
		if (enablePublishQuotePriceChange != null)
			TradeConfig.setPublishQuotePriceChange(true);
		else 
			TradeConfig.setPublishQuotePriceChange(false);
		currentConfigStr += "\t\tTradeStreamer MDB Enabled:\t" + TradeConfig.getPublishQuotePriceChange() + "\n";
		
		String enableTrace = req.getParameter("EnableTrace");
		if (enableTrace != null)
			Log.setTrace(true);
		else 
			Log.setTrace(false);			
		String enableActionTrace = req.getParameter("EnableActionTrace");
		if (enableActionTrace != null)
			Log.setActionTrace(true);
		else 
			Log.setActionTrace(false);						

        String enableLongRun = req.getParameter("EnableLongRun");
        
        if (enableLongRun != null)
            TradeConfig.setLongRun(true);
        else 
            TradeConfig.setLongRun(false);
        currentConfigStr += "\t\tLong Run Enabled:\t\t" + TradeConfig.getLongRun()  + "\n";
		
		
		System.out.println(currentConfigStr);

	}

	public void service(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		String action = null;
		String result = "";
		
		resp.setContentType("text/html");
		try
		{
			action = req.getParameter("action");
			if (action == null)
			{
				doConfigDisplay(req, resp, result + "<b><br>Current AriesTrader Configuration:</br></b>");
				return;
			}
			else if (action.equals("updateConfig"))
			{
				doConfigUpdate(req, resp);
				result = "<B><BR>AriesTrader Configuration Updated</BR></B>";
			}
			else if (action.equals("resetTrade"))
			{
				doResetTrade(req, resp, "");
				return;
			}
			else if (action.equals("buildDB"))
			{
				resp.setContentType("text/html");
                new TradeBuildDB(resp.getWriter(), null);
				result = "AriesTrader Database Built - " + TradeConfig.getMAX_USERS() + "users created";
			}
            else if (action.equals("buildDBTables"))
            {
                resp.setContentType("text/html");
                new TradeBuildDB(resp.getWriter(), getServletConfig().getServletContext().getRealPath("/"));
            }
			doConfigDisplay(req, resp, result + "Current AriesTrader Configuration:");
		}
		catch (Exception e)
		{
			Log.error(
				e, 
				"TradeConfigServlet.service(...)", 
				"Exception trying to perform action=" + action);

			resp.sendError(
				500, 
				"TradeConfigServlet.service(...)"
					+ "Exception trying to perform action="
					+ action
                                        + "\nException details: " + e.toString()); 
			
		}
	}
}

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
package org.apache.aries.samples.ariestrader.web.prims;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import org.apache.aries.samples.ariestrader.util.*;

/**
 * 
 * PingServlet2JNDI performs a basic JNDI lookup of a JDBC DataSource
 * 
 */

public class PingServlet2JNDI extends HttpServlet
{

	private static String initTime;
	private static int hitCount;
	
	/**
	 * forwards post requests to the doGet method
	 * Creation date: (11/6/2000 10:52:39 AM)
	 * @param res javax.servlet.http.HttpServletRequest
	 * @param res2 javax.servlet.http.HttpServletResponse
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		doGet(req, res);
	}
	/**
	* this is the main method of the servlet that will service all get requests.
	* @param request HttpServletRequest
	* @param responce HttpServletResponce
	**/
	public void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		res.setContentType("text/html");
		java.io.PrintWriter out = res.getWriter();

		StringBuffer output = new StringBuffer(100);

		try
			{

			int iter = TradeConfig.getPrimIterations();
			for (int ii = 0; ii < iter; ii++) {
                            DataSource dataSource = (DataSource) ServiceUtilities.getOSGIService(DataSource.class.getName(),TradeConfig.OSGI_DS_NAME_FILTER);
			}			


			output.append(
				"<html><head><title>Ping JNDI -- lookup of JDBC DataSource</title></head>"
					+ "<body><HR><FONT size=\"+2\" color=\"#000066\">Ping JNDI -- lookup of JDBC DataSource</FONT><HR><FONT size=\"-1\" color=\"#000066\">Init time : "
					+ initTime);
			hitCount++;
			output.append("</FONT><BR>Hit Count: " + hitCount);
			output.append("<HR></body></html>");
			out.println(output.toString());
		}
		catch (Exception e)
		{
			Log.error(e, "PingServlet2JNDI -- error look up of a JDBC DataSource");
			res.sendError(500, "PingServlet2JNDI Exception caught: " + e.toString());
		}

	}
	/** 
	 * returns a string of information about the servlet
	 * @return info String: contains info about the servlet
	 **/
	public String getServletInfo()
	{
		return "Basic JNDI look up of a JDBC DataSource";
	}
	/**
	* called when the class is loaded to initialize the servlet
	* @param config ServletConfig:
	**/
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		hitCount = 0;
		initTime = new java.util.Date().toString();
	}
}
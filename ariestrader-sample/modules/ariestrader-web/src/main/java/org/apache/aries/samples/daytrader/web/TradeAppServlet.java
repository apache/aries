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
package org.apache.aries.samples.daytrader.web;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.aries.samples.daytrader.api.TradeServicesManager;
import org.apache.aries.samples.daytrader.api.TradeServiceUtilities;
import org.apache.aries.samples.daytrader.api.TradeServices;
import org.apache.aries.samples.daytrader.util.*;

import java.io.IOException;


/**
 * 
 * TradeAppServlet provides the standard web interface to Trade and can be
 * accessed with the Go Trade! link. Driving benchmark load using this interface
 * requires a sophisticated web load generator that is capable of filling HTML
 * forms and posting dynamic data.
 */

public class TradeAppServlet extends HttpServlet {

    private static TradeServicesManager tradeServicesManager = null;

    /**
     * Servlet initialization method.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        java.util.Enumeration en = config.getInitParameterNames();
        while (en.hasMoreElements()) {
            String parm = (String) en.nextElement();
            String value = config.getInitParameter(parm);
            TradeConfig.setConfigParam(parm, value);
        }
    }

    /**
     * Returns a string that contains information about TradeScenarioServlet
     * 
     * @return The servlet information
     */
    public java.lang.String getServletInfo() {
        return "TradeAppServlet provides the standard web interface to Trade";
    }

    /**
     * Process incoming HTTP GET requests
     * 
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    public void doGet(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
            throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Process incoming HTTP POST requests
     * 
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    public void doPost(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
            throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Main service method for TradeAppServlet
     * 
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    public void performTask(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = null;
        String userID = null;
        // String to create full dispatch path to TradeAppServlet w/ request
        // Parameters
        String dispPath = null; // Dispatch Path to TradeAppServlet

        resp.setContentType("text/html");

        if (tradeServicesManager == null) {
            tradeServicesManager = TradeServiceUtilities.getTradeServicesManager();
        }
        TradeServices tradeServices = tradeServicesManager.getTradeServices();

        TradeServletAction tsAction = new TradeServletAction(tradeServices);

        // Dyna - need status string - prepended to output
        action = req.getParameter("action");

        ServletContext ctx = getServletConfig().getServletContext();

        if (action == null) {
            tsAction.doWelcome(ctx, req, resp, "");
            return;
        } else if (action.equals("login")) {
            userID = req.getParameter("uid");
            String passwd = req.getParameter("passwd");
            String inScenario = req.getParameter("inScenario");
            tsAction.doLogin(ctx, req, resp, userID, passwd);
            return;
        } else if (action.equals("register")) {
            userID = req.getParameter("user id");
            String passwd = req.getParameter("passwd");
            String cpasswd = req.getParameter("confirm passwd");
            String fullname = req.getParameter("Full Name");
            String ccn = req.getParameter("Credit Card Number");
            String money = req.getParameter("money");
            String email = req.getParameter("email");
            String smail = req.getParameter("snail mail");
            tsAction.doRegister(ctx, req, resp, userID, passwd, cpasswd,
                    fullname, ccn, money, email, smail);
            return;
        }

        // The rest of the operations require the user to be logged in -
        // Get the Session and validate the user.
        HttpSession session = req.getSession();
        userID = (String) session.getAttribute("uidBean");

        if (userID == null) {
            System.out
                    .println("TradeAppServlet service error: User Not Logged in");
            tsAction.doWelcome(ctx, req, resp, "User Not Logged in");
            return;
        }
        if (action.equals("quotes")) {
            String symbols = req.getParameter("symbols");
            tsAction.doQuotes(ctx, req, resp, userID, symbols);
        } else if (action.equals("buy")) {
            String symbol = req.getParameter("symbol");
            String quantity = req.getParameter("quantity");
            tsAction.doBuy(ctx, req, resp, userID, symbol, quantity);
        } else if (action.equals("sell")) {
            int holdingID = Integer.parseInt(req.getParameter("holdingID"));
            tsAction.doSell(ctx, req, resp, userID, new Integer(holdingID));
        } else if (action.equals("portfolio")
                || action.equals("portfolioNoEdge")) {
            tsAction.doPortfolio(ctx, req, resp, userID, "Portfolio as of "
                    + new java.util.Date());
        } else if (action.equals("logout")) {
            tsAction.doLogout(ctx, req, resp, userID);
        } else if (action.equals("home")) {
            tsAction.doHome(ctx, req, resp, userID, "Ready to Trade");
        } else if (action.equals("account")) {
            tsAction.doAccount(ctx, req, resp, userID, "");
        } else if (action.equals("update_profile")) {
            String password = req.getParameter("password");
            String cpassword = req.getParameter("cpassword");
            String fullName = req.getParameter("fullname");
            String address = req.getParameter("address");
            String creditcard = req.getParameter("creditcard");
            String email = req.getParameter("email");
            tsAction.doAccountUpdate(ctx, req, resp, userID,
                    password == null ? "" : password.trim(),
                    cpassword == null ? "" : cpassword.trim(),
                    fullName == null ? "" : fullName.trim(),
                    address == null ? "" : address.trim(),
                    creditcard == null ? "" : creditcard.trim(),
                    email == null ? "" : email.trim());
        } else {
            System.out.println("TradeAppServlet: Invalid Action=" + action);
            tsAction.doWelcome(ctx, req, resp,
                    "TradeAppServlet: Invalid Action" + action);
        }
    }

    private void sendRedirect(HttpServletResponse resp, String page)
            throws ServletException, IOException {
        resp.sendRedirect(resp.encodeRedirectURL(page));
    }

    // URL Path Prefix for dispatching to TradeAppServlet
    private final static String tasPathPrefix = "/app?action=";

}
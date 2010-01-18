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
package org.apache.geronimo.samples.daytrader.web;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.geronimo.samples.daytrader.api.TradeServicesManager;
import org.apache.geronimo.samples.daytrader.api.TradeServiceUtilities;
import org.apache.geronimo.samples.daytrader.api.TradeServices;
import org.apache.geronimo.samples.daytrader.util.*;

public class OrdersAlertFilter implements Filter {

    private static TradeServicesManager tradeServicesManager = null;

    /**
     * Constructor for CompletedOrdersAlertFilter
     */
    public OrdersAlertFilter() {
        super();
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    private FilterConfig filterConfig = null;
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(
                        ServletRequest req,
                        ServletResponse resp,
                        FilterChain chain)
    throws IOException, ServletException {
        if (filterConfig == null)
            return;

        if (tradeServicesManager == null) {
            tradeServicesManager = TradeServiceUtilities.getTradeServicesManager();
        }
        TradeServices tradeServices = tradeServicesManager.getTradeServices();

        try {
            String action = req.getParameter("action");
            if ( action != null ) {
                action = action.trim();
                if ( (action.length() > 0) && (!action.equals("logout")) ) {
                    String userID;
                    if ( action.equals("login") )
                        userID = req.getParameter("uid");
                    else
                        userID = (String) ((HttpServletRequest) req).getSession().getAttribute("uidBean");
                    if ( (userID != null) && (userID.trim().length()>0) ) {

                        java.util.Collection closedOrders = tradeServices.getClosedOrders(userID);
                        if ( (closedOrders!=null) && (closedOrders.size() > 0) ) {
                            req.setAttribute("closedOrders", closedOrders);
                        }
                        if (Log.doTrace()) {
                            Log.printCollection("OrdersAlertFilter: userID="+userID+" closedOrders=", closedOrders);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Log.error(e, "OrdersAlertFilter - Error checking for closedOrders");
        }

        ServletContext sc = filterConfig.getServletContext();
        //String xyz = (String) sc.getAttribute("hitCounter");
        chain.doFilter(req, resp/*wrapper*/);        

    }

    /**
     * @see Filter#destroy()
     */
    public void destroy() {
        this.filterConfig = null;   
    }

}


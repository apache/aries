<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<%@ page
	import="java.math.BigDecimal, org.apache.aries.samples.ariestrader.api.*, org.apache.aries.samples.ariestrader.api.persistence.*, org.apache.aries.samples.ariestrader.util.*"
	session="true" isThreadSafe="true" isErrorPage="false"%>
<% 
    String symbol = request.getParameter("symbol");
    TradeServicesManager tradeServicesManager = null;

    if (tradeServicesManager == null) {
        tradeServicesManager = TradeServiceUtilities.getTradeServicesManager();
    }
    TradeServices tradeServices = tradeServicesManager.getTradeServices();

	try { 
		QuoteDataBean quoteData = tradeServices.getQuote(symbol);

 %>
<TR align="center" bgcolor="#fafcb6">
	<TD><%= FinancialUtils.printQuoteLink(quoteData.getSymbol()) %></TD>
	<TD><%= quoteData.getCompanyName()%></TD>
	<TD><%= quoteData.getVolume()%></TD>
	<TD><%= quoteData.getLow() + " - " + quoteData.getHigh()%></TD>
	<TD nowrap><%= quoteData.getOpen()%></TD>
	<TD>$ <%= quoteData.getPrice()%></TD>
	<TD><%= FinancialUtils.printGainHTML(new BigDecimal(quoteData.getChange())) %>
	<%= FinancialUtils.printGainPercentHTML( FinancialUtils.computeGainPercent(quoteData.getPrice(), quoteData.getOpen())) %></TD>
	<TD>
	<FORM action=""><INPUT type="submit" name="action" value="buy"><INPUT
		type="hidden" name="symbol" value="<%= quoteData.getSymbol()%>"><INPUT
		size="4" type="text" name="quantity" value="100"></FORM>
	</TD>
</TR>

<%
	}
	catch (Exception e)
	{
		Log.error("displayQuote.jsp  exception", e);
	}
%>

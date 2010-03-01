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
	import="java.util.Collection, java.util.Iterator, org.apache.aries.samples.ariestrader.api.*, org.apache.aries.samples.ariestrader.persistence.api.*, org.apache.aries.samples.ariestrader.util.*"
	session="true" isThreadSafe="true" isErrorPage="false"%>

<%
TradeServicesManager tradeServicesManager = null;

if (tradeServicesManager == null) {
    tradeServicesManager = TradeServiceUtilities.getTradeServicesManager();
}

MarketSummaryDataBean marketSummaryData = tradeServicesManager.getMarketSummary();
%>
<TABLE border="1" bgcolor="#ffffff" width="100%"
	style="font-size: smaller">
	<TBODY>
		<TR>
			<TD colspan="2" bgcolor="#000000" align="center" height="15"><FONT
				color="#ffffff"><B>Market Summary<BR>
			<%= marketSummaryData.getSummaryDate() %></B></FONT></TD>
		</TR>
		<TR>
			<TD align="right" bgcolor="#fafcb6" height="47" width="100"><A
				href="docs/glossary.html">AriesTrader Stock Index (TSIA)</A></TD>
			<TD align="center" valign="middle" bgcolor="#ffffff" height="47"
				width="141"><%= marketSummaryData.getTSIA() %> <%= FinancialUtils.printGainPercentHTML(marketSummaryData.getGainPercent()) %></TD>
		</TR>
		<TR>
			<TD align="right" bgcolor="#fafcb6"><A href="docs/glossary.html">Trading
			Volume</A></TD>
			<TD align="center" valign="middle"><%= marketSummaryData.getVolume() %></TD>
		</TR>
		<TR>
			<TD align="right" bgcolor="#fafcb6" width="74"><A
				href="docs/glossary.html">Top Gainers</A></TD>
			<TD bgcolor="#ffffff">
			<TABLE width="100%" border="1" height="100%"
				style="font-size: smaller">
				<TBODY>
					<TR align="center">
						<TD><A href="docs/glossary.html">symbol</A></TD>
						<TD><A href="docs/glossary.html">price</A></TD>
						<TD><A href="docs/glossary.html">change</A></TD>
					</TR>
					<%                              
Collection topGainers = marketSummaryData.getTopGainers();
Iterator gainers = topGainers.iterator();
int count=0;
while (gainers.hasNext() && (count++ < 5))
{
	QuoteDataBean quoteData = (QuoteDataBean) gainers.next();
%>
					<TR align="center">
						<TD width="24"><%= FinancialUtils.printQuoteLink(quoteData.getSymbol()) %>
						</TD>
						<TD><%= quoteData.getPrice() %></TD>
						<TD width="52" nowrap><%= FinancialUtils.printGainHTML(quoteData.getPrice().subtract(quoteData.getOpen())) /*FinancialUtils.printGainPercentHTML(FinancialUtils.computeGainPercent(quoteData.getPrice(), quoteData.getOpen()))*/ %></TD>
					</TR>
					<%
}
%>
				</TBODY>
			</TABLE>
			</TD>
		</TR>
		<TR>
			<TD align="right" height="55" bgcolor="#fafcb6" width="74"><A
				href="docs/glossary.html">Top Losers</A></TD>
			<TD height="55" bgcolor="#ffffff" width="141">
			<TABLE width="100%" border="1" height="100%"
				style="font-size: smaller">
				<TBODY>
					<TR align="center">
						<TD><A href="docs/glossary.html">symbol</A></TD>
						<TD><A href="docs/glossary.html">price</A></TD>
						<TD><A href="docs/glossary.html">change</A></TD>
					</TR>
					<%
Collection topLosers = marketSummaryData.getTopLosers();
Iterator losers  = topLosers.iterator();
count=0;
while (losers.hasNext() && (count++ < 5))

{
	QuoteDataBean quoteData = (QuoteDataBean) losers.next();
%>
					<TR align="center">
						<TD width="24" nowrap><%= FinancialUtils.printQuoteLink(quoteData.getSymbol()) %>
						</TD>
						<TD><%= quoteData.getPrice() %></TD>
						<TD width="52" nowrap><%= FinancialUtils.printGainHTML(quoteData.getPrice().subtract(quoteData.getOpen())) /* FinancialUtils.printGainPercentHTML(FinancialUtils.computeGainPercent(quoteData.getPrice(), quoteData.getOpen())) */%></TD>
					</TR>
					<%
}
%>
				</TBODY>
			</TABLE>
			</TD>
		</TR>
	</TBODY>
</TABLE>

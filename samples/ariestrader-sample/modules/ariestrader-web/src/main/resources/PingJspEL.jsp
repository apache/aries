<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
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
<html>
<head>
	<title>PingJspEL</title>
</head>
<body>
<%@ page import="org.apache.aries.samples.ariestrader.util.*,org.apache.aries.samples.ariestrader.api.*,org.apache.aries.samples.ariestrader.persistence.api.*" session="false" %>

<%!
int hitCount = 0;
String initTime = new java.util.Date().toString();
%>
 
<%
// setup some variables to work with later
int someint1 = TradeConfig.rndInt(100) + 1;
pageContext.setAttribute("someint1", new Integer(someint1));
int someint2 = TradeConfig.rndInt(100) + 1;
pageContext.setAttribute("someint2", new Integer(someint2));
float somefloat1 = TradeConfig.rndFloat(100) + 1.0f;
pageContext.setAttribute("somefloat1", new Float(somefloat1));
float somefloat2 = TradeConfig.rndFloat(100) + 1.0f;
pageContext.setAttribute("somefloat2", new Float(somefloat2));

TradeServicesManager tradeServicesManager = null;

if (tradeServicesManager == null) {
    tradeServicesManager = TradeServiceUtilities.getTradeServicesManager();
}
TradeServices tradeServices = tradeServicesManager.getTradeServices();

QuoteDataBean quoteData0=null, quoteData1=null, quoteData2=null, quoteData3=null;

try { 
	quoteData0 = tradeServices.getQuote(TradeConfig.rndSymbol());
	quoteData1 = tradeServices.getQuote(TradeConfig.rndSymbol());
	quoteData2 = tradeServices.getQuote(TradeConfig.rndSymbol());
	quoteData3 = tradeServices.getQuote(TradeConfig.rndSymbol());
}
catch (Exception e)
{
    Log.error("displayQuote.jsp  exception", e);
}

pageContext.setAttribute("quoteData0", quoteData0);
pageContext.setAttribute("quoteData1", quoteData1);
pageContext.setAttribute("quoteData2", quoteData2);
pageContext.setAttribute("quoteData3", quoteData3);

QuoteDataBean quoteData[] = new QuoteDataBean[4];
quoteData[0] = quoteData0;
quoteData[1] = quoteData1;
quoteData[2] = quoteData2;
quoteData[3] = quoteData3;

pageContext.setAttribute("quoteData", quoteData);
%>
  
<HR>
<BR>
  <FONT size="+2" color="#000066">PING JSP EL:<BR></FONT><FONT size="+1" color="#000066">Init time: <%= initTime %></FONT>
  <P>
    <B>Hit Count: <%= hitCount++ %></B>
   </P>
<HR>

<P>

someint1 = <%= someint1 %><br/>
someint2 = <%= someint2 %><br/>
somefloat1 = <%= somefloat1 %><br/>
somefloat2 = <%= somefloat2 %><br/>

<P>

<HR>

<table border="1">
	<thead>
		<th>EL Type</th>
		<th>EL Expressions</th>
		<th>Result</th>
	</thead>
	<tr>
		<td>Integer Arithmetic</td>
		<td>\${someint1 + someint2 - someint1 * someint2 mod someint1}</td>
		<td>${someint1 + someint2 - someint1 * someint2 mod someint1}</td>
	</tr>
	<tr>
		<td>Floating Point Arithmetic</td>
		<td>\${somefloat1 + somefloat2 - somefloat1 * somefloat2 / somefloat1}</td>
		<td>${somefloat1 + somefloat2 - somefloat1 * somefloat2 / somefloat1}</td>
	</tr>
	<tr>
		<td>Logical Operations</td>
		<td>\${(someint1 < someint2) && (someint1 <= someint2) || (someint1 == someint2) && !Boolean.FALSE}</td>
		<td>${(someint1 < someint2) && (someint1 <= someint2) || (someint1 == someint2) && !Boolean.FALSE}</td>
	</tr>
	<tr>
		<td>Indexing Operations</td>
		<td>
			\${quoteData1.symbol}<br/>
			\${quoteData[2].symbol}<br/>
			\${quoteData3["symbol"]}<br/>
			\${header["host"]}<br/>
			\${header.host}<br/>
		</td>
		<td>
			${quoteData1.symbol}<br/>
			${quoteData[2].symbol}<br/>
			${quoteData3["symbol"]}<br/>
			${header["host"]}<br/>
			${header.host}
		</td>
	</tr>
	<tr>
		<td>Variable Scope Tests</td>
		<td>
			\${(quoteData0 == null) ? "null" : quoteData0}<br/>
			\${(noSuchVariableAtAnyScope == null) ? "null" : noSuchVariableAtAnyScope}
		</td>
		<td>
			${(quoteData0 == null) ? "null" : quoteData0}<br/>
			${(noSuchVariableAtAnyScope == null) ? "null" : noSuchVariableAtAnyScope}
		</td>
	</tr>
</table>
</body>
</html>

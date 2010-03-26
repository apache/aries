<%--

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<HTML>
<HEAD>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<META http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<META name="GENERATOR" content="IBM Software Development Platform">
<TITLE>helloWorld.jsp</TITLE>
</HEAD>
<BODY>
<P>
Hello world - from JSP! With JMS Import!
xxx <%@ page import="javax.jms.*, javax.mystuff.SomeClass" %> <%@ page import = "javax.transaction.package" %>xxx
<%@ page import="a.b.AClass" %>
<%@ page import="a.b.AnotherClassInADotB" %>
<%@ page import="java.util.List" %>

<BR/><BR/>
Here is a random number <%= Math.random() %>
</P>

<% 
  JMSException jmsx = new JMSException ("reason");
%>
Here's an exception: <%= jmsx.toString() %>

</BODY>
</HTML>

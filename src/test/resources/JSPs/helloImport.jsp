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

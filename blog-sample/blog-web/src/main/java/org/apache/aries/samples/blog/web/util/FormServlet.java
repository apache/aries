/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.blog.web.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;



public abstract class FormServlet extends HttpServlet
{
  private static final long serialVersionUID = -1019904995493434571L;
  public static final String ERROR_MESSAGES_ID = "errorMessages";
  private String id;
  
  public static void addError(HttpServletRequest req, String error)
  {
    HttpSession session = req.getSession();
    if (session != null) {
      @SuppressWarnings("unchecked")
      List<String> errors = (List<String>) session.getAttribute(ERROR_MESSAGES_ID); 
  
      if (errors == null) {
        errors = new ArrayList<String>();
        session.setAttribute(ERROR_MESSAGES_ID, errors);
      }
      
      errors.add(error);
    }
  }
  
  public static void storeParam(HttpServletRequest req, String id, String param, String value)
  {
    HttpSession session = req.getSession();
    if (session != null)
      session.setAttribute(id + ":" + param, value);
  }
  
  protected FormServlet(String id)
  {
    this.id = id;
  }
  
  protected abstract void writeCustomHeaderContent(HttpServletRequest req, PrintWriter out);
  protected abstract void writeForm(HttpServletRequest req, PrintWriter out) throws IOException;
  protected abstract String getPageTitle(HttpServletRequest req) throws IOException;
  
  protected String retrieveOrEmpty(HttpServletRequest req, String param)
  {
    HttpSession session = req.getSession();
    String value = "";
    if (session != null) { 
      value = (String) session.getAttribute(id+":"+param);
      if (value == null) {
        value = "";
      }
    }
    
    return value;
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,IOException
  {
    PrintWriter out = resp.getWriter();

    HTMLOutput.writeHTMLHeaderPartOne(out, getPageTitle(req));
    writeCustomHeaderContent(req, out);
    
    List<String> errors = null;
    if (req.getSession() != null)
      errors = (List<String>) req.getSession().getAttribute(ERROR_MESSAGES_ID);
    
    if (errors == null) {    
      try {
		HTMLOutput.writeHTMLHeaderPartTwo(out);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    } else {
      try {
		HTMLOutput.writeHTMLHeaderPartTwo(out, errors);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    }
    
    writeForm(req, out);
    
    HTMLOutput.writeHTMLFooter(out);
    cleanupSession(req);
  }
  
  private void cleanupSession(HttpServletRequest req) 
  {
    HttpSession session = req.getSession();

    if (session != null) {
      @SuppressWarnings("unchecked")
      Enumeration<String> names = session.getAttributeNames();
      
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        if (name.startsWith(id+":"))
          session.removeAttribute(name);
      }
      
      session.removeAttribute("errorMessages");
    }
  }  
}

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
package org.apache.aries.samples.blog.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.api.BloggingService;
import org.apache.aries.samples.blog.web.util.FormServlet;
import org.apache.aries.samples.blog.web.util.HTMLOutput;
import org.apache.aries.samples.blog.web.util.JNDIHelper;


public class EditAuthorForm extends FormServlet
{
  private static final long serialVersionUID = 4996935653835900015L;
  public static final String ID = "author";

  public EditAuthorForm()
  {
    super(ID);
  }
  
  @Override
  protected void writeCustomHeaderContent(HttpServletRequest req, PrintWriter out)
  {
    HTMLOutput.writeDojoUses(out, "dijit.form.TextBox", "dijit.form.DateTextBox", "dijit.form.Textarea");
  }

  @Override
  protected String getPageTitle(HttpServletRequest req) throws IOException
  {
    String pageTitle = "Create Author";
    
    BloggingService service = JNDIHelper.getBloggingService();
    String email = getEmail(req);
    
    if (email != null && !!!"".equals(email)) {
      BlogAuthor author = service.getBlogAuthor(email);
      if (author != null) {
        pageTitle = "Update " + author.getName() + "'s profile";
      }
    }
    
    return pageTitle;
  }

  private String getEmail(HttpServletRequest req)
  {
    String email = retrieveOrEmpty(req, "email");
    if ("".equals(email)) {
      email = req.getParameter("email");
    }
    return checkEmail(email);
  }
  
  @Override
  protected void writeForm(HttpServletRequest req, PrintWriter out) throws IOException
  {
    String name = retrieveOrEmpty(req, "name");
    String nickName = retrieveOrEmpty(req, "nickName");
    String bio = retrieveOrEmpty(req, "bio");
    String dob = retrieveOrEmpty(req, "dob");
    String email = getEmail(req);
    
    BloggingService service = JNDIHelper.getBloggingService();
    
    if (email != null && !!!"".equals(email)) {
      BlogAuthor author = service.getBlogAuthor(email);
      
      if ("".equals(name))
        name = author.getFullName();
      if ("".equals(nickName))
        nickName = author.getName();
      if ("".equals(bio))
        bio = author.getBio();
      if ("".equals(dob))
        dob = author.getDateOfBirth();
    } else {
      email = "";
    }
    
    out.println("<form method=\"get\" action=\"EditAuthor\">");
    
    out.print("<div class=\"textEntry\"><label>Name <input dojoType=\"dijit.form.TextBox\" type=\"text\" name=\"name\" value=\"");
    out.print(name);
    out.println("\"/></label></div>");
    out.print("<div class=\"textEntry\"><label>Nickname <input dojoType=\"dijit.form.TextBox\" type=\"text\" name=\"nickName\" value=\"");
    out.print(nickName);
    out.println("\"/></label></div>");
    out.print("<div class=\"textEntry\"><label>Email <input dojoType=\"dijit.form.TextBox\" type=\"text\" name=\"email\" value=\"");
    out.print(email);
    out.println("\"/></label></div>");
    out.print("<div class=\"textEntry\"><label>Date of Birth <input dojoType=\"dijit.form.DateTextBox\" type=\"text\" name=\"dob\" required=\"true\" value=\"");
    out.print(dob);
    out.println("\"/></label></div>");
    out.print("<div class=\"textEntry\"><label>Bio <textarea dojoType=\"dijit.form.Textarea\" style=\"width:300px\" name=\"bio\">");
    out.print(bio);
    out.println("</textarea></label></div>");

    out.println("<input class=\"submit\" type=\"submit\" value=\"Submit\" name=\"Submit\"/>");
    
    out.println("</form>");
  }
  
  private String checkEmail(String parameter)
  {
    if (parameter != null && parameter.matches("^(?:[a-zA-Z0-9_'^&amp;/+-])+(?:\\.(?:[a-zA-Z0-9_'^&amp;/+-])+)*@(?:(?:\\[?(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))\\.){3}(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\]?)|(?:[a-zA-Z0-9-]+\\.)+(?:[a-zA-Z]){2,}\\.?)$")) return parameter;
    return null;
  }
}
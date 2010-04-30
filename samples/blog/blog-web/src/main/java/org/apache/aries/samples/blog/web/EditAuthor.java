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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.aries.samples.blog.api.BloggingService;
import org.apache.aries.samples.blog.web.util.FormServlet;
import org.apache.aries.samples.blog.web.util.FormatChecker;
import org.apache.aries.samples.blog.web.util.JNDIHelper;

public class EditAuthor extends HttpServlet
{
  private static final long serialVersionUID = -8881545878284864977L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    doPost(req, resp);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException
  {
    // This method will update or create an author depending on the
    // existence of the author in the database.
    
    // The authors email address is the key in the database, thus if
    // the email address is not in the database we create this as a 
    // new author.
    
    String email = req.getParameter("email");
    String nickName = req.getParameter("nickName");
    String name = req.getParameter("name");
    String bio = req.getParameter("bio");
    String dob = req.getParameter("dob");
    
    if (email == null || email.equals("")) {
      storeParam(req, "email", email);
      storeParam(req, "nickName", nickName);
      storeParam(req, "name", name);
      storeParam(req, "bio", bio);
      storeParam(req, "dob", dob);
      
      FormServlet.addError(req, "The email field is required.");
      resp.sendRedirect("EditAuthorForm");
      
    }else if (!FormatChecker.isValidEmail(email)) {
    	storeParam(req, "email", email);
        storeParam(req, "nickName", nickName);
        storeParam(req, "name", name);
        storeParam(req, "bio", bio);
        storeParam(req, "dob", dob);
        
    	FormServlet.addError(req, "The email field is not properly formatted");
        resp.sendRedirect("EditAuthorForm");	
    } else {
      BloggingService service = JNDIHelper.getBloggingService();

      if (service.getBlogAuthor(email) != null) {
        // do an update
        service.updateBlogAuthor(email, nickName, name, bio, dob);
      } else {
        // do a create
        service.createBlogAuthor(email, nickName, name, bio, dob);
      } 
      RequestDispatcher dispatch = getServletContext().getRequestDispatcher("/ViewAuthor");
      dispatch.forward(req, resp);
    }
  }
  
    private void storeParam(HttpServletRequest req, String param, String value) 
  {
    FormServlet.storeParam(req, EditAuthorForm.ID, param, value);
  }
}
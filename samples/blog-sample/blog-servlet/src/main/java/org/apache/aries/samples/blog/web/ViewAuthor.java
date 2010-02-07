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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.api.BloggingService;
import org.apache.aries.samples.blog.web.util.HTMLOutput;
import org.apache.aries.samples.blog.web.util.JNDIHelper;



public class ViewAuthor extends HttpServlet
{
  private static final long serialVersionUID = 3020369464892668248L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException
  {
    String email = req.getParameter("email");
    
    if (email == null || "".equals(email)) {
      // TODO dispatch to another page
    } else {
      PrintWriter out = resp.getWriter();
      
      BloggingService service = JNDIHelper.getBloggingService();
      
      BlogAuthor author = service.getBlogAuthor(email);
      
      HTMLOutput.writeHTMLHeaderPartOne(out, author.getName());
      
      HTMLOutput.writeHTMLHeaderPartTwo(out);

      out.println("<h2 class=\"header\">Name</h2>");
      out.print("<div class=\"text\">");
      out.print(author.getFullName());
      out.println("</div>");
      out.println("<h2 class=\"header\">Nick Name</h2>");
      out.print("<div class=\"text\">");
      out.print(author.getName());
      out.println("</div>");
      out.println("<h2 class=\"header\">Email</h2>");
      out.print("<div class=\"text\">");
      out.print(author.getEmailAddress());
      out.println("</div>");
      out.println("<h2 class=\"header\">DOB</h2>");
      out.print("<div class=\"text\">");
      out.print(author.getDateOfBirth());
      out.println("</div>");
      out.println("<h2 class=\"header\">Bio</h2>");
      out.print("<div class=\"text\">");
      out.print(author.getBio());
      out.println("</div>");
      
      out.print("<a href=\"EditAuthorForm?email=");
      out.print(author.getEmailAddress());
      out.println("\">Edit Author Information</a>");
      
      HTMLOutput.writeHTMLFooter(out);
    }
  }
}
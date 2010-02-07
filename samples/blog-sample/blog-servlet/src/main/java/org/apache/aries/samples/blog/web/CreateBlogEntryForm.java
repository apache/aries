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

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.aries.samples.blog.web.util.FormServlet;
import org.apache.aries.samples.blog.web.util.HTMLOutput;


public class CreateBlogEntryForm extends FormServlet
{
  private static final long serialVersionUID = -6484228320837122235L;
  public static final String ID = "post";
  
  public CreateBlogEntryForm()
  {
    super(ID);
  }
  
  @Override
  protected String getPageTitle(HttpServletRequest req)
  { 
    return "Create Blog Post";
  }

  @Override
  protected void writeForm(HttpServletRequest req, PrintWriter out)
  {
    String email = retrieveOrEmpty(req, "email");
    String title = retrieveOrEmpty(req, "title");
    String text = retrieveOrEmpty(req, "text");
    String tags = retrieveOrEmpty(req, "tags");
    
    out.println("<form name=\"createPost\" method=\"post\" action=\"CreateBlogEntry\">");

    out.println("<div class=\"textEntry\"><label>Title <input dojoType=\"dijit.form.TextBox\" type=\"text\" name=\"title\" value=\"" + title + "\"/></label></div>");
    out.println("<div class=\"textEntry\"><textarea dojoType=\"dijit.Editor\" id=\"text\" name=\"text\">" + text + "</textarea></div>");
    out.println("<div class=\"textEntry\"><label>Email <input dojoType=\"dijit.form.TextBox\" type=\"text\" name=\"email\" value=\"" + email + "\"/></label></div>");
    out.println("<div class=\"textEntry\"><label>Tags &nbsp;<input dojoType=\"dijit.form.TextBox\" type=\"text\" name=\"tags\" value=\"" + tags + "\"/></label></div>");
    
    out.println("<input type=\"hidden\" name=\"text\" id=\"text\" value=\"\"/>");
    out.println("<input class=\"submit\" type=\"submit\" value=\"Submit\" name=\"Submit\" onclick=\"storeBlogContent();return true;\"/>");
    out.println("</form>");
  }
  
  @Override
  protected void writeCustomHeaderContent(HttpServletRequest req, PrintWriter out)
  {
    HTMLOutput.writeDojoUses(out, "dojo.parser", "dijit.dijit", "dijit.Editor", "dijit.form.TextBox"); 
    
    
    out.println("<script type=\"text/javascript\">");
    out.println("  function storeBlogContent() {");
    out.println("    var textBox = dijit.byId('textArea');");
    out.println("    var textArea = dojo.byId('text');");
    out.println("    textArea.value = textBox.getValue();");
    out.println("  }");
    out.println("</script>");
  }
}
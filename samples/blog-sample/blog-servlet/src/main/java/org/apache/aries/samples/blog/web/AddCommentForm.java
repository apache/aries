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
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.aries.samples.blog.web.util.HTMLOutput;

public class AddCommentForm extends HttpServlet{
	private static final long serialVersionUID = 4989805137759774598L;
	public static final String ERROR_MESSAGES_ID = "commentErrorMessages";
	public static final String ID = "comment";


	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		PrintWriter out = resp.getWriter();
		
		String postId = checkPostId(req.getParameter("postId"));

		// if we have a valid postId, display the add comment page
		if (postId != null) {
			HTMLOutput.writeHTMLHeaderPartOne(out, "Add Comment");
			HTMLOutput.writeDojoUses(out, "dojo.parser", "dijit.dijit",
					"dijit.Editor", "dijit.form.TextBox");

			out.println("<script type=\"text/javascript\">");
			out.println("function storeCommentContent() {");
			out.println("var textBox = dijit.byId('textArea');");
			out.println("var textArea = dojo.byId('text');");
			out.println("textArea.value = textBox.getValue();");
			out.println("}");
			out.println("</script>");

			HTMLOutput.writeHTMLHeaderPartTwo(out);
			
			List<String> errors = null;
			if (req.getSession() != null)
				errors = (List<String>) req.getSession().getAttribute(
						ERROR_MESSAGES_ID);

			if (errors != null) {
				out.println("\t\t\t<div id=\"errorMessages\">");
				for (String msg : errors) {
					out.println("\t\t\t\t<div class=\"errorMessage\">" + msg
							+ "</div>");
				}

				out.println("\t\t\t</div>");
				req.getSession().removeAttribute("commentErrorMessages");
			}

			out
					.println("<form name=\"createComment\" method=\"get\" action=\"AddComment\">");
			out
					.println("<div class=\"textEntry\"><textarea dojoType=\"dijit.Editor\" id=\"textArea\" name=\"textArea\"></textarea></div>");
			out
					.println("<div class=\"textEntry\"><label>Email <input dojoType=\"dijit.form.TextBox\" type=\"text\" name=\"email\" /></label></div>");
			out
					.println("<input type=\"hidden\" name=\"text\" id=\"text\" value=\"\"/>");
			out.print("<input type=\"hidden\" name=\"postId\" value=\"");
			out.print(postId);
			out.println("\"/>");
			out
					.println("<input class=\"submit\" type=\"submit\" value=\"Submit\" name=\"Submit\" onclick=\"storeCommentContent()\"/>");
			out.println("</form>");

			HTMLOutput.writeHTMLFooter(out);
			
		} else {
			// otherwise show the blog
			RequestDispatcher dispatch = getServletContext()
					.getRequestDispatcher("ViewBlog");
			dispatch.forward(req, resp);
		}
	}

	private String checkPostId(String parameter) {
		if (parameter != null && parameter.matches("^\\d*$"))
			return parameter;
		return null;
	}
}

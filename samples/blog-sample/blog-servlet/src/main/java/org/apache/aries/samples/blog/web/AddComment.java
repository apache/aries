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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.aries.samples.blog.api.BloggingService;
import org.apache.aries.samples.blog.web.util.JNDIHelper;

public class AddComment extends HttpServlet {
	private static final long serialVersionUID = -920234218060948564L;
	public static final String ERROR_MESSAGES_ID = "commentErrorMessages";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// email address of the comment's author
		String email = req.getParameter("email");
		// the id of the blog entry to which this comment is associated
		long postId = Long.parseLong(req.getParameter("postId"));
		// the text of the comment
		String text = req.getParameter("text");

		BloggingService service = JNDIHelper.getBloggingService();

		// retrieve the blog entry and create the associated comment

		if (service.getBlogAuthor(email) != null) {
			service.createBlogComment(text, email, postId);
			resp.sendRedirect("ViewBlog");
		} else {

			if (email.equals(""))
				addError(req, "The email field is required.");
			else
				addError(req, "The email filed is not valid.");
			resp.sendRedirect("AddCommentForm?postId=" + postId);
		}
	}

	public static void addError(HttpServletRequest req, String error) {
		HttpSession session = req.getSession();
		if (session != null) {
			@SuppressWarnings("unchecked")
			List<String> errors = (List<String>) session
					.getAttribute(ERROR_MESSAGES_ID);

			if (errors == null) {
				errors = new ArrayList<String>();
				session.setAttribute(ERROR_MESSAGES_ID, errors);
			}

			errors.add(error);
		}
	}
}

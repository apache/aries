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

package org.apache.words.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.words.AssociationRecorderService;
import org.apache.words.WordGetterService;

/**
 * Servlet implementation class AssociateWord
 */
public class AssociateWord extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public AssociateWord() {
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		out.println("<html>");
		String randomWord = null;
		WordGetterService getter = null;
		try {
			getter = (WordGetterService) new InitialContext()
					.lookup("aries:services/"
							+ WordGetterService.class.getName());
		} catch (NamingException e) {
			e.printStackTrace();
		}
		if (getter != null) {
			randomWord = getter.getRandomWord();
			out.println("The word is " + randomWord);
		} else {
			out.println("Oh dear. We couldn't find our service.");
		}
		out.println("</br>");
		out.println("<form action=\"AssociateWord\" method=\"post\">");
		out.println("What do you associate with "
				+ randomWord
				+ "?	    <input type=\"text\" name=\"association\" /> <br />    ");
		out.println("<input type=\"hidden\" name=\"word\" value=\""
				+ randomWord + "\"/>");
		out.println("<input type=\"submit\" name=\"Submit\"/>");
		out.println("</form>");
		out.println("</html>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String word = request.getParameter("word");
		String association = request.getParameter("association");

		AssociationRecorderService recorder = null;
		try {
			recorder = (AssociationRecorderService) new InitialContext()
					.lookup("aries:services/"
							+ AssociationRecorderService.class.getName());
		} catch (NamingException e) {
			e.printStackTrace();
		}
		String previousAssociation = null;
		if (recorder != null) {
			previousAssociation = recorder.getLastAssociation(word);
			recorder.recordAssociation(word, association);
		}
		PrintWriter out = response.getWriter();
		out.println("The last person associated " + word + " with "
				+ previousAssociation + ".");

	}
}

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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class to provide html headers, footers, dojo use and blogging
 * service.
 */
public class HTMLOutput {

	public static final void writeHTMLHeaderPartOne(PrintWriter out,
			String pageTitle) {
		out.println("<html>");
		out.println(" <head>");

		out
				.println("  <link type=\"text/css\" rel=\"stylesheet\" href=\"style/blog.css\"></link>");
		out.println("  <meta name=\"keywords\" content=\"...\">");
		out.println("  <meta name=\"description\" content=\"...\">");

		out.print("  <title>");
		out.print(pageTitle);
		out.println("  </title>");

		out
				.println("  <META http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">");
		out.println(" </head>");
		
	}

	public static final void writeDojoUses(PrintWriter out, String... modules) {
		out
				.println("<link rel=\"Stylesheet\" href=\"http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dijit/themes/tundra/tundra.css\" type=\"text/css\" media=\"screen\"/>");
		out
				.println("<link rel=\"Stylesheet\" href=\"http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dijit/themes/nihilo/nihilo.css\" type=\"text/css\" media=\"screen\"/>");
		out
				.println("<link rel=\"Stylesheet\" href=\"http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dijit/themes/soria/soria.css\" type=\"text/css\" media=\"screen\"/>");
	
		out
				.println("<script type=\"text/javascript\"  src=\"http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dojo/dojo.xd.js\" djConfig=\"parseOnLoad: true\"></script>");
		out.println("<script type=\"text/javascript\">");
		out.println("dojo.require(\"dojo.parser\");");

		for (String module : modules) {
			out.print("dojo.require(\"");
			out.print(module);
			out.println("\");");
		}

		out.println("</script>");
	}

	public static final void writeHTMLHeaderPartTwo(PrintWriter out) {
		writeHTMLHeaderPartTwo(out, new ArrayList<String>());
	}

	public static final void writeHTMLHeaderPartTwo(PrintWriter out,
			Collection<String> errorMessages) {

		out.println(" <body class=\"soria\">");

		out
				.println("  <TABLE width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
		out.println("   <TR width=\"100%\">");
		out.println("    <TD id=\"cell-0-0\" colspan=\"2\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-0-1\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-0-2\" colspan=\"2\">&nbsp;</TD>");
		out.println("   </TR>");

		out.println("   <TR width=\"100%\">");
		out.println("    <TD id=\"cell-1-0\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-1-1\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-1-2\">");

		out.println("     <DIV style=\"padding: 5px;\">");
		out.println("      <DIV id=\"banner\">");

		out
				.println("       <TABLE border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">");
		out.println("        <TR>");
		out.println("         <TD align=\"left\" class=\"topbardiv\" nowrap=\"\">");
		out
				.println("          <A href=\"http://incubator.apache.org/aries/\" title=\"Apache Aries (incubating)\">");
		out
				.println("          <IMG border=\"0\" src=\"images/Arieslogo_Horizontal.gif\">");
		out.println("          </A>");
		out.println("         </TD>");
		out.println("         <TD align=\"right\" nowrap=\"\">");
		out
				.println("          <A href=\"http://www.apache.org/\" title=\"The Apache Software Foundation\">");
		out
				.println("          <IMG border=\"0\" src=\"images/apache-incubator-logo.png\">");
		out.println("          </A>");
		out.println("         </TD>");
		out.println("        </TR> ");
		out.println("       </TABLE>");
		out.println("      </DIV>");
		out.println("     </DIV>");

		out.println("     <DIV id=\"top-menu\">");
		out
				.println("      <TABLE border=\"0\" cellpadding=\"1\" cellspacing=\"0\" width=\"100%\">");
		out.println("       <TR>");
		out.println("        <TD>");
		out.println("         <DIV align=\"left\">");
		out.println("          <!-- Breadcrumbs -->");
		out.println("          <!-- Breadcrumbs -->");
		out.println("         </DIV>");
		out.println("        </TD>");
		out.println("        <TD>");
		out.println("         <DIV align=\"right\">");
		out.println("          <!-- Quicklinks -->");
		out.println("           <p><a href=\"ViewBlog\" style=\"text-decoration: none; color: white\">Blog home</a></p>");
		out.println("          <!-- Quicklinks -->");
		out.println("         </DIV>");
		out.println("        </TD>");
		out.println("       </TR>");
		out.println("      </TABLE>");
		out.println("     </DIV>");
		out.println("    </TD>");
		out.println("    <TD id=\"cell-1-3\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-1-4\">&nbsp;</TD>");
		out.println("   </TR>");

		out.println("   <TR width=\"100%\">");
		out.println("    <TD id=\"cell-2-0\" colspan=\"2\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-2-1\">");
		out.println("     <TABLE>");
		out.println("      <TR height=\"100%\" valign=\"top\">");
		out.println("       <TD height=\"100%\"></td>");
		out.println("       <TD height=\"100%\" width=\"100%\">");
		out.println("        <H1>Apache Aries Sample Blog</H1><br>");

		if (!!!errorMessages.isEmpty()) {
			out.println("\t\t\t<div id=\"errorMessages\">");
			for (String msg : errorMessages) {
				out.println("\t\t\t\t<div class=\"errorMessage\">" + msg
						+ "</div>");
			}
			out.println("\t\t\t</div>");
		}

		out.println("        <div id=\"mainContent\" class=\"mainContent\">");
	}

	public static final void writeHTMLFooter(PrintWriter out) {
		out.println("         <BR>");
		out.println("        </DIV>");
		out.println("       </TD>");
		out.println("      </TR>");
		out.println("     </TABLE>");
		out.println("    </TD>");
		out.println("    <TD id=\"cell-2-2\" colspan=\"2\">&nbsp;</TD>");
		out.println("   </TR>");
		out.println("   <TR width=\"100%\">");

		out.println("    <TD id=\"cell-3-0\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-3-1\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-3-2\">");
		out.println("     <DIV id=\"footer\">");
		out.println("     <!-- Footer -->");
		out.println("     </DIV>");
		
		out.println("    </TD>");
		out.println("    <TD id=\"cell-3-3\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-3-4\">&nbsp;</TD>");
		out.println("   </TR>");
		out.println("   <TR width=\"100%\">");
		out.println("    <TD id=\"cell-4-0\" colspan=\"2\">&nbsp;</TD>");
		out.println("    <TD id=\"cell-4-1\">&nbsp;</TD>");

		out.println("    <TD id=\"cell-4-2\" colspan=\"2\">&nbsp;</TD>");
		out.println("   </TR>");
		out.println("  </TABLE>");
		out.println(" </BODY>");
		out.println("</HTML> ");

	}

}

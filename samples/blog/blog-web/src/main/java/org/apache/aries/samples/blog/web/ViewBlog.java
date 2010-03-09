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
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.aries.samples.blog.api.BlogComment;
import org.apache.aries.samples.blog.api.BlogEntry;
import org.apache.aries.samples.blog.api.BloggingService;
import org.apache.aries.samples.blog.web.util.HTMLOutput;
import org.apache.aries.samples.blog.web.util.JNDIHelper;



public class ViewBlog extends HttpServlet
{
  private static final long serialVersionUID = -1854915218416871420L;
  private static final int POSTS_PER_PAGE = 10;
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException
  {
    PrintWriter out = resp.getWriter();

    BloggingService service = JNDIHelper.getBloggingService();
    
    String blogTitle = service.getBlogTitle();

    // TODO cope with the service being null, redirect elsewhere.

    HTMLOutput.writeHTMLHeaderPartOne(out, blogTitle);
    HTMLOutput.writeDojoUses(out, "dojo.parser");
    
    

		HTMLOutput.writeHTMLHeaderPartTwo(out);

    int maxPage = (service.getNoOfEntries()-1) / POSTS_PER_PAGE;
    int pageNoInt = 0;
    
    String pageNo = req.getParameter("page");
    if (pageNo != null) {
      try {
        pageNoInt = Integer.parseInt(pageNo)-1;

        if (pageNoInt > maxPage)
          pageNoInt = maxPage;
        else if (pageNoInt < 0)
          pageNoInt = 0;
        
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
  
    Iterator<? extends BlogEntry> posts = service.getBlogEntries(pageNoInt * POSTS_PER_PAGE, POSTS_PER_PAGE).iterator();
    
    out.println("<div class=\"links\"><a href=\"CreateBlogEntryForm\">Create New Post</a> <a href=\"EditAuthorForm\">Create Author</a></div>");
    
    Date currentDate = null;

    for (int i = 0; posts.hasNext(); i++) {
      BlogEntry post = posts.next();
      
      if (doesNotMatch(post.getPublishDate(), currentDate)) {
        currentDate = post.getPublishDate();
        out.print("<div class=\"postDate\">");
        //out.print(DateFormat.getDateInstance(DateFormat.FULL).format(currentDate));
        if (currentDate != null) {
        	 out.print(DateFormat.getDateInstance(DateFormat.FULL).format(currentDate));          
        }

        out.println("</div>");
      }
      
      out.print("\t\t<div class=\"post\" id=\"");
      out.print(i);
      out.println("\">");

      out.print("\t\t\t<div class=\"postTitle\">");
      out.print(post.getTitle());
      out.print("</div>");
      out.print("\t\t\t<div class=\"postBody\">");
      out.print(post.getBody());
      out.println("</div>");
      out.print("\t\t\t<div class=\"postAuthor\"><a href=\"ViewAuthor?email=");
      out.print(post.getAuthorEmail());
      out.print("\">");
      out.print(post.getAuthor().getFullName());
      out.println("</a></div>");
      
      if (service.isCommentingAvailable()) {

			out.print("<div class=\"links\"><a href=\"AddCommentForm?postId=");
			out.print(post.getId());
			out.print("\">Add Comment</a></div>");

			List<? extends BlogComment> comments = service
					.getCommentsForEntry(post);
			int size = comments.size();
			out.print("<div class=\"commentTitle\"");
			if (size > 0) {
				out.print("onclick=\"expand(");
				out.print(post.getId());
				out.print(")\"");
			}
			out.print(" style=\"cursor: pointer;\">Comments (");
			out.print(size);
			out.println(")</div>");

			if (size > 0) {

				out.print("<div id=\"comments");
				out.print(post.getId());
				out.println("\">");

				for (BlogComment comment : comments) {
					out.println("<div class=\"comment\">");

					out.println(comment.getComment());

					out.println("</div>");
					out
							.print("\t\t\t<div class=\"commentAuthor\"><a href=\"ViewAuthor?email=");
					out.print(comment.getAuthor().getEmailAddress());
					out.print("\">");
					out.print(
						comment.getAuthor().getName());
					out.println("</a></div>");
				}

				out.println("</div>");
			}
		}

     
      out.println("\t\t</div>");
    }
    
    /*
     * Translate indices from 0-indexed to 1-indexed
     */
    writePager(out, pageNoInt+1, maxPage+1);

    HTMLOutput.writeHTMLFooter(out);
  }
  
  /**
   * Write a paging bar (if there is more than a single page)
   * 
   * @param out
   * @param currentPage Page number (indices starting from 1)
   * @param maxPage (indices starting from 1)
   */
  private void writePager(PrintWriter out, int currentPage, int maxPage)
  {
    /*
     * No paging is needed if we only have a single page
     */
    if (maxPage > 1) {
      out.println("<div id=\"pagination\">");
      
      if (currentPage > 1) {
        out.println("<a href=\"ViewBlog?page=1\">&lt;&lt;</a>");
        out.println("<a href=\"ViewBlog?page="+(currentPage-1)+"\">&lt;</a>");
      } else {
        out.println("<span>&lt;&lt;</span>");
        out.println("<span>&lt;</span>");
      }
      
      out.println(currentPage + " of " + maxPage);
  
      if (currentPage < maxPage) {
        out.println("<a href=\"ViewBlog?page="+(currentPage+1)+"\">&gt;</a>");
        out.println("<a href=\"ViewBlog?page=" + maxPage + "\">&gt;&gt;</a>");
      } else {
        out.println("<span>&gt;&gt;</span>");
        out.println("<span>&gt;</span>");        
      }
      
      out.println("</div>");
    }
  }

  private boolean doesNotMatch(Date publishDate, Date currentDate)
  {
    if (currentDate == null) return true;
    Calendar publish = Calendar.getInstance();
    Calendar current = Calendar.getInstance();
    publish.setTime(publishDate);
    current.setTime(currentDate);
    boolean differentYear = publish.get(Calendar.YEAR) != current.get(Calendar.YEAR);
    boolean differentMonth = publish.get(Calendar.MONTH) != current.get(Calendar.MONTH);
    boolean differentDayOfMonth = publish.get(Calendar.DAY_OF_MONTH) != current.get(Calendar.DAY_OF_MONTH);
    return differentYear || differentMonth || differentDayOfMonth;
  }
}
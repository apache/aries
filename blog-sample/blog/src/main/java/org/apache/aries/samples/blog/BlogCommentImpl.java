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
package org.apache.aries.samples.blog;

import java.util.Date;
import java.util.Calendar;

import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.api.BlogComment;
import org.apache.aries.samples.blog.api.BlogEntry;
import org.apache.aries.samples.blog.comment.persistence.api.Comment;


public class BlogCommentImpl implements BlogComment {
	private static Calendar cal = Calendar.getInstance();
	
	private Comment comment;
	
	public BlogCommentImpl(Comment c) {
		 comment = c;
	}
  /** Get comment 
   *  @return the String representing the comment 
   */
  public String getComment() {
	  return comment.getComment();
  }
  
  /** Get the author of the comment 
   *  @return the BlogAuthor instance 
   */
  public BlogAuthor getAuthor() {
	  return new BlogAuthorImpl(comment.getAuthor());
  }
  
  /** Get the parent blog post for the comment 
   *  @return the BlogPost instance the comment is attached to.  
   */
  public BlogEntry getEntry() {
	  return new BlogEntryImpl(comment.getEntry());
  }

  /** Get the Id value of the comment 
   *  @return the integer id of the comment 
   */
  public int getId() {
	  return comment.getId();
  }
  
  /** Get the creation date for the comment 
   *  @return the String representation of the date the comment was
   *  created in dd-mm-yyyy format. 
   */
  public String getCommentCreationDate() {
	  
	  Date dc = comment.getCreationDate();
	  int year;
		int month;
		int date;

		synchronized (cal) {
			cal.setTime(dc);
			year = cal.get(Calendar.YEAR);
			month = cal.get(Calendar.MONTH) + 1;
			date = cal.get(Calendar.DATE);
		}

		return year + "-" + month + "-" + date;

  }
  
}

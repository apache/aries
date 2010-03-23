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
package org.apache.aries.samples.blog.api.comment.persistence.api;

import java.util.List;

public interface BlogCommentService {

	  /**
	   * Create a comment against a blog entry.
	   * 
	   * @param comment the comment text
	   * @param author the author
	   * @param blogEntry the blog entry against which we are commenting
	   */
	  void createComment(String comment, String authorEmail, long entryId);

	  /**
	   * Delete a blog entry comment
	   * 
	   * @param comment the comment being deleted.
	   */
	  void delete(int id);

	  /**
	   * Get comments for a given blog entry post.
	   * 
	   * @param id the blog entry id
	   * @return a List<BlogComment> for the blog entry
	   */
	  List<? extends Comment> getCommentsForEntry(long id);

	  /**
	   * Get comments for a given author.
	   * 
	   * @param emailAddress the email address of the author
	   * @return a List<BlogComment> for the given email address
	   */
	 List<? extends Comment> getCommentsForAuthor(String emailAddress);

}

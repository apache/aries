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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.aries.samples.blog.api.BlogComment;
import org.apache.aries.samples.blog.api.BlogCommentManager;
import org.apache.aries.samples.blog.comment.persistence.api.BlogCommentService;
import org.apache.aries.samples.blog.comment.persistence.api.Comment;


public class BlogCommentManagerImpl implements BlogCommentManager {
	
	private BlogCommentService commentService;
	private boolean commentServiceValid;
	
	// Injected via blueprint
	public void setCommentService(BlogCommentService bcs) {
		commentService = bcs;
	}


	public void createComment(String comment, String email, long entryId) {
		commentService.createComment(comment, email, entryId);
	}

	public List<? extends BlogComment> getCommentsByAuthor(String email) {
		List<? extends Comment> comment = commentService.getCommentsForAuthor(email);
		return adaptComment(comment);
		
	}		

	public List<? extends BlogComment> getCommentsForPost(long id) {
		List<? extends Comment> comment = commentService.getCommentsForEntry(id);
		return adaptComment(comment);
	}
		

	public void deleteComment(int id) {
		commentService.delete(id);
	}

	private List<? extends BlogComment> adaptComment(
			List<? extends Comment> comments) {
		List<BlogComment> list = new ArrayList<BlogComment>();

		Iterator<? extends Comment> c = comments.iterator();
		while (c.hasNext()) {
			list.add(new BlogCommentImpl(c.next()));
			
		}
		return list;

	}
	
	public boolean isCommentingAvailable() {
		return commentServiceValid;
	}
	
	public void blogServiceBound(BlogCommentService comment, Map props) {
		commentServiceValid = true;
	}

	public void blogServiceUnbound(BlogCommentService comment, Map props) {

	}

}

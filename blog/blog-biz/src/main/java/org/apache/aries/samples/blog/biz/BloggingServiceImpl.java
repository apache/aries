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
package org.apache.aries.samples.blog.biz;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.api.BlogAuthorManager;
import org.apache.aries.samples.blog.api.BlogComment;
import org.apache.aries.samples.blog.api.BlogCommentManager;
import org.apache.aries.samples.blog.api.BlogEntry;
import org.apache.aries.samples.blog.api.BlogEntryManager;
import org.apache.aries.samples.blog.api.BloggingService;

/** Implementation of the BloggingService */
public class BloggingServiceImpl implements BloggingService {
	private BlogEntryManager blogEntryManager;
	private BlogAuthorManager blogAuthorManager;
	private BlogCommentManager blogCommentManager;

	// Injected via blueprint
	public void setBlogEntryManager(BlogEntryManager blogPostManager) {
		this.blogEntryManager = blogPostManager;
	}

	// Injected via blueprint
	public void setBlogAuthorManager(BlogAuthorManager authorManager) {
		this.blogAuthorManager = authorManager;
	}
	
	// Injected via blueprint
	public void setBlogCommentManager(BlogCommentManager commentManager) {
		this.blogCommentManager = commentManager;
	}


	public String getBlogTitle() {
		return new BlogImpl().getBlogTitle();
	}

	public BlogAuthor getBlogAuthor(String email) {
		return blogAuthorManager.getAuthor(email);
	}

	public void createBlogAuthor(String email, String nickName, String name,
			String bio, String dob) {
		try {
			blogAuthorManager.createAuthor(email, dob, name, nickName, bio);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateBlogAuthor(String email, String nickName, String name,
			String bio, String dob) {
		try {
			blogAuthorManager.updateAuthor(email, dob, name, nickName, bio);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public BlogEntry getPost(long id) {
		return blogEntryManager.getBlogPost(id);
	}

	public List<? extends BlogEntry> getBlogEntries(int firstPostIndex,
			int noOfPosts) {
		return blogEntryManager.getBlogEntries(firstPostIndex, noOfPosts);

	}

	public List<? extends BlogEntry> getAllBlogEntries() {
		return blogEntryManager.getAllBlogEntries();
	}

	public int getNoOfEntries() {
		return blogEntryManager.getNoOfPosts();
	}

	public void createBlogEntry(String email, String title, String blogText,
			String tags) {
		blogEntryManager.createBlogPost(email, title, blogText, Arrays
				.asList(tags.split(",")));
	}

	public void createBlogComment(String comment, String authorEmail, long id) {
		blogCommentManager.createComment(comment, authorEmail, id);
	}

	public void deleteBlogComment(BlogComment comment) {
		blogCommentManager.deleteComment(comment.getId());
	}

	public List<? extends BlogComment> getCommentsForEntry(BlogEntry entry) {
		return blogCommentManager.getCommentsForPost(entry.getId());
	}

	public BlogEntry getBlogEntry(long id) {
		return blogEntryManager.getBlogPost(id);
	}
	
	public boolean isCommentingAvailable() {
		return blogCommentManager.isCommentingAvailable();

	}
}
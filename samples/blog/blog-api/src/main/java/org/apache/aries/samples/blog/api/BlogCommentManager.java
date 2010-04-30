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
package org.apache.aries.samples.blog.api;

import java.util.List;

public interface BlogCommentManager {
	/**
	 * Create a comment by an author (email) against a post (Id)
	 * @param comment
	 * @param email
	 * @param entryId
	 */
	public void createComment(String comment, String email, long entryId);

	/**
	 * Get all the comments made by an author
	 * @param email
	 * @return a list of comments made by an author
	 */
	public List<? extends BlogComment> getCommentsByAuthor(String email);

	/**
	 * 
	 * @param id
	 * @return A list of comments made about an entry
	 */
	public List<? extends BlogComment> getCommentsForPost(long id); 

	/**
	 * Delete a specific comment using it's id
	 * @param id
	 */
	public void deleteComment(int id);

	/**
	 * Check to see whether the comment service is available
	 * @return
	 */
	public boolean isCommentingAvailable();

}

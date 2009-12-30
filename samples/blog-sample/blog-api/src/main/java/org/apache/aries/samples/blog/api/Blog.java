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

public interface Blog
{
  /**
   * Gets the title of the blog
   * @return currently returns the fixed value of "Aries Sample Blog"
   */
  String getBlogTitle();

  /**
   * Get the total number of blog entries in the database.
   * @return an int representing the number of entries
   */
  int getNoOfPosts();
  
  /**
   * Get N posts from the database starting with post number X.
   * @param firstPostIndex index of the first post to retrieve
   * @param noOfPosts number of posts to retrieve
   * @return a List<BlogPost> containing N posts
   */
  List<BlogPost> getPosts(int firstPostIndex, int noOfPosts);

  /**
   * Creates a new blog posting
   * 
   * @param email the author's email address
   * @param title the title for the blog entry
   * @param text the text of the entry
   * @param tags keyword tags for the blog entry
   */
  void createPost(String email, String title, String text, String tags);

}

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

public interface BloggingService
{
  /**
   *  Get the blog
   *  @return the blog with all entries
   */
  Blog getBlog();

  /** 
   * Get the author associated with a given email address.
   * 
   * @param email the email address of interest
   * @return the blog author with the supplied email address
   */
  BlogAuthor getBlogAuthor(String email);
  
  /**
   * Get the blog post with the specified id.
   * 
   * @param id the blog entry id
   * @return the blog post
   */
  BlogPost getPost(long id);

  /**
   * Update the attributes of an author.
   * 
   * @param email the email address of the author being updated
   * @param nickName the display name for this author
   * @param name the full name for this author
   * @param bio the biography for this author
   * @param dob the date of birth for this author
   */
  void updateAuthor(String email, String nickName, String name, String bio, String dob);

  /**
   * Create a new author.
   * 
   * @param email the author's email address
   * @param nickName the author's display name
   * @param name the author's full name
   * @param bio the author's biography
   * @param dob the author's date of birth
   */
  void createAuthor(String email, String nickName, String name, String bio, String dob);
  
}

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

public interface BloggingService
{
  /**
   *  Get the blog
   *  @return the title of the Blog
   */
  String getBlogTitle();

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
  BlogEntry getBlogEntry(long id);


  /**
   * Update the attributes of an author.
   * 
   * @param email the email address of the author being updated
   * @param nickName the display name for this author
   * @param name the full name for this author
   * @param bio the biography for this author
   * @param dob the date of birth for this author
   */
  void updateBlogAuthor(String email, String nickName, String name, String bio, String dob);
  
  /**
   * Get the number of entries(posts) in the blog
   * @return the number of posts.
   */
  public int getNoOfEntries(); 
  
  /**
   * Get the a number of entries starting at the teh first index
   * @param firstPostIndex
   * @param noOfPosts
   * @return a list of BlogEntries
   */
  public List<? extends BlogEntry> getBlogEntries(int firstPostIndex, int noOfPosts);
  
  /**
   * Get all the blog entries
   * @return a lost of BlogEntrys
   */
  public List<? extends BlogEntry> getAllBlogEntries();

  /**
   * Create a new author.
   * 
   * @param email the author's email address
   * @param nickName the author's display name
   * @param name the author's full name
   * @param bio the author's biography
   * @param dob the author's date of birth
   */
  void createBlogAuthor(String email, String nickName, String name, String bio, String dob);
  
  /**
   * 
   * @param email the email address of the author
   * @param title the title of the post
   * @param blogText the test of the post
   * @param tags list of tags associated with the post
   */
  void createBlogEntry(String email, String title, String blogText, String tags); 
  
  /**
   * Retrieve the state of the blog commenting service
   * 
   * @return true if available else false
   */
  boolean isCommentingAvailable();
  
  /**
   * Create a comment
   * @param text
   * @param email
   * @param entryId
   */
  void createBlogComment(String text, String email, long entryId);
  
  /**
   * Get the comments associated with an entry
   * @param entry
   * @return a list of comments for an entry (post)
   */
  List <? extends BlogComment> getCommentsForEntry(BlogEntry entry);
  
  
  
  
}

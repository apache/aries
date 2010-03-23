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

import java.text.ParseException;
import java.util.List;

import org.apache.aries.samples.blog.api.persistence.Entry;



public interface BlogEntryManager
{
  /**
   * Create a blog posting.
   * @param email the author's email
   * @param title the title of the entry
   * @param blogText the text of the entry
   * @param tags tags associated with the blog entry
   */
  public void createBlogPost(String email, String title, String blogText, List<String> tags);
  
  /**
   * Find a specific blog entry by title.
   * @param title the title to search for
   * @return the blog entry
   */
  public Entry findBlogEntryByTitle(String title);
  
  /**
   * Retrieve all blog entries.
   * @return a List<BlogEntry> of all blog entries
   */
  public List<? extends BlogEntry> getAllBlogEntries();
  
  /**
   * Retrieve all blog entries for a specific author.
   * @param emailAddress the email address of the author in question
   * @return a List<BlogEntry>
   */
  public List<? extends BlogEntry> getBlogsForAuthor(String emailAddress);
  
  /**
   * Retrieve all blog entries created between a specified date range.
   * @param startDate the start date
   * @param endDate the end date
   * @return a List<BlogEntry>
   * @throws ParseException
   */
  public List<?extends BlogEntry> getBlogEntriesModifiedBetween(String startDate, String endDate) throws ParseException;

  /**
   * Get N posts from the database starting at post number X
   * 
   * @param firstPostIndex the first post to retrieve
   * @param noOfPosts the number of posts to retrieve in total
   * @return a List<BlogEntry> of N posts
   */
  public List<? extends BlogEntry> getBlogEntries(int firstPostIndex, int noOfPosts);
  
  /**
   * Get the total number of blog entries in the database
   * @return the int number of entries
   */
  public int getNoOfPosts();
  
  /**
   * Remove a specific blog entry.
   * @param a the author of the blog entry
   * @param title the title of the blog entry
   * @param publishDate the publication date of the blog entry
   * @throws ParseException
   */
  public void removeBlogEntry(BlogAuthor author, String title, String publishDate) throws ParseException;
  
  /**
   * Update a blog entry.
   * @param originalEntry the original blog entry
   * @param a the author of the blog entry
   * @param title the title of the blog entry
   * @param publishDate the publication date of the blog entry
   * @param blogText the text content of the blog entry
   * @param tags any assocaited tags for the blog entry
   * @throws ParseException
   */
  public void updateBlogEntry(BlogEntry originalEntry, BlogAuthor a, String title, String publishDate, String blogText, List<String> tags) throws ParseException;

  
  /**
   * Get the specified blog posting.
   * @param id the id of the blog posting
   * @return the blog post
   */
  public BlogEntry getBlogPost(long id);
}

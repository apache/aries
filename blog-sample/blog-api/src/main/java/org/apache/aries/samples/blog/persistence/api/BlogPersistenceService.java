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
package org.apache.aries.samples.blog.persistence.api;

import java.util.Date;
import java.util.List;



/**
 * This is the interface for the persistence layer of the blog
 * application.  This persistence layer is registered in the service
 * registry and is used by the main application layer. 
 *
 */
public interface BlogPersistenceService
{

  /**
   * Get all the blog entries in the data store
   * @return a list of BlogEntry objects
   */
  public List<BlogEntry> getAllBlogEntries();

  /**
   * Get the number of blog entries in the data store
   * @return the number of blog entries
   */
  public int getNoOfBlogEntries();

  /**
   * Get the first N most recent posts starting from post X
   * @param firstPostIndex - The index of the first post to be retrieved
   * @param no - The number of posts to be retrieved starting from firstPostIndex
   */
  public List<BlogEntry> getBlogEntries(int firstPostIndex, int no);

  /**
   * Get all the blog entries made by a particular
   * author
   * 
   * @param emailAddress the author's email address
   * @return a list of BlogEntry objects
   */
  public List<BlogEntry> getBlogsForAuthor(String emailAddress);

  /**
   * Get a BlogEntry that has a given title
   * @param title the title of interest
   * @return A BlogEntry with a specific title (or null if no entry exists in the
   *         data store)
   */
  public BlogEntry findBlogEntryByTitle(String title);

  /**
   * Get BlogEntries created or modified between two specified dates
   * @param start  The Date defining the start of the time period
   * @param end    The Date defining the end of the time period
   * @return  A list of BlogEntry objects
   */
  public List<BlogEntry> getBlogEntriesModifiedBetween(Date start, Date end);

  /**
   * Obtain a given Blog post using its unique id.
   * 
   * @param postId the posts unique id.
   * @return the Blog post.
   */
  public BlogEntry getBlogEntryById(long postId);

  /**
   * Get the details for an author
   * @param emailAddress the Author's email address
   * @return An Author object
   */
  public Author getAuthor(String emailAddress);

  /**
   * Get all authors in the database
   * @return a List of Authors
   */
  public List<Author> getAllAuthors();

  /**
   * Create an author in the database
   * 
   * @param emailAddress
   * 			The author's email address
   * @param dob
   *     		The author's date of birth
   * @param name
   *        	The author's name
   * @param displayName
   *        	??
   * @param bio
   *        	The author's bio.
   */
  public void createAuthor(String email, Date dob, String name, String displayName, String bio);

  /**
   * Create an Blog post in the database
   * 
   * @param a
   * 			The author
   * @param title
   * 			The title of the post
   * @param blogText
   * 			The text of the post
   * @param tags
   * 			??
   */
  public void createBlogPost(Author a, String title, String blogText, List<String> tags);

  /**
   * Update an author in the database
   * @param 
   */
  public void updateAuthor(String email, Date dob, String name, String displayName, String bio);

  /**
   * Update an post in the database
   * 
   * @param b  The updated BlogEntry object. This object must be a modified BlogEntry
   *           previously returned by this service.
   */
  public void updateBlogPost(BlogEntry b);

  /**
   * Remove the author with the specified email address
   * 
   * @param emailAddress the email address of the author to remove
   */
  public void removeAuthor(String emailAddress);

  /**
   * Remove the specified BlogEntry, note that this must be a BlogEntry returned by
   * this service.
   * 
   * @param blogEntry the blog entry to remove
   */
  public void removeBlogEntry(BlogEntry blogEntry);

}

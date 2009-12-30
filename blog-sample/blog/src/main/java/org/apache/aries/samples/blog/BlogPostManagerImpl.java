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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.aries.samples.blog.api.BlogPost;
import org.apache.aries.samples.blog.api.BlogPostManager;
import org.apache.aries.samples.blog.persistence.api.Author;
import org.apache.aries.samples.blog.persistence.api.BlogEntry;
import org.apache.aries.samples.blog.persistence.api.BlogPersistenceService;



public class BlogPostManagerImpl implements BlogPostManager
{
  private BlogPersistenceService persistenceService;
  
  
  private boolean commentServiceValid;

  // Injected via blueprint
  public void setPersistenceService(BlogPersistenceService persistenceService)
  {
    this.persistenceService = persistenceService;
  }
  
  
  public void createBlogPost(Author a, String title, String blogText, List<String> tags)
  {
    persistenceService.createBlogPost(a, title, blogText, tags);
  }
  
  public BlogEntry findBlogEntryByTitle(String title)
  {
    return persistenceService.findBlogEntryByTitle(title);
  }
  
  public List<BlogEntry> getAllBlogEntries()
  {
    return persistenceService.getAllBlogEntries();
  }
  
  public List<BlogEntry> getBlogEntries(int firstPostIndex, int noOfPosts)
  { 
    return persistenceService.getBlogEntries(firstPostIndex, noOfPosts);
  }
  
  public List<BlogEntry> getBlogsForAuthor(String emailAddress)
  {
    return persistenceService.getBlogsForAuthor(emailAddress);
  }
  
  public List<BlogEntry> getBlogEntriesModifiedBetween(String startDate, String endDate) throws ParseException
  {
    if(startDate == null || "".equals(startDate)) throw new IllegalArgumentException("A valid start date must be supplied");
    if(endDate == null || "".equals(endDate)) throw new IllegalArgumentException("A valid end date must be supplied");
    SimpleDateFormat sdf = new SimpleDateFormat("dd-mm-yyyy");
    Date start = sdf.parse(startDate);
    Date end = sdf.parse(endDate);
    return persistenceService.getBlogEntriesModifiedBetween(start, end); 
  }
  
  public int getNoOfPosts()
  {
    return persistenceService.getNoOfBlogEntries();
  }
  
  public void removeBlogEntry(Author a, String title, String publishDate) throws ParseException
  {
    if(a == null) throw new IllegalArgumentException("An author must be specified");
    if(title == null) title = "";
    if(publishDate == null) throw new IllegalArgumentException("The article must have a publication date");
    BlogEntry found = null;
    Date pubDate = parseDate(publishDate);
    
    for(BlogEntry b : a.getPosts()) {
      if(title.equals(b.getTitle()) && pubDate.equals(b.getPublishDate())){
        found = b;
        break;
      }
    }
    persistenceService.removeBlogEntry(found);
  }
  
  public void updateBlogEntry(BlogEntry originalEntry, Author a, String title, String publishDate, String blogText, List<String> tags) throws ParseException
  {
    if(originalEntry.getAuthor() == null || originalEntry.getAuthor().getEmail() == null) throw new IllegalArgumentException("An author must be specified");
    if(title == null) title = "";
    if(publishDate == null) throw new IllegalArgumentException("The article must have a publication date");
    BlogEntry found = null;
    Date pubDate = parseDate(publishDate);
    for(BlogEntry b : getBlogsForAuthor(originalEntry.getAuthor().getEmail())) {
      if(title.equals(b.getTitle()) && pubDate.equals(b.getPublishDate())){
        found = b;
        break;
      }
    }
    
    if(found == null) 
      throw new IllegalArgumentException("No blog entry could be found");
    
    found.setAuthor(a);
    found.setTitle(title);
    found.setBlogText(blogText);
    found.setTags((tags == null) ? new ArrayList<String>() : tags);
    found.setUpdatedDate(new Date(System.currentTimeMillis()));
    
    persistenceService.updateBlogPost(found);
  }
  
  private Date parseDate(String dateString) throws ParseException
  {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-mm-yyyy");
    return sdf.parse(dateString);
  }

  public boolean isCommentingAvailable()
  {
    return commentServiceValid;
  }

  
  public BlogPost getBlogPost(long id)
  {
    return new BlogPostImpl(persistenceService.getBlogEntryById(id), this);
  }
}
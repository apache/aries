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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.api.BlogEntry;
import org.apache.aries.samples.blog.api.BlogEntryManager;
import org.apache.aries.samples.blog.api.persistence.api.BlogPersistenceService;
import org.apache.aries.samples.blog.api.persistence.api.Entry;



public class BlogEntryManagerImpl implements BlogEntryManager
{
  private BlogPersistenceService persistenceService;
  

  // Injected via blueprint
  public void setPersistenceService(BlogPersistenceService persistenceService)
  {
    this.persistenceService = persistenceService;
  }
  
  
  public void createBlogPost(String email, String title, String blogText, List<String> tags)
  {
    persistenceService.createBlogPost(email, title, blogText, tags);
  }
  
  public Entry findBlogEntryByTitle(String title)
  {
    return persistenceService.findBlogEntryByTitle(title);
  }
  
  public List<? extends BlogEntry> getAllBlogEntries()
  {
	  List<? extends Entry> entries = persistenceService.getAllBlogEntries();
		return adaptEntries(entries);
  }
  
  public List<? extends BlogEntry> getBlogEntries(int firstPostIndex, int noOfPosts)
  { 
	  List<? extends Entry> entries = persistenceService.getBlogEntries(firstPostIndex, noOfPosts);		   		      
		return adaptEntries(entries);
  }
  
  public List<? extends BlogEntry> getBlogsForAuthor(String emailAddress)
  {
		List <?extends Entry> entries= persistenceService.getBlogsForAuthor(emailAddress);
		return adaptEntries(entries);
    
  }
  
  public List<? extends BlogEntry> getBlogEntriesModifiedBetween(String startDate, String endDate) throws ParseException
  {
    if(startDate == null || "".equals(startDate)) throw new IllegalArgumentException("A valid start date must be supplied");
    if(endDate == null || "".equals(endDate)) throw new IllegalArgumentException("A valid end date must be supplied");
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    Date start = sdf.parse(startDate);
    Date end = sdf.parse(endDate);
	List <? extends Entry> entries = persistenceService.getBlogEntriesModifiedBetween(start, end);
	return adaptEntries(entries);
  }
  
  public int getNoOfPosts()
  {
    return persistenceService.getNoOfBlogEntries();
  }
  
  public void removeBlogEntry(BlogAuthor a, String title, String publishDate) throws ParseException
  {
    if(a == null) throw new IllegalArgumentException("An author must be specified");
    if(title == null) title = "";
    if(publishDate == null) throw new IllegalArgumentException("The article must have a publication date");
    Date pubDate = parseDate(publishDate);
    long found = -920234218060948564L;
    
    for(BlogEntry b : a.getEntries()) {
      if(title.equals(b.getTitle()) && pubDate.equals(b.getPublishDate())){
        found = b.getId();
        break;
      }
    }
    persistenceService.removeBlogEntry(found);
  }
  
  public void updateBlogEntry(BlogEntry originalEntry, BlogAuthor a, String title, String publishDate, String blogText, List<String> tags) throws ParseException
  {
	
	if (originalEntry.getAuthor() == null
			|| originalEntry.getAuthorEmail() == null)
		throw new IllegalArgumentException("An author must be specified");
	if (title == null)
		title = "";
	if (publishDate == null)
		throw new IllegalArgumentException(
				"The article must have a publication date");
	long found = -920234218060948564L;
	Date pubDate = parseDate(publishDate);
	for (BlogEntry b : getBlogsForAuthor(originalEntry.getAuthorEmail()
			)) {
		if (title.equals(b.getTitle())
				&& pubDate.equals(b.getPublishDate())) {
			found = b.getId();
			break;
		}
	}

	if (found == -920234218060948564L)
		throw new IllegalArgumentException("No blog entry could be found");

	String email = a.getEmailAddress();

	if (tags == null) {
		tags = new ArrayList<String>();
	}

	Date updatedDate = new Date(System.currentTimeMillis());

	persistenceService.updateBlogEntry(found, email, title, blogText, tags,
			updatedDate);
  }
  
  private Date parseDate(String dateString) throws ParseException
  {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    return sdf.parse(dateString);
  }

  
  public BlogEntry getBlogPost(long id)
  {
    return new BlogEntryImpl(persistenceService.getBlogEntryById(id));
  }
  
	private List <? extends BlogEntry> adaptEntries(List<? extends Entry> entries) {
		return new BlogListAdapter<BlogEntry, Entry>(entries, BlogEntryImpl.class, Entry.class);
  }
	
}
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
import java.util.Iterator;
import java.util.List;

import org.apache.aries.samples.blog.api.*;
import org.apache.aries.samples.blog.api.persistence.api.Author;
import org.apache.aries.samples.blog.api.persistence.api.BlogPersistenceService;



public class BlogAuthorManagerImpl implements BlogAuthorManager
{
  private BlogPersistenceService persistenceService;

  // Blueprint injection used to set the persistenceService
  public void setPersistenceService(BlogPersistenceService persistenceService)
  {
    this.persistenceService = persistenceService;
  }
  
  public void createAuthor(String email, String dob, String name, String displayName, String bio) throws ParseException
  {
    if(email == null) throw new IllegalArgumentException("Email must not be null");
   
    Date dateOfBirth;
    
	dateOfBirth = (dob == null || "".equals(dob)) ? null : new SimpleDateFormat("yyyy-MM-dd").parse(dob);
	
    persistenceService.createAuthor(email, dateOfBirth, name, displayName, bio);
  }
  
  public List<? extends BlogAuthor> getAllAuthors()
  {
	  List<? extends Author> authors = persistenceService.getAllAuthors();
		return adaptAuthor(authors);
  }
  
  public BlogAuthor getAuthor(String emailAddress)
  {
    if(emailAddress == null) throw new IllegalArgumentException("Email must not be null");
    Author a = persistenceService.getAuthor(emailAddress);
    if (a != null)
		return new BlogAuthorImpl(a);
	else
		return null;
  }
  
  public void removeAuthor(String emailAddress)
  {
    if(emailAddress == null) throw new IllegalArgumentException("Email must not be null");
    persistenceService.removeAuthor(emailAddress);
  }
  
  public void updateAuthor(String email, String dob, String name, String displayName, String bio) throws ParseException
  { 
	  if (email == null)
			throw new IllegalArgumentException("Email must not be null");
    Date dateOfBirth = (dob == null) ? null : new SimpleDateFormat("yyyy-MM-dd").parse(dob);
    persistenceService.updateAuthor(email, dateOfBirth, name, displayName, bio);
  }
  
	private List<? extends BlogAuthor> adaptAuthor(
			List<? extends Author> authors) {
			return new BlogListAdapter<BlogAuthor, Author>(authors, BlogAuthorImpl.class, Author.class);

	}
 
}

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
import java.util.Date;
import java.util.List;

import org.apache.aries.samples.blog.api.*;
import org.apache.aries.samples.blog.persistence.api.Author;
import org.apache.aries.samples.blog.persistence.api.BlogPersistenceService;



public class AuthorManagerImpl implements AuthorManager
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
    
	dateOfBirth = (dob == null || "".equals(dob)) ? null : new SimpleDateFormat("dd-mm-yyyy").parse(dob);
	
    persistenceService.createAuthor(email, dateOfBirth, name, displayName, bio);
  }
  
  public List<Author> getAllAuthors()
  {
    return persistenceService.getAllAuthors();
  }
  
  public Author getAuthor(String emailAddress)
  {
    if(emailAddress == null) throw new IllegalArgumentException("Email must not be null");
    return persistenceService.getAuthor(emailAddress);
  }
  
  public void removeAuthor(String emailAddress)
  {
    if(emailAddress == null) throw new IllegalArgumentException("Email must not be null");
    persistenceService.removeAuthor(emailAddress);
  }
  
  public void updateAuthor(String email, String dob, String name, String displayName, String bio) throws ParseException
  {    
    Date dateOfBirth = (dob == null) ? null : new SimpleDateFormat("yyyy-MM-dd").parse(dob);
    updateAuthor(email, dateOfBirth, name, displayName, bio);
  }
  
  public void updateAuthor(String email, Date dob, String name, String displayName, String bio) throws ParseException
  {
    if(email == null) throw new IllegalArgumentException("Email must not be null");   
    
    persistenceService.updateAuthor(email, dob, name, displayName, bio);
  }
  
}

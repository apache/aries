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

import java.util.Calendar;
import java.util.Date;

import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.persistence.api.Author;



public class BlogAuthorImpl implements BlogAuthor
{
  private static Calendar cal = Calendar.getInstance();
  private Author author;
  private BloggingServiceImpl bloggingService;
  
  public BlogAuthorImpl(Author a, BloggingServiceImpl bs)
  {
    author = a;
    bloggingService = bs;
  }

  public String getBio()
  {
    return author.getBio();
  }

  public String getEmailAddress()
  {
    return author.getEmail();
  }

  public String getFullName()
  {
    return author.getName();
  }

  public String getName()
  {
    return author.getDisplayName();
  }

  public String getDateOfBirth()
  {
    Date dob = author.getDob();
    
    int year;
    int month;
    int date;
    
    synchronized (cal) {
      cal.setTime(dob);
      year = cal.get(Calendar.YEAR);
      month = cal.get(Calendar.MONTH) + 1;
      date = cal.get(Calendar.DATE);
    }
    
    return year + "-" + month + "-" + date;
  }
}
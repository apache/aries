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

import java.util.Date;

import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.api.BlogEntry;
import org.apache.aries.samples.blog.api.persistence.api.Entry;


/** Implementation of a BlogPast */
public class BlogEntryImpl implements BlogEntry
{
  public Entry theEntry;

  public BlogEntryImpl(Entry blogEntry)
  {
    theEntry = blogEntry;
  }

  public BlogAuthor getAuthor()
  {
    return new BlogAuthorImpl(theEntry.getAuthor());
    
  }

  public String getBody()
  {
    return theEntry.getBlogText();
  }

  public String getTitle()
  {
    return theEntry.getTitle();
  }

  public String getAuthorEmail()
  {
    return theEntry.getAuthor().getEmail();
  }
  
  public Date getPublishDate()
  {
    return theEntry.getPublishDate();
  }
  
  public long getId()
  {
    return theEntry.getId();
  }

}
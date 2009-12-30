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

import java.util.Date;

import org.apache.aries.samples.blog.api.BlogPost;
import org.apache.aries.samples.blog.api.BlogPostManager;
import org.apache.aries.samples.blog.persistence.api.BlogEntry;


/** Implementation of a BlogPast */
public class BlogPostImpl implements BlogPost
{
  public BlogEntry theEntry;
  
  private BlogPostManager postManager;

  public BlogPostImpl(BlogEntry blogEntry, BlogPostManager pManager)
  {
    theEntry = blogEntry;
    postManager = pManager;
  }

  public String getAuthor()
  {
    String result = theEntry.getAuthor().getDisplayName();
    
    if (result == null || result.length() == 0) result = theEntry.getAuthor().getEmail();
    
    return result;
  }

  public String getBody()
  {
    return theEntry.getBlogText();
  }

  public String getTitle()
  {
    return theEntry.getTitle();
  }

  protected BlogEntry getBlogEntry()
  {
    return theEntry;
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
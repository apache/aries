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

import org.apache.aries.samples.blog.api.AuthorManager;
import org.apache.aries.samples.blog.api.Blog;
import org.apache.aries.samples.blog.api.BlogAuthor;
import org.apache.aries.samples.blog.api.BlogPost;
import org.apache.aries.samples.blog.api.BlogPostManager;
import org.apache.aries.samples.blog.api.BloggingService;
import org.apache.aries.samples.blog.persistence.api.Author;



/** Implementation of the BloggingService */
public class BloggingServiceImpl implements BloggingService
{
  private BlogPostManager blogPostManager;
  private AuthorManager authorManager;
  
  // Injected via blueprint
  public void setBlogPostManager(BlogPostManager blogPostManager)
  {
    this.blogPostManager = blogPostManager;
  }
  
  // Injected via blueprint
  public void setAuthorManager(AuthorManager authorManager)
  {
    this.authorManager = authorManager;
  }

  public Blog getBlog()
  {
    return new BlogImpl(authorManager, blogPostManager);
  }

  public BlogAuthor getBlogAuthor(String email)
  {
    Author a = authorManager.getAuthor(email);
    if (a != null)
      return new BlogAuthorImpl(a, this);
    else
      return null;
  }

  public void createAuthor(String email, String nickName, String name, String bio, String dob)
  {
    try {
      authorManager.createAuthor(email, dob, name, nickName, bio);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void updateAuthor(String email, String nickName, String name, String bio, String dob)
  {
    try {
      authorManager.updateAuthor(email, dob, name, nickName, bio);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  public BlogPost getPost(long id)
  {
    return blogPostManager.getBlogPost(id);
  }
}
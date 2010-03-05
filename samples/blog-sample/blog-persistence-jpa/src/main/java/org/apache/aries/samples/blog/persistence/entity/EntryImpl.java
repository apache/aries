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
package org.apache.aries.samples.blog.persistence.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.aries.samples.blog.persistence.api.Entry;



/**
 * This class represents a blog entry
 */
@Entity(name = "BLOGENTRY")
@Table(name = "BLOGENTRY")
public class EntryImpl implements Entry
{
  /** An auto-generated primary key */

  private Long id;

  /** The author of the blog post */

  private AuthorImpl author;

  /** The date the post was published */
  private Date publishDate;
  /** The date the post was last updated */
  private Date updatedDate;
  /** The title of the post */
  private String title;
  /** Tags associated with the post */
  private List<String> tags;
  /** The text of the blog */

  private String blogText;

  /** Get the author of this blog post */
  public AuthorImpl getAuthor()
  {
    return author;
  }

  /** Set the author of this blog post */
  public void setAuthor(AuthorImpl author)
  {
    this.author = author;
  }

  /** Get the publish date of this blog post */
  public Date getPublishDate()
  {
    return publishDate;
  }

  /** Set the publish date of this blog post */
  public void setPublishDate(Date publishDate)
  {
    this.publishDate = publishDate;
  }

  /** Get the title of this blog post */
  public String getTitle()
  {
    return title;
  }

  /** Set the title of this blog post */ 
  public void setTitle(String title)
  {
    this.title = title;
  }


  /** Get the tags for this blog post */
  public List<String> getTags()
  {
    return tags;
  }

  /** Set the tags for this blog post */
  public void setTags(List<String> tags)
  {
    this.tags = tags;
  }

  /** Get the text for this blog post */
  public String getBlogText()
  {
    return blogText;
  }

  /** Set the text for this blog post */
  public void setBlogText(String blogText)
  {
    this.blogText = blogText;
  }

  /** get the Blog post id */
  public long getId()
  {
    return id;
  }

  /** Set the id */
  public void setId(Long id)
  {
    this.id = id;
  }

  /**
   * @return The date of the last update to this blog
   *         or null if it has never been modified
   */
  public Date getUpdatedDate()
  {
    return updatedDate;
  }

  /**
   * Set the date that the blog post was last updated
   * 
   * @param updatedDate
   */
  public void setUpdatedDate(Date updatedDate)
  {
    this.updatedDate = updatedDate;
  }


}


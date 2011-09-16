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
package org.apache.aries.samples.blog.comment.ejb;

import java.util.Date;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.aries.samples.blog.api.comment.persistence.Comment;
import org.apache.aries.samples.blog.api.persistence.Author;
import org.apache.aries.samples.blog.api.persistence.BlogPersistenceService;
import org.apache.aries.samples.blog.api.persistence.Entry;

@Entity(name="Comment")
public class CommentImpl implements Comment{

  @Id
  @GeneratedValue
  private int id;
  
  private String comment;
  
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationDate;
  
  //Details for author
  private String authorId;
  
  //Details for entry
  private long entryId;
  
  public CommentImpl(String comment, String authorId, long entryId) {
    this.comment = comment;
    this.authorId = authorId;
    this.entryId = entryId;
    this.creationDate = new Date();
  }

  public String getComment() {
    return comment;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public int getId() {
    return id;
  }

  public Author getAuthor() {
    try {
      BlogPersistenceService bps = (BlogPersistenceService) new InitialContext().lookup(
          "osgi:service/" + BlogPersistenceService.class.getName());
      return bps.getAuthor(authorId);
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }

  public Entry getEntry() {
    try {
      BlogPersistenceService bps = (BlogPersistenceService) new InitialContext().lookup(
          "osgi:service/" + BlogPersistenceService.class.getName());
      return bps.getBlogEntryById(entryId);
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }

  public String getAuthorId() {
    return authorId;
  }

  public void setAuthorId(String authorId) {
    this.authorId = authorId;
  }

  public long getEntryId() {
    return entryId;
  }

  public void setEntryId(long entryId) {
    this.entryId = entryId;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
}

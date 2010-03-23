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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.apache.aries.samples.blog.api.persistence.api.Author;

/**
 * This class represents a blog post Author
 */

@Entity(name = "AUTHOR")
@Table(name = "AUTHOR")
public class AuthorImpl implements Author
{
  /** The author's email address */
  @Id
  @Column(nullable = false, unique = true)
  private String email;
  
  /** The author's full name */
  private String name;
  /** The display name for this author */
  private String displayName;
  /** A short bio for this author */
  private String bio;  
  /** The Author's date of birth */
  private Date dob;

  /** The blog entries posted by this user */
  @OneToMany(cascade = {CascadeType.REMOVE}, fetch = FetchType.EAGER)
  @OrderBy("publishDate DESC")
  private List<EntryImpl> posts;

  /** Get the author's email address */
  public String getEmail()
  {
    return email;
  }

  /** Get the author's full name */
  public String getName()
  {
    return name;
  } 
  
  /** Get the author's displayed name */
  public String getDisplayName()
  {
    return displayName;
  }

  /** Get the author's biographical information */
  public String getBio()
  {
    return bio;
  }

  /** Get the author's date of birth */
  public Date getDob()
  {
    return dob;
  } 

  /** Get the author's blog posts */
  public List<EntryImpl> getEntries()
  {
    return posts;
  }
  
  // Set methods are not defined in the interface
  
  /** Set the author's email address */
  public void setEmail(String email)
  {
    this.email = email;
  }
  
  /** Set the author's full name */
  public void setName(String name)
  {
    this.name = name;
  }
  
  /** Set the author's displayed name */
  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }
  
  /** Set the author's biographical information */
  public void setBio(String bio)
  {
    this.bio = bio;
  }
  
  /** Set the author's date of birth */
  public void setDob(Date dob)
  {
    this.dob = dob;
  }

  /** Update  the author's blog posts */
  public void updateEntries(EntryImpl b)
  {
    this.posts.add(b);
  }
  
  /** set  the author's blog posts */
  public void setEntries(List<EntryImpl> lb)
  {
    this.posts = lb;
  }
  
}

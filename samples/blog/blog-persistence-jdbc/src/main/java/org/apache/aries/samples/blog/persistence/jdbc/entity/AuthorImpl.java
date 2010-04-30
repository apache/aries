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

package org.apache.aries.samples.blog.persistence.jdbc.entity;

import java.util.Date;
import java.util.List;

import org.apache.aries.samples.blog.api.persistence.Author;



/**
 * This class represents a blog post Author
 */

public class AuthorImpl implements Author
{
  /** The author's email address */


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

  private List<EntryImpl> posts;

  /** Get the author's email address */
  public String getEmail()
  {
    return email;
  }

  /** Set the author's email address */
  public void setEmail(String email)
  {
    this.email = email;
  }

  /** Get the author's full name */
  public String getName()
  {
    return name;
  }

  /** Set the author's full name */
  public void setName(String name)
  {
    this.name = name;
  }

  /** Get the author's displayed name */
  public String getDisplayName()
  {
    return displayName;
  }

  /** Set the author's displayed name */
  public void setDisplayName(String displayName)
  {
    this.displayName = displayName;
  }

  /** Get the author's biographical information */
  public String getBio()
  {
    return bio;
  }

  /** Set the author's biographical information */
  public void setBio(String bio)
  {
    this.bio = bio;
  }

  /** Get the author's date of birth */
  public Date getDob()
  {
    return dob;
  }

  /** Set the author's date of birth */
  public void setDob(Date dob)
  {
    this.dob = dob;
  }

  /** Get the author's blog posts */
  public List<EntryImpl> getEntries()
  {
    return posts;
  }

  /** Set the author's blog posts */
  public void setEntries(List<EntryImpl> posts)
  {
    this.posts = posts;
  }

}

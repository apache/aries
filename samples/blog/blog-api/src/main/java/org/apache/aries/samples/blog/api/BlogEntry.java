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
package org.apache.aries.samples.blog.api;

import java.util.Date;

public interface BlogEntry
{
  /** 
   * Get the title of the blog posting.
   * @return the title String
   */
  String getTitle();

  /** 
   * Get the body of the blog posting.
   * @return the body content as a String
   */
  String getBody();

  /** 
   * Get the author of the blog entry.
   * @return the author's display name or email address if display name is null
   */
  BlogAuthor getAuthor();

  /**
   * Get the email address of the author of the blog posting.
   * @return the author's email address
   */
  String getAuthorEmail();

  /**
   * Get the publish date of a blog posting.
   * @return the date of publish
   */
  public Date getPublishDate();

  /**
   * Get the Id value for the blog posting. 
   * @return the id value
   */
  public long getId();
  
}
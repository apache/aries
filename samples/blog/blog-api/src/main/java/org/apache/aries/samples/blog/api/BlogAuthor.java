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

import java.util.List;


public interface BlogAuthor
{
  /** Get the author's display name 
   *  @return the display name String
   */
  String getName();

  /** Get the author's full name
   *  @return the full name String
   */
  String getFullName();

  /** Get the author's email address 
   *  @return the email address String
   */
  String getEmailAddress();

  /** Get the author's biography 
   *  @return the biography String
   */
  String getBio();

  /** Get the author's date of birth 
   *  @return the date of birth String (dd-mm-yyyy) 
   */
  String getDateOfBirth();
  
  /**
   * 
   * @return a list of Blog Entries
   */
  List <? extends BlogEntry> getEntries();
}

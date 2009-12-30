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
package org.apache.aries.samples.blog.persistence.api;

import java.util.Date;
import java.util.List;


public interface Author {

	/** Get the author's email address */
	public String getEmail();

	/** Set the author's email address */
	public void setEmail(String email);

	/** Get the author's full name */
	public String getName();

	/** Set the author's full name */
	public void setName(String name);

	/** Get the author's displayed name */
	public String getDisplayName();

	/** Set the author's displayed name */
	public void setDisplayName(String displayName);

	/** Get the author's biographical information */
	public String getBio();

	/** Set the author's biographical information */
	public void setBio(String bio);

	/** Get the author's date of birth */
	public Date getDob();

	/** Set the author's date of birth */
	public void setDob(Date dob);

	/** Get the author's blog posts */
	public List<BlogEntry> getPosts();

	/** Set the author's blog posts */
	public void setPosts(List<BlogEntry> posts);

}

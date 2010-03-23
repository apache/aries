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
package org.apache.aries.samples.blog.api.persistence.api;

import java.util.Date;
import java.util.List;



public interface Entry {

	/** Get the author of this blog post */
	public Author getAuthor();

	/** Get the publish date of this blog post */
	public Date getPublishDate();
	
	/** Get the title of this blog post */
	public String getTitle();
	
	/** Get the tags for this blog post */
	public List<String> getTags();
	
	/** Get the text for this blog post */
	public String getBlogText();
	
	/** get the Blog post id */
	public long getId();

	/**
	 * @return The date of the last update to this blog or null if it has never
	 *         been modified
	 */
	public Date getUpdatedDate();

}

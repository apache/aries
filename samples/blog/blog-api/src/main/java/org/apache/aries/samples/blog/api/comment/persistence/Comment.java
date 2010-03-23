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
package org.apache.aries.samples.blog.api.comment.persistence;

import java.util.Date;

import org.apache.aries.samples.blog.api.persistence.Author;
import org.apache.aries.samples.blog.api.persistence.Entry;

public interface Comment {
	 /** Get comment 
	   *  @return the String representing the comment 
	   */
	  String getComment();
	  
	  /** Get the author of the comment 
	   *  @return the BlogAuthor instance 
	   */
	  Author getAuthor();
	  
	  /** Get the parent blog post for the comment 
	   *  @return the BlogPost instance the comment is attached to.  
	   */
	  Entry getEntry();

	  /** Get the Id value of the comment 
	   *  @return the integer id of the comment 
	   */
	  int getId();
	  
	  /** Get the creation date for the comment 
	   *  @return the String representation of the date the comment was
	   *  created in dd-mm-yyyy format. 
	   */
	  Date getCreationDate();
}

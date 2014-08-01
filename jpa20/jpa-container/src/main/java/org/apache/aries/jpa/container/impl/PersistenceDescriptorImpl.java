/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.impl;

import java.io.InputStream;

import org.apache.aries.jpa.container.parsing.PersistenceDescriptor;

/**
 * Stores the location of a persistence descriptor and
 * a stream to its contents. Note that there is only one
 * copy of the InputStream, only one thread should try to
 * read from it, and it can only be closed once. 
 */
public class PersistenceDescriptorImpl implements PersistenceDescriptor {

  /** The location of the persistence descriptor */
  private final String location;
  /** The wrapped InputStream */ 
  private final InputStream inputStream;

  /**
   * Create a PersistenceDescriptor wrapping the location and InputStream
   * @param location
   * @param inputStream
   */
  public PersistenceDescriptorImpl(String location, InputStream inputStream) {
    this.location = location;
    this.inputStream = inputStream;
  }

  /* (non-Javadoc)
   * @see org.apache.aries.jpa.container.impl.PersistenceDescriptor#getLocation()
   */
  public String getLocation() {
    return location;
  }

  /* (non-Javadoc)
   * @see org.apache.aries.jpa.container.impl.PersistenceDescriptor#getInputStream()
   */
  public InputStream getInputStream() {
    return inputStream;
  }
  
  public String toString()
  {
    if(location != null)
      return location;
    else 
      return super.toString();
  }
  
}

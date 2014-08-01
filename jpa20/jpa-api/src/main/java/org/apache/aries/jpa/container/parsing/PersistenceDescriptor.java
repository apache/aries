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
package org.apache.aries.jpa.container.parsing;

import java.io.InputStream;

/**
 * This is a utility interface that is used by the {@link PersistenceDescriptorParser}.
 *
 * This interfaces provides access to a single {@link InputStream} that returns the bytes
 * of the persistence descriptor, and a String denoting the location of the persistence
 * descriptor as present in the persistence bundle's Meta-Persistence header. 
 */
public interface PersistenceDescriptor {

  /**
   * Get the location of the persistence descriptor as it appears in the
   * Meta-Persistence header. The default location should be returned as
   * "META-INF/persistence.xml".
   * @return the location
   */
  public String getLocation();

  /**
   * Get an {@link InputStream} to the persistence descriptor. This method need not return a
   * new {@link InputStream} each time, and it is undefined for multiple clients to attempt to use
   * the {@link InputStream} from this {@link PersistenceDescriptor}. It is also undefined for a
   * client to try to retrieve multiple {@link InputStream} objects from this method.
   *
   * @return An {@link InputStream} to the persistence descriptor.
   */
  public InputStream getInputStream();

}
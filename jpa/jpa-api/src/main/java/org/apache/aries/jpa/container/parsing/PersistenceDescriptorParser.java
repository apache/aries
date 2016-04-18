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
package org.apache.aries.jpa.container.parsing;

import java.util.Collection;

import org.osgi.framework.Bundle;

/**
 * A utility service for parsing JPA persistence descriptors 
 */
public interface PersistenceDescriptorParser {

  /**
   * Parse the supplied {@link PersistenceDescriptor} 
   * 
   * @param b  The bundle that contains the persistence descriptor
   * @param descriptor The descriptor
   * 
   * @return A collection of {@link ParsedPersistenceUnit}
   * @throws PersistenceDescriptorParserException  if any error occurs in parsing
   */
  public abstract Collection<? extends ParsedPersistenceUnit> parse(Bundle b,
      PersistenceDescriptor descriptor)
      throws PersistenceDescriptorParserException;

}
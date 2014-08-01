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

/**
 * This Exception will be thrown when there was an error parsing a PersistenceDescriptor
 * It will use the standard chaining mechanism to wrap the Exception thrown by the parser. 
 */
public class PersistenceDescriptorParserException extends Exception {

  /**
   * Construct a PersistenceDescriptorException
   * @param string 
   * @param e the exception to wrap
   */
  public PersistenceDescriptorParserException(String string, Exception e) {
    super(string, e);
  }

  /**
   * Construct a PersistenceDescriptorException
   * @param string 
   */
  public PersistenceDescriptorParserException(String string) {
    super(string);
  }
  
  /**
   * For Serialization
   */
  private static final long serialVersionUID = -8960763303021136544L;

}

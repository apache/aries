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

package org.apache.aries.application.modelling;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

/** 
 * This interface is implemented by the service which proxies the
 * Apache Aries blueprint parser. ParserProxy services offer higher
 * level methods built on top of the Blueprint parser. 
 *
 */
public interface ParserProxy {

  /**
   * Parse blueprint xml files and extract the parsed ServiceMetadata objects
   * @param blueprintsToParse URLs to blueprint xml files
   * @return List of (wrapped) ServiceMetadata objects
   */
  public List<? extends WrappedServiceMetadata> parse (List<URL> blueprintsToParse) throws Exception;
  
  /**
   * Parse a blueprint xml files and extract the parsed ServiceMetadata objects
   * @param blueprintToParse URL to blueprint xml file
   * @return List of (wrapped) ServiceMetadata objects
   */
  public List<? extends WrappedServiceMetadata> parse (URL blueprintToParse) throws Exception;
  
  /**
   * Parse an InputStream containing blueprint xml and extract the parsed ServiceMetadata objects
   * @param blueprintToParse InputStream containing blueprint xml data. The caller is responsible
   * for closing the stream afterwards. 
   * @return List of (wrapped) ServiceMetadata objects
   */
  public List<? extends WrappedServiceMetadata> parse (InputStream blueprintToParse) throws Exception;
  
  /**
   * Parse an InputStream containing blueprint xml and extract Service, Reference and RefList
   * elements.
   * @return All parsed Service, Reference and RefList elements 
   */
  public ParsedServiceElements parseAllServiceElements (InputStream blueprintToParse) throws Exception;
  
}

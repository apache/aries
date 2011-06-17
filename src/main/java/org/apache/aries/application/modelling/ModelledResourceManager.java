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

import java.io.IOException;
import java.io.InputStream;

import org.apache.aries.util.filesystem.IDirectory;

public interface ModelledResourceManager
{
  /**
   * Utility interface for re-presenting a source of InputStreams
   */
  interface InputStreamProvider {
    /** 
     * Return a fresh input stream
     */
    InputStream open() throws IOException;
  }
    
  /**
   * Obtain a ModelledResource object.
   * @param uri The URI to the conceptual location of the bundle, that will be returned from the getLocation method
   * on {@link ModelledResource}
   * @param bundle the bundle file
   * @return the modelled resource.
   */    
  ModelledResource getModelledResource(String uri, IDirectory bundle) throws ModellerException;
  
  /**
   * Obtain a ModelledResource object. 
   * 
   * This method is equivalent to calling <pre>getModelledResource(bundle.toURL().toURI().toString(), bundle)</pre>
   * @param bundle the bundle file
   * @return the modelled resource.
   */    
  ModelledResource getModelledResource(IDirectory bundle) throws ModellerException;
  
  /**
   * Obtain a ModelledResource via InputStreams
   * 
   * 
   * @param uri The URI to the conceptual location of the bundle, that will be returned from the getLocation method
   * on {@link ModelledResource}
   * @param bundle The bundle
   * @return
   * @throws ModellerException
   */
  ModelledResource getModelledResource(String uri, InputStreamProvider bundle) throws ModellerException;

  /**
   * Parse service and reference elements of a bundle
   * @param archive
   * @return
   * @throws ModellerException
   */
  ParsedServiceElements getServiceElements (IDirectory archive) throws ModellerException; 

  /**
   * Parse service and reference elements of a bundle
   * @param archive
   * @return
   * @throws ModellerException
   */
  ParsedServiceElements getServiceElements (InputStreamProvider archive) throws ModellerException;   
}

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
package org.apache.aries.application; 

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import org.osgi.framework.Version;

/**
 * this interface describes the Application.mf file
 *
 */
public interface ApplicationMetadata
{
  /**
   * get the value of the Application-SymbolicName header
   * @return the value of the Application-SymbolicName header
   */
  public String getApplicationSymbolicName();
  
  /**
   * get the value of the Application-Version header
   * @return the value of the Application-Version header
   */
  public Version getApplicationVersion();
  
  /**
   * get the name of the application
   * @return the name of the application
   */
  public String getApplicationName();
  /**
   * get the list of Application contents includes bundle name, 
   * version, directives and attributes
   * @return the list of the Application contents 
   */
  public List<Content> getApplicationContents();
  
  /**
   * get the value of the Export-Service header
   * @return the list of ServiceDeclaration
   */
  public List<ServiceDeclaration> getApplicationExportServices();
  
  /**
   * get the value of the Import-Service header
   * @return the list of ServiceDeclaration
   */
  public List<ServiceDeclaration> getApplicationImportServices();  
  
  /**
   * get the value of the Application-Scope, which is 
   * calculated from Application-SymbolicName and Application-Version
   * @return    the value of the AppScope
   */
  public String getApplicationScope();
  
  /** Stores any changes to disk using this implementation's storage form */
  public void store(File f);
  public void store(OutputStream out);
}

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
package org.apache.aries.application.modelling.impl;


import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple data structure containing two immutable Collections, 
 * one each of ImportedServiceImpl and ExportedServiceImpl
 */
public final class ParsedServiceElementsImpl implements ParsedServiceElements
{
 
  private final Logger logger = LoggerFactory.getLogger(ParsedServiceElementsImpl.class);
  private final Set<ExportedService> _services;
  private final Set<ImportedService> _references;
  
  /**
   * Copy collections of Service and Reference metadata into a ParsedServiceElementsImpl
   * @param services
   * @param references
   */
  public ParsedServiceElementsImpl ( Collection<ExportedService> services, 
      Collection<ImportedService> references) { 
    logger.debug(LOG_ENTRY, "ParsedServiceElementsImpl", new Object[]{services, references});
    _services = new HashSet<ExportedService>(services);
    _references = new HashSet<ImportedService>(references);
    logger.debug(LOG_ENTRY, "ParsedServiceElementsImpl");
  }

  /**
   * Get the ImportedServices
   * @return imported services
   */
  public Collection<ImportedService> getReferences() {
    logger.debug(LOG_ENTRY, "getReferences");
    logger.debug(LOG_EXIT, "getReferences", _references);
    return Collections.unmodifiableCollection(_references);
  }

  /**
   * Get the exported services
   * @return exported services
   */
  public Collection<ExportedService> getServices() {
    logger.debug(LOG_ENTRY, "getServices");
    logger.debug(LOG_EXIT, "getServices", _services);
    return Collections.unmodifiableCollection(_services);
  }
}

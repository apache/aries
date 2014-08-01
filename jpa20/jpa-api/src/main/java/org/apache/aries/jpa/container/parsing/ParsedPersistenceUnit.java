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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;

/**
 * This interface provides access to the information defined by a 
 * persistence unit in a persistence descriptor.
 * 
 * Implementations of this interface will be returned by calls to
 * {@link PersistenceDescriptorParser}.
 */
public interface ParsedPersistenceUnit {
  /*
   * Keys for use in the PersistenceXml Map
   * Stored values are Strings unless otherwise specified, and all values
   * other than the schema version and unit name may be null. A null value
   * indicates that the element/attribute was not present in the xml.
   */
  
  /** The version of the JPA schema being used */
  public static final String SCHEMA_VERSION = "org.apache.aries.jpa.schema.version";
  /** The name of the persistence unit */
  public static final String UNIT_NAME = "org.apache.aries.jpa.unit.name";
  /** The Transaction type of the persistence unit */
  public static final String TRANSACTION_TYPE = "org.apache.aries.jpa.transaction.type";
  /** A {@link List} of {@link String} mapping file names */
  public static final String MAPPING_FILES = "org.apache.aries.jpa.mapping.files";
  /** A {@link List} of {@link String} jar file names */
  public static final String JAR_FILES = "org.apache.aries.jpa.jar.files";
  /** A {@link List} of {@link String} managed class names */
  public static final String MANAGED_CLASSES = "org.apache.aries.jpa.managed.classes";
  /** A {@link Properties} object containing the properties from the persistence unit */
  public static final String PROPERTIES = "org.apache.aries.jpa.properties";
  /** The provider class name */
  public static final String PROVIDER_CLASSNAME = "org.apache.aries.jpa.provider";
  /** The jta-datasource name */
  public static final String JTA_DATASOURCE = "org.apache.aries.jpa.jta.datasource";
  /** The non-jta-datasource name */
  public static final String NON_JTA_DATASOURCE = "org.apache.aries.jpa.non.jta.datasource";
  /** A {@link Boolean} indicating whether unlisted classes should be excluded */
  public static final String EXCLUDE_UNLISTED_CLASSES = "org.apache.aries.jpa.exclude.unlisted";
  
  /* JPA 2 extensions */
  
  /** The caching type of the persistence unit. This will only be available for JPA2 persistence units. */
  public static final String SHARED_CACHE_MODE = "org.apache.aries.jpa2.shared.cache.mode";
  /** The validation mode of the persistence unit. This will only be available for JPA2 persistence units. */
  public static final String VALIDATION_MODE = "org.apache.aries.jpa2.validation.mode";
  
  
  /* End of Map keys */
  
  /** This property is used in the JPA properties to indicate a provider version range */
  public static final String JPA_PROVIDER_VERSION = "org.apache.aries.jpa.provider.version";

  /**
   * Return the persistence bundle that defines this persistence unit
   * @return the defining bundle
   */
  public Bundle getDefiningBundle();

  /**
   * Returns a deep copy of the persistence metadata, modifications to the
   * returned {@link Map} will not be reflected in future calls. 
   * @return the metadata
   */
  public Map<String, Object> getPersistenceXmlMetadata();
}

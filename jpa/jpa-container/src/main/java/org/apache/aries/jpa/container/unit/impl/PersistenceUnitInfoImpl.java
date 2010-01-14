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
package org.apache.aries.jpa.container.unit.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;

public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
  
  private final Bundle bundle;

  private final ParsedPersistenceUnit unit;
  
  private final BundleDelegatingClassLoader cl;
  
  public PersistenceUnitInfoImpl (Bundle b, ParsedPersistenceUnit parsedData)
  {
    bundle = b;
    unit = parsedData;
    cl = new BundleDelegatingClassLoader(b);
  }
  
  public void addTransformer(ClassTransformer arg0) {
    // TODO Auto-generated method stub
  }

  public boolean excludeUnlistedClasses() {
    Boolean result = (Boolean) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES);
    return (result == null) ? false : result;
  }

  public ClassLoader getClassLoader() {
    return cl;
  }

  @SuppressWarnings("unchecked")
  public List<URL> getJarFileUrls() {
    List<String> jarFiles = (List<String>) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES);
    List<URL> urls = new ArrayList<URL>();
    
    for(String jarFile : jarFiles)
      urls.add(bundle.getResource(jarFile));
    
    return urls;
  }

  public DataSource getJtaDataSource() {
    String jndiString = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE);
    DataSource toReturn = null;
    try {
      InitialContext ctx = new InitialContext();
      toReturn = (DataSource) ctx.lookup(jndiString);
    } catch (NamingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return toReturn;
  }

  @SuppressWarnings("unchecked")
  public List<String> getManagedClassNames() {
    return (List<String>) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES);
  }

  @SuppressWarnings("unchecked")
  public List<String> getMappingFileNames() {
    return (List<String>) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES);
  }

  public ClassLoader getNewTempClassLoader() {
    return new TempBundleDelegatingClassLoader(bundle);
  }

  public DataSource getNonJtaDataSource() {
    
    String jndiString = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE);
    DataSource toReturn = null;
    try {
      InitialContext ctx = new InitialContext();
      toReturn = (DataSource) ctx.lookup(jndiString);
    } catch (NamingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return toReturn;
  }

  public String getPersistenceProviderClassName() {
    return (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROVIDER_CLASSNAME);
  }

  public String getPersistenceUnitName() {
    return (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.UNIT_NAME);
  }

  public URL getPersistenceUnitRootUrl() {
    return bundle.getResource("/");
  }

  public String getPersistenceXMLSchemaVersion() {
    return (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SCHEMA_VERSION);
  }

  public Properties getProperties() {
    return (Properties) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES);
  }

  public SharedCacheMode getSharedCacheMode() {
    // TODO Auto-generated method stub
    return null;
  }

  public PersistenceUnitTransactionType getTransactionType() {
    return PersistenceUnitTransactionType.valueOf(
        (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.TRANSACTION_TYPE));
  }

  public ValidationMode getValidationMode() {
    // TODO Auto-generated method stub
    return null;
  }
  
}
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
import java.util.Collections;
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
import org.apache.aries.jpa.transformer.TransformerAgent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
  
  private final Bundle bundle;

  private final ParsedPersistenceUnit unit;
  
  private final BundleDelegatingClassLoader cl;
  
  private final ServiceReference providerRef;
  
  private final TransformerAgent agent;
    
  private final List<TransformerWrapper> transformers;
  
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  
  public PersistenceUnitInfoImpl(Bundle bundle, 
                                 ParsedPersistenceUnit parsedData, 
                                 ServiceReference providerRef,
                                 TransformerAgent agent)
  {
    this.bundle = bundle;
    unit = parsedData;
    this.providerRef = providerRef;
    this.agent = agent;
    this.transformers = new ArrayList<TransformerWrapper>();
    cl = new BundleDelegatingClassLoader(bundle);
  }
  
  public void addTransformer(ClassTransformer classTransformer) {
      if (agent != null) {
          TransformerWrapper transformer = new TransformerWrapper(classTransformer, bundle);
          transformers.add(transformer);
          agent.addTransformer(transformer);
      }
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
    if(jarFiles != null) {
      for(String jarFile : jarFiles){
        URL url = bundle.getResource(jarFile);
        if(url == null) {
          _logger.error("The persistence unit {} in bundle {} listed the jar file {}, but " +
          		"{} could not be found in the bundle", new Object[]{getPersistenceUnitName(),
              bundle.getSymbolicName() + "_" + bundle.getVersion(), jarFile, jarFile});
        } else {
            urls.add(url);
        }
      }
    }
    return urls;
  }

  public DataSource getJtaDataSource() {
    String jndiString = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JTA_DATASOURCE);
    DataSource toReturn = null;
    if(jndiString != null) {
      try {
        InitialContext ctx = new InitialContext();
        toReturn = (DataSource) ctx.lookup(jndiString);
      } catch (NamingException e) {
        _logger.error("No JTA datasource could be located using the JNDI name " + jndiString,
            e);
      }
    }
    return toReturn;
  }

  @SuppressWarnings("unchecked")
  public List<String> getManagedClassNames() {
    List<String> classes = (List<String>) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES);
    if(classes == null)
      classes = new ArrayList<String>();
    
    return Collections.unmodifiableList(classes);
  }

  @SuppressWarnings("unchecked")
  public List<String> getMappingFileNames() {
    List<String> mappingFiles = (List<String>) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MAPPING_FILES);
    if(mappingFiles == null)
      mappingFiles = new ArrayList<String>();
    
    return Collections.unmodifiableList(mappingFiles);
  }

  public ClassLoader getNewTempClassLoader() {
    return new TempBundleDelegatingClassLoader(bundle, new BundleDelegatingClassLoader(providerRef.getBundle()));
  }

  public DataSource getNonJtaDataSource() {
    
    String jndiString = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE);
    DataSource toReturn = null;
    if(jndiString != null) {
      try {
        InitialContext ctx = new InitialContext();
        toReturn = (DataSource) ctx.lookup(jndiString);
      } catch (NamingException e) {
        _logger.error("No Non JTA datasource could be located using the JNDI name " + jndiString,
            e);
      }
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
    String s = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.SHARED_CACHE_MODE);
    
    if (s == null)
      return SharedCacheMode.UNSPECIFIED;
    else
      return SharedCacheMode.valueOf(s);
  }

  public PersistenceUnitTransactionType getTransactionType() {
    
    String s = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.TRANSACTION_TYPE);

    if(s == null)
      return PersistenceUnitTransactionType.JTA;
    else
      return PersistenceUnitTransactionType.valueOf(s);
  }

  public ValidationMode getValidationMode() {
    String s = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.VALIDATION_MODE);
    
    if (s == null)
      return ValidationMode.AUTO;
    else
      return ValidationMode.valueOf(s);

  }
  
  public void destroy() {  
      if (agent != null) {
          for (TransformerWrapper transformer : transformers) {
              agent.removeTransformer(transformer);
          }
          transformers.clear();
      }
  }
  
}

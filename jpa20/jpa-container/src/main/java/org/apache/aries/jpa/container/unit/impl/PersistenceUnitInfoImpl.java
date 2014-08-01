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
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.annotation.impl.AnnotationScanner;
import org.apache.aries.jpa.container.annotation.impl.AnnotationScannerFactory;
import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.jpa.container.weaving.impl.TransformerRegistry;
import org.apache.aries.jpa.container.weaving.impl.TransformerRegistryFactory;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
  
  private final Bundle bundle;

  private final ParsedPersistenceUnit unit;
  
  private final ServiceReference providerRef;
  
  private final Boolean useDataSourceFactory;
  
  private ClassTransformer transformer;
  
  private final AtomicReference<DataSourceFactoryDataSource> jtaDSFDS = 
    new AtomicReference<DataSourceFactoryDataSource>();
  
  private final AtomicReference<DataSourceFactoryDataSource> nonJtaDSFDS = 
    new AtomicReference<DataSourceFactoryDataSource>();
  
  // initialize it lazily because we create a PersistenceUnitInfoImpl when the bundle is INSTALLED state
  private final AtomicReference<ClassLoader> cl = new AtomicReference<ClassLoader>();
  
  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  private static final String JDBC_PREFIX = "javax.persistence.jdbc.";
  
  public PersistenceUnitInfoImpl (Bundle b, ParsedPersistenceUnit parsedData, 
      final ServiceReference providerRef, Boolean globalUsedatasourcefactory)
  {
    bundle = b;
    unit = parsedData;
    this.providerRef = providerRef;
    //Local override for global DataSourceFactory usage
    Boolean localUseDataSourceFactory = Boolean.parseBoolean(getInternalProperties().getProperty(
        PersistenceUnitConstants.USE_DATA_SOURCE_FACTORY, "true"));
    
    this.useDataSourceFactory = globalUsedatasourcefactory && localUseDataSourceFactory;
  }
  
  public synchronized void addTransformer(ClassTransformer arg0) {
    TransformerRegistry reg = TransformerRegistryFactory.getTransformerRegistry();
    if(reg != null) {
      reg.addTransformer(bundle, arg0, providerRef);
      transformer = arg0;
    }
  }

  public boolean internalExcludeUnlistedClasses() {
    Boolean result = (Boolean) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES);
    return (result == null) ? false : result;
  }
  
  public boolean excludeUnlistedClasses() {
    return true;
  }

  public ClassLoader getClassLoader() {
    if (cl.get() == null) {
        // use forced because for even for a resolved bundle we could otherwise get null
        cl.compareAndSet(null, AriesFrameworkUtil.getClassLoaderForced(bundle));
    }
    
    return cl.get();
  }

  @SuppressWarnings("unchecked")
  public List<URL> getJarFileUrls() {
    List<String> jarFiles = (List<String>) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.JAR_FILES);
    List<URL> urls = new ArrayList<URL>();
    if(jarFiles != null) {
      for(String jarFile : jarFiles){
        URL url = bundle.getResource(jarFile);
        if(url == null) {
          _logger.error(NLS.MESSAGES.getMessage("pu.not.found", getPersistenceUnitName(), bundle.getSymbolicName(), bundle.getVersion(), jarFile));
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
      toReturn = new JndiDataSource(jndiString, getPersistenceUnitName(), bundle, 
          getTransactionType() == PersistenceUnitTransactionType.JTA);
    } else if(useDataSourceFactory) {
      toReturn = jtaDSFDS.get();
      if(toReturn == null) {
        Properties props = getInternalProperties();
        String driverName = props.getProperty("javax.persistence.jdbc.driver");
        if(driverName != null) {
          if(_logger.isDebugEnabled())
            _logger.debug(NLS.MESSAGES.getMessage("using.datasource.factory", getPersistenceUnitName(),
                bundle.getSymbolicName(), bundle.getVersion()));
          
          boolean jta = getTransactionType() == PersistenceUnitTransactionType.JTA;
          jtaDSFDS.compareAndSet(null, new DataSourceFactoryDataSource(bundle, driverName, getDsProps(props), jta)); 
          toReturn = jtaDSFDS.get();
        }
      }
    }
    return toReturn;
  }


  @SuppressWarnings("unchecked")
  public List<String> getManagedClassNames() {
    List<String> classes = (List<String>) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.MANAGED_CLASSES);
    if(classes == null)
      classes = new ArrayList<String>();
    if(!!!internalExcludeUnlistedClasses()) {
      AnnotationScanner scanner = AnnotationScannerFactory.getAnnotationScanner();
      if(scanner != null)
        classes.addAll(scanner.findJPAAnnotatedClasses(bundle));
    }
    
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
    ClassLoader cl = AriesFrameworkUtil.getClassLoader(providerRef.getBundle());
    return new TempBundleDelegatingClassLoader(bundle, cl);
  }

  public DataSource getNonJtaDataSource() {
    
    String jndiString = (String) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.NON_JTA_DATASOURCE);
    DataSource toReturn = null;
    if(jndiString != null) {
      toReturn = new JndiDataSource(jndiString, getPersistenceUnitName(), bundle, false);
    } else if(useDataSourceFactory) {
      toReturn = nonJtaDSFDS.get();
      if(toReturn == null) {
        Properties props = getInternalProperties();
        String driverName = props.getProperty("javax.persistence.jdbc.driver");
        if(driverName != null) {
          if(_logger.isDebugEnabled())
            _logger.debug(NLS.MESSAGES.getMessage("using.datasource.factory", getPersistenceUnitName(),
                bundle.getSymbolicName(), bundle.getVersion()));
          
          nonJtaDSFDS.compareAndSet(null, new DataSourceFactoryDataSource(bundle, driverName, getDsProps(props), false));
          toReturn = nonJtaDSFDS.get();
        }
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

  private Properties getInternalProperties() {
    return (Properties) unit.getPersistenceXmlMetadata().get(ParsedPersistenceUnit.PROPERTIES);
  }
  
  public Properties getProperties() {
    Properties p = new Properties();
    p.putAll(getInternalProperties());
    
    String jdbcClass = p.getProperty("javax.persistence.jdbc.driver");
    if(useDataSourceFactory && jdbcClass != null) {
      p.setProperty(PersistenceUnitConstants.DATA_SOURCE_FACTORY_CLASS_NAME, 
          jdbcClass);
      p.remove("javax.persistence.jdbc.driver");
    }
    return p;
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

  public synchronized void clearUp() {
    if(transformer != null) {
      TransformerRegistry reg = TransformerRegistryFactory.getTransformerRegistry();
      reg.removeTransformer(bundle, transformer);
      transformer = null;
    }
  }
  
  public void unregistered() {
    DataSourceFactoryDataSource dsfds = jtaDSFDS.get();
    if(dsfds != null) {
      dsfds.closeTrackers();
    }
    
    dsfds = nonJtaDSFDS.get();
    if(dsfds != null) {
      dsfds.closeTrackers();
    }
  }
  
  /**
   * Return all properties that start with the prefix JDBC_PREFIX and cut off that prefix.
   * 
   * @param props
   * @return
   */
  private Properties getDsProps(Properties props) {
	  Properties dsProps = new Properties();
	  for (Object keyO : props.keySet()) {
		  String key = (String)keyO;
		  if (key.startsWith(JDBC_PREFIX) && !key.equals(JDBC_PREFIX + "driver")) {
			  dsProps.put(key.substring(JDBC_PREFIX.length()), props.get(key));
		  }
	  }
	  return dsProps;
  }

  
}
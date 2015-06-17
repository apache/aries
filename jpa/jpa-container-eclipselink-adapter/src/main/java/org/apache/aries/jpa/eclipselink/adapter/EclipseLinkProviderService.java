/*
// * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.aries.jpa.eclipselink.adapter;

import org.apache.aries.jpa.eclipselink.adapter.platform.OSGiTSServer;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

/**
 * Service factory for generating the Eclipselink OSGi compatible provider. It proxies the provider so that
 * we can go in at entity manager creation time and set the eclipselink target-server to be {@link OSGiTSServer}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EclipseLinkProviderService implements ServiceFactory {
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);
  
  private final Bundle eclipseLinkJpaBundle;
    
  public EclipseLinkProviderService(Bundle b) {
      eclipseLinkJpaBundle = b;
  }
  
  @Override
  public Object getService(Bundle bundle, ServiceRegistration registration) {
    logger.debug("Requested EclipseLink Provider service");
    
    try {
      Class<? extends PersistenceProvider> providerClass = (Class<? extends PersistenceProvider>) eclipseLinkJpaBundle.loadClass(Activator.ECLIPSELINK_JPA_PROVIDER_CLASS_NAME);
      Constructor<? extends PersistenceProvider> con = providerClass.getConstructor();
      final PersistenceProvider provider = con.newInstance();
      
      return new PersistenceProvider() {
        public ProviderUtil getProviderUtil() {
          return provider.getProviderUtil();
        }
        
        public EntityManagerFactory createEntityManagerFactory(String arg0, Map arg1) {
          return provider.createEntityManagerFactory(arg0, arg1);
        }
        
        public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo punit, Map props) {
          return provider.createContainerEntityManagerFactory(new PersistenceUnitProxyWithTargetServer(punit, 
                  eclipseLinkJpaBundle), props);
        }
        
        @Override
        public void generateSchema(PersistenceUnitInfo punit, Map arg1) {
          provider.generateSchema(new PersistenceUnitProxyWithTargetServer(punit, 
              eclipseLinkJpaBundle), arg1);
        }

        @Override
        public boolean generateSchema(String arg0, Map arg1) {
          return provider.generateSchema(arg0, arg1);
        }
      };
      
    } catch (Exception e) {
        logger.error("An exception was caught trying to instantiate the EclipseLink JPA provider.", e);
        return null;                
    }
  }

  public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {}
}
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

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.util.tracker.SingleServiceTracker;
import org.apache.aries.util.tracker.SingleServiceTracker.SingleServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceFactoryDataSource extends DelayedLookupDataSource implements SingleServiceListener {

  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  
  private AtomicReference<DataSource> ds = new AtomicReference<DataSource>();
  
  private final String driverName;
  private final Bundle persistenceBundle;
  private final Properties props;
  private final boolean jta;
  
  private final AtomicReference<SingleServiceTracker<DataSourceFactory>> trackerRef =
    new AtomicReference<SingleServiceTracker<DataSourceFactory>>();
  
  public DataSourceFactoryDataSource(Bundle bundle, String driverName, String dbURL, 
      String dbUserName, String dbPassword, boolean jta) {
    this.persistenceBundle = bundle;
    this.driverName = driverName;
    props = new Properties();
    if(dbURL != null)
      props.setProperty(DataSourceFactory.JDBC_URL, dbURL);
    if(dbUserName != null)
      props.setProperty(DataSourceFactory.JDBC_USER, dbUserName);
    if(dbPassword != null)
      props.setProperty(DataSourceFactory.JDBC_PASSWORD, dbPassword);
    
    this.jta = jta;
  }

  @Override
  protected DataSource getDs() {
    if(ds.get() == null) {
      SingleServiceTracker<DataSourceFactory> tracker = trackerRef.get();
      
      if(tracker == null) {
        try {
          tracker = new SingleServiceTracker<DataSourceFactory>(
              persistenceBundle.getBundleContext(), DataSourceFactory.class, "(" +
              DataSourceFactory.OSGI_JDBC_DRIVER_CLASS + "=" + driverName + ")", this);
        } catch (InvalidSyntaxException ise) {
          //This should never happen
          throw new RuntimeException(ise);
        }
        if(trackerRef.compareAndSet(null, tracker))
          tracker.open();
        else 
          tracker = trackerRef.get(); 
      } 
      
      DataSourceFactory dsf = tracker.getService();
      if(dsf != null) {
        try {
          if(jta) {
            ds.compareAndSet(null, wrapXADataSource(dsf.createXADataSource(props)));
          } else {
            ds.compareAndSet(null, dsf.createDataSource(props));
          }
        } catch (SQLException e) {
          String message = NLS.MESSAGES.getMessage("datasourcefactory.sql.exception", driverName, props, 
              persistenceBundle.getSymbolicName(), persistenceBundle.getVersion());
          _logger.error(message, e);
          throw new RuntimeException(message, e);
        }
      } else {
        _logger.error(NLS.MESSAGES.getMessage("no.datasource.factory", driverName, props, 
            persistenceBundle.getSymbolicName(), persistenceBundle.getVersion()));
      }
    }
    return ds.get();
  }

  public void closeTrackers() {
    SingleServiceTracker<DataSourceFactory> tracker = trackerRef.getAndSet(null);
    if(tracker != null) {
      tracker.close();
    }
  }

  @Override
  public void serviceFound() {
    //No op
  }

  @Override
  public void serviceLost() {
    ds.set(null);
  }

  @Override
  public void serviceReplaced() {
    ds.set(null);
  }

  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return null;
  }

}

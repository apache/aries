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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelayedLookupDataSource implements DataSource {

  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  
  private DataSource ds = null;
  
  private DataSource getDs() {
    if(ds == null) {
      try {
        InitialContext ctx = new InitialContext();
        ds = (DataSource) ctx.lookup(jndiName);
      } catch (NamingException e) {
        _logger.error("No JTA datasource could be located using the JNDI name " + jndiName,
            e);
        throw new RuntimeException("The DataSource " + jndiName + " could not be used.", e);
      }
    }
    return ds;
  }

  private final String jndiName;
  
  public DelayedLookupDataSource (String jndi) {
    jndiName = jndi;
  }
  
  public Connection getConnection() throws SQLException {
    return getDs().getConnection();
  }

  public Connection getConnection(String theUsername, String thePassword)
      throws SQLException {
    return getDs().getConnection(theUsername, thePassword);
  }

  public int getLoginTimeout() throws SQLException {
    return getDs().getLoginTimeout();
  }

  public PrintWriter getLogWriter() throws SQLException {
    return getDs().getLogWriter();
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return getDs().isWrapperFor(iface);
  }

  public void setLoginTimeout(int seconds) throws SQLException {
    getDs().setLoginTimeout(seconds);
  }

  public void setLogWriter(PrintWriter out) throws SQLException {
    getDs().setLogWriter(out);
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    return getDs().unwrap(iface);
  }
}

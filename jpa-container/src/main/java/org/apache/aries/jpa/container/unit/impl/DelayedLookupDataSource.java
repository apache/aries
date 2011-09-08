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

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.jpa.container.tx.impl.XADatasourceEnlistingWrapper;

public abstract class DelayedLookupDataSource implements DataSource {

  protected abstract DataSource getDs();

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
  
  protected DataSource wrapXADataSource(XADataSource xaDs) throws IllegalStateException {
    boolean b;
    try {
      Class.forName("javax.transaction.TransactionManager");
      b = true;
    } catch (ClassNotFoundException cnfe) {
      b = false;
    }
    
    if(!b)
      throw new IllegalStateException(NLS.MESSAGES.getMessage("no.xa.wrapping"));
    
    return new XADatasourceEnlistingWrapper(xaDs);
  }
}

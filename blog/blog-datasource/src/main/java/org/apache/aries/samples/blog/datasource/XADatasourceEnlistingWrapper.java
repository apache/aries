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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.blog.datasource;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

/**
 * This class allows JDBC XA data sources to participate in global transactions,
 * via the {@link ConnectionWrapper} that is returned. The only service provided
 * is enlistment/delistment of the associated {@link XAResource} in transactions.
 * Important consideration such as connection pooling and error handling are
 * completely ignored.
 *
 */
public class XADatasourceEnlistingWrapper implements DataSource, Serializable {
    /** The serial version UID */
    private static final long serialVersionUID = -3200389791205501228L;

    private XADataSource wrappedDS;
    
    private transient TransactionManager tm;
    
    public Connection getConnection() throws SQLException
    {
      XAConnection xaConn = wrappedDS.getXAConnection();
      Connection conn = getEnlistedConnection(xaConn);
      
      return conn;
    }

    public Connection getConnection(String username, String password) throws SQLException
    {
      XAConnection xaConn = wrappedDS.getXAConnection(username, password);
      Connection conn = getEnlistedConnection(xaConn);
      
      return conn;
    }

    public PrintWriter getLogWriter() throws SQLException
    {
      return wrappedDS.getLogWriter();
    }

    public int getLoginTimeout() throws SQLException
    {
      return wrappedDS.getLoginTimeout();
    }

    public void setLogWriter(PrintWriter out) throws SQLException
    {
      wrappedDS.setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException
    {
      wrappedDS.setLoginTimeout(seconds);
    }

    private Connection getEnlistedConnection(XAConnection xaConn) throws SQLException
    {
        return new ConnectionWrapper(xaConn, tm);
    }

    public void setDataSource(XADataSource dsToWrap)
    {
      wrappedDS = dsToWrap;
    }


    public void setTxManager(TransactionManager txMgr)
    {
      tm = txMgr;
    }
    
    @Override
    public boolean equals(Object other)
    {
      if (other == this) return true;
      if (other == null) return false;
      
      if (other.getClass() == this.getClass()) {
        return wrappedDS.equals(((XADatasourceEnlistingWrapper)other).wrappedDS);
      }
      
      return false;
    }
    
    @Override
    public int hashCode()
    {
      return wrappedDS.hashCode();
    }

    public boolean isWrapperFor(Class<?> arg0) throws SQLException
    {
      return false;
    }

    public <T> T unwrap(Class<T> arg0) throws SQLException
    {
      return null;
    }
}

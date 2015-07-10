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
package org.apache.aries.transaction.test.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.aries.transaction.test.TestBean;

public class TestBeanImpl implements TestBean {
    private Connector connector;
    private TestBean bean;

    public TestBeanImpl() {
    }

    public void insertRow(String name, int value) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = connector.connect();
            stmt = conn.prepareStatement("INSERT INTO TESTTABLE VALUES (?, ?)");
            stmt.setString(1, name);
            stmt.setInt(2, value);
            stmt.executeUpdate();
        }
        finally {
            connector.safeClose(stmt);
            connector.safeClose(conn);
        }
    }

    public void insertRow(String name, int value, Exception e) throws SQLException {
        insertRow(name, value);
        
        if (e instanceof SQLException)
            throw (SQLException) e;
        else if (e instanceof RuntimeException)
            throw (RuntimeException) e;
    }

    public void delegateInsertRow(String name, int value) throws SQLException {
        bean.insertRow(name, value);
    }

    public int countRows() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int count = -1;

        try {
            conn = connector.connect();
            stmt = conn.prepareStatement("SELECT * FROM TESTTABLE", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery();
            rs.last();
            count = rs.getRow();
        }
        finally {
            if (rs != null)
                rs.close();
            connector.safeClose(stmt);
            connector.safeClose(conn);
        }

        return count;
    }

    public void throwApplicationException() throws SQLException {
        throw new SQLException("Test exception");
    }

    public void throwRuntimeException() {
        throw new RuntimeException("Test exception");
    }

    public void setTestBean(TestBean bean) {
        this.bean = bean;
    }
    
    public void setConnector(Connector connector) {
        this.connector = connector;
    }

}

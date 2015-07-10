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
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.aries.transaction.test.TestBean;

public class TestBeanImpl implements TestBean {
    private DataSource xads;
    private DataSource ds;
    private String user;
    private String password;
    private TestBean bean;

    public TestBeanImpl() {
    }

    public void initialize() {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = ds.getConnection(user, password);
            conn.setAutoCommit(true);
            stmt = conn.createStatement();
            stmt.executeUpdate("DROP TABLE TESTTABLE");
        }
        catch (Exception e) {
            // Ignore
        }
        finally {
            safeClose(stmt);
            safeClose(conn);
        }

        try {
            conn = ds.getConnection(user, password);
            conn.setAutoCommit(true);
            stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE TESTTABLE (NAME VARCHAR(64), VALUE INTEGER, PRIMARY KEY(NAME, VALUE))");
        }
        catch (Exception e) {
            // Ignore
        }
        finally {
            safeClose(stmt);
            safeClose(conn);
        }
    }



    public void insertRow(String name, int value) throws SQLException {
        insertRow(name, value, false);
    }

    public void insertRow(String name, int value, Exception e) throws SQLException {
        insertRow(name, value, false);
        
        if (e instanceof SQLException)
            throw (SQLException) e;
        else if (e instanceof RuntimeException)
            throw (RuntimeException) e;
    }

    public void insertRow(String name, int value, boolean delegate) throws SQLException {
        if (delegate) {
            bean.insertRow(name, value);
        }
        else {
            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                conn = xads.getConnection(user, password);
                stmt = conn.prepareStatement("INSERT INTO TESTTABLE VALUES (?, ?)");
                stmt.setString(1, name);
                stmt.setInt(2, value);
                stmt.executeUpdate();
            }
            finally {
                safeClose(stmt);
                safeClose(conn);
            }
        }
    }

    public int countRows() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int count = -1;

        try {
            conn = ds.getConnection(user, password);
            stmt = conn.prepareStatement("SELECT * FROM TESTTABLE", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery();
            rs.last();
            count = rs.getRow();
        }
        finally {
            if (rs != null)
                rs.close();
            safeClose(stmt);
            safeClose(conn);
        }

        return count;
    }

    public void throwApplicationException() throws SQLException {
        throw new SQLException("Test exception");
    }

    public void throwRuntimeException() {
        throw new RuntimeException("Test exception");
    }

    public void setEnlistingDataSource(DataSource xads) {
        this.xads = xads;
    }

    public void setDataSource(DataSource ds) {
        this.ds = ds;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setTestBean(TestBean bean) {
        this.bean = bean;
    }
    
    private void safeClose(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException e) {
            // Ignore
        }
    }

    private void safeClose(Statement stmt) {
        if (stmt == null) {
            return;
        }
        try {
            stmt.close();
        } catch (SQLException e) {
            // Ignore
        }
    }
}

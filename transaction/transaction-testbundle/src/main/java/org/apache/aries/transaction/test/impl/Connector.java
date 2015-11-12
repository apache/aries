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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.aries.transaction.test.Counter;

public class Connector implements Counter {
    private DataSource xads;
    private String user;
    private String password;
    private Connection conn;

    public void setXads(DataSource xads) {
        this.xads = xads;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private Connection connect() throws SQLException {
        return xads.getConnection(user, password);
    }
    
    public Connection getConn() {
        return conn;
    }

    public void initialize() throws SQLException {
        conn = connect();
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet rs = dbmd.getTables(null, "", "TESTTABLE", null);
        if (!rs.next()) {
            executeUpdate("CREATE TABLE TESTTABLE (NAME VARCHAR(64), VALUE INTEGER, PRIMARY KEY(NAME, VALUE))");
        }
    }
    
    public void executeUpdate(String sql) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e); // NOSONAR
        } finally {
            safeClose(stmt);
        }
    }
    
    @Override
    public int countRows() {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int count = -1;
        try {
            stmt = conn.prepareStatement("SELECT * FROM TESTTABLE", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery();
            rs.last();
            count = rs.getRow();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e); // NOSONAR
        }
        finally {
            safeClose(rs);
            safeClose(stmt);
        }

        return count;
    }

    
    public void insertRow(String name, int value) throws SQLException {
        PreparedStatement stmt = null;
        Connection con2 = null;
        try {
            // Need to create a new connection to participate in transaction
            con2 = connect();
            stmt = con2.prepareStatement("INSERT INTO TESTTABLE VALUES (?, ?)");
            stmt.setString(1, name);
            stmt.setInt(2, value);
            stmt.executeUpdate();
        }
        finally {
            safeClose(stmt);
            safeClose(con2);
        }
    }
    
    public void close() {
        safeClose(conn);
    }

    private static void safeClose(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static void safeClose(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    

    private static void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

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
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

public class Connector {
    private DataSource xads;
    private DataSource ds;
    private String user;
    private String password;

    public void setXads(DataSource xads) {
        this.xads = xads;
    }
    public void setDs(DataSource ds) {
        this.ds = ds;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Connection connect() throws SQLException {
        return xads.getConnection(user, password);
    }
    
    public void initialize() {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = connect();
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
            conn = connect();
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

    public void safeClose(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException e) {
            // Ignore
        }
    }

    public void safeClose(Statement stmt) {
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

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

import java.sql.SQLException;

import org.apache.aries.transaction.test.TestBean;

public class TestBeanImpl implements TestBean {
    private Connector connector;
    private TestBean bean;

    @Override
    public void insertRow(String name, int value, Exception e) throws SQLException {
        connector.insertRow(name, value);
        if (e instanceof SQLException) { 
            throw (SQLException) e;
        } else if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
    }

    @Override
    public void delegateInsertRow(String name, int value) throws SQLException {
        bean.insertRow(name, value, null);
    }

    @Override
    public void throwApplicationException() throws SQLException {
        throw new SQLException("Test exception");
    }

    @Override
    public void throwRuntimeException() {
        throw new RuntimeException("Test exception"); // NOSONAR
    }

    public void setTestBean(TestBean bean) {
        this.bean = bean;
    }
    
    public void setConnector(Connector connector) {
        this.connector = connector;
    }

}

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
package org.apache.aries.transaction.jdbc.internal;

import org.tranql.connector.AllExceptionsAreFatalSorter;
import org.tranql.connector.ExceptionSorter;
import org.tranql.connector.NoExceptionsAreFatalSorter;
import org.tranql.connector.jdbc.AbstractXADataSourceMCF;
import org.tranql.connector.jdbc.ConfigurableSQLStateExceptionSorter;
import org.tranql.connector.jdbc.KnownSQLStateExceptionSorter;

import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.XADataSource;
import java.util.ArrayList;
import java.util.List;

public class XADataSourceMCFFactory {

    private XADataSource dataSource;
    private ExceptionSorter exceptionSorter = new AllExceptionsAreFatalSorter();
    private String userName;
    private String password;

    private ManagedConnectionFactory connectionFactory;

    public ManagedConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void init() throws Exception {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource must be set");
        }
        if (connectionFactory == null) {
            connectionFactory = new XADataSourceMCF();
        }
    }

    public void setExceptionSorterAsString(String sorter) {
        if ("all".equalsIgnoreCase(sorter)) {
            this.exceptionSorter = new AllExceptionsAreFatalSorter();
        } else if ("none".equalsIgnoreCase(sorter)) {
            this.exceptionSorter = new NoExceptionsAreFatalSorter();
        } else if ("known".equalsIgnoreCase(sorter)) {
            this.exceptionSorter = new KnownSQLStateExceptionSorter();
        } else if (sorter.toLowerCase().startsWith("custom(") && sorter.endsWith(")")) {
            List<String> states = new ArrayList<String>();
            for (String s : sorter.substring(7, sorter.length() - 2).split(",")) {
                if (s != null && s.length() > 0) {
                    states.add(s);
                }
            }
            this.exceptionSorter = new ConfigurableSQLStateExceptionSorter(states);
        } else {
            throw new IllegalArgumentException("Unknown exceptionSorter " + sorter);
        }
    }

    public XADataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(XADataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ExceptionSorter getExceptionSorter() {
        return exceptionSorter;
    }

    public void setExceptionSorter(ExceptionSorter exceptionSorter) {
        this.exceptionSorter = exceptionSorter;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public class XADataSourceMCF extends AbstractXADataSourceMCF<XADataSource> {

        public XADataSourceMCF() {
            super(XADataSourceMCFFactory.this.dataSource, XADataSourceMCFFactory.this.exceptionSorter);
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }
    }

}

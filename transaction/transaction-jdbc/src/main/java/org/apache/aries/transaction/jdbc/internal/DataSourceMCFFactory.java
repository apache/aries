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

import javax.resource.spi.TransactionSupport;
import javax.sql.DataSource;

import org.tranql.connector.jdbc.AbstractLocalDataSourceMCF;

public class DataSourceMCFFactory extends AbstractMCFFactory {

    @Override
    public void init() throws Exception {
        if (getDataSource() == null) {
            throw new IllegalArgumentException("dataSource must be set");
        }
        if (connectionFactory == null) {
            connectionFactory = new DataSourceMCF();
        }
    }

    @SuppressWarnings("serial")
    public class DataSourceMCF extends AbstractLocalDataSourceMCF<DataSource> implements TransactionSupport {
        public DataSourceMCF() {
            super((DataSource) DataSourceMCFFactory.this.getDataSource(), DataSourceMCFFactory.this.getExceptionSorter(), true);
        }

        public String getUserName() {
            return DataSourceMCFFactory.this.getUserName();
        }

        public String getPassword() {
            return DataSourceMCFFactory.this.getPassword();
        }

        @Override
        public TransactionSupportLevel getTransactionSupport() {
            return TransactionSupportLevel.LocalTransaction;
        }
    }
}

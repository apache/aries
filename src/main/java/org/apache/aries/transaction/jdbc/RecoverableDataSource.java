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
package org.apache.aries.transaction.jdbc;

import org.apache.aries.transaction.jdbc.internal.GenericResourceManager;
import org.apache.aries.transaction.jdbc.internal.XADatasourceEnlistingWrapper;

/**
 * Defines a JDBC DataSource that will auto-enlist into existing XA transactions.
 * The DataSource will also be registered with the Aries/Geronimo transaction
 * manager in order to provide proper transaction recovery at startup.
 * Other considerations such as connection pooling and error handling are
 * completely ignored.
 *
 * @org.apache.xbean.XBean
 */
public class RecoverableDataSource extends XADatasourceEnlistingWrapper {

    private String name;

    public String getName() {
        return name;
    }

    /**
     * The unique name for this managed XAResource.  This name will be used
     * by the transaction manager to recover transactions.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @org.apache.xbean.InitMethod
     */
    public void start() {
        new GenericResourceManager(getName(), getTransactionManager(), getDataSource()).recoverResource();
    }
}

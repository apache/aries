/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.javax;

import com.google.common.base.CaseFormat;
import org.apache.aries.blueprint.plugin.model.TransactionalDef;
import org.apache.aries.blueprint.plugin.spi.TransactionalFactory;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.util.HashMap;

public class JavaxTransactionFactory implements TransactionalFactory<Transactional> {
    private static HashMap<TxType, String> txTypeNames;
    
    static {
        txTypeNames = new HashMap<TxType, String>();
        txTypeNames.put(TxType.REQUIRED, TransactionalDef.TYPE_REQUIRED);
        txTypeNames.put(TxType.REQUIRES_NEW, TransactionalDef.TYPE_REQUIRES_NEW);
    }

    @Override
    public String getTransactionTypeName(Transactional transactional) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, transactional.value().name());
    }

    @Override
    public Class<Transactional> getTransactionalClass() {
        return Transactional.class;
    }
}

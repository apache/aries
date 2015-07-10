/**
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
package org.apache.aries.transaction.parsing;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.aries.transaction.annotations.TransactionPropagationType;

public class TxTypeConverter {
    private static Map<TxType, TransactionPropagationType> typeMap;
    
    static {
        typeMap = new HashMap<Transactional.TxType, TransactionPropagationType>();
        typeMap.put(TxType.MANDATORY, TransactionPropagationType.Mandatory);
        typeMap.put(TxType.NEVER, TransactionPropagationType.Never);
        typeMap.put(TxType.NOT_SUPPORTED, TransactionPropagationType.NotSupported);
        typeMap.put(TxType.REQUIRED, TransactionPropagationType.Required);
        typeMap.put(TxType.REQUIRES_NEW, TransactionPropagationType.RequiresNew);
        typeMap.put(TxType.SUPPORTS, TransactionPropagationType.Supports);
    }
    
    public static TransactionPropagationType convert(TxType jtaType) {
        return typeMap.get(jtaType);
    }
}

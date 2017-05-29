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
package org.apache.aries.transaction;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import javax.transaction.Transactional.TxType;

import org.apache.aries.transaction.pojo.BadlyAnnotatedPojo1;
import org.apache.aries.transaction.pojo.AnnotatedPojo;
import org.apache.aries.transaction.pojo.ExtendedPojo;
import org.apache.aries.transaction.pojo.ExtendedPojo2;
import org.apache.aries.transaction.pojo.ExtendedPojo3;
import org.junit.Assert;
import org.junit.Test;

public class ComponentTxDataTest {

    @Test
    public void testFindAnnotation() throws NoSuchMethodException, SecurityException {
        ComponentTxData txData = new ComponentTxData(AnnotatedPojo.class);
        Assert.assertTrue(txData.isTransactional());
        assertEquals(TxType.REQUIRED, getType(txData, "increment"));
        assertEquals(TxType.SUPPORTS, getType(txData, "checkValue"));
        assertEquals(TxType.MANDATORY, getType(txData, "getRealObject"));
    }
    
    @Test
    public void testFindAnnotationExtended() throws Exception {
        ComponentTxData txData = new ComponentTxData(ExtendedPojo.class);
        assertEquals(TxType.REQUIRED, getType(txData, "defaultType"));
        assertEquals(TxType.SUPPORTS, getType(txData, "supports"));
    }

    
    @Test
    public void testFindAnnotationExtended2() throws Exception {
        ComponentTxData txData = new ComponentTxData(ExtendedPojo2.class);
        assertEquals(TxType.MANDATORY, getType(txData, "defaultType"));
        assertEquals(TxType.SUPPORTS, getType(txData, "supports"));
    }
    
    @Test
    public void testFindAnnotationExtended3() throws Exception {
        ComponentTxData txData = new ComponentTxData(ExtendedPojo3.class);
        assertEquals(TxType.MANDATORY, getType(txData, "defaultType"));
        assertEquals(TxType.REQUIRED, getType(txData, "supports"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNoPrivateAnnotation() {
        new ComponentTxData(BadlyAnnotatedPojo1.class);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNoStaticAnnotation() {
        new ComponentTxData(BadlyAnnotatedPojo1.class);
    }
    
    private TxType getType(ComponentTxData txData, String methodName) {
        Class<?> c = txData.getBeanClass();
        Method m;
        try {
            m = c.getDeclaredMethod(methodName, String.class);
        } catch (NoSuchMethodException e) {
            try {
                m = c.getMethod(methodName, String.class);
            } catch (NoSuchMethodException e1) {
                throw new IllegalArgumentException(e1);
            }
        }
        return txData.getEffectiveType(m).getTxType();
    }

}

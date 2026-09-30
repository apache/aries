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
package org.apache.aries.proxy.impl;

import org.objectweb.asm.Opcodes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ProxyUtilsTest {
    @Test
    public void testVerifyJavaClassVersion8AndAbove() {
        assertEquals(Opcodes.V1_8, ProxyUtils.verifyJavaClassVersion(52));
        assertEquals(Opcodes.V9, ProxyUtils.verifyJavaClassVersion(53));
        assertEquals(Opcodes.V10, ProxyUtils.verifyJavaClassVersion(54));
        assertEquals(Opcodes.V11, ProxyUtils.verifyJavaClassVersion(55));
        assertEquals(Opcodes.V12, ProxyUtils.verifyJavaClassVersion(56));
        assertEquals(Opcodes.V13, ProxyUtils.verifyJavaClassVersion(57));
        assertEquals(Opcodes.V14, ProxyUtils.verifyJavaClassVersion(58));
        assertEquals(Opcodes.V15, ProxyUtils.verifyJavaClassVersion(59));
        assertEquals(Opcodes.V16, ProxyUtils.verifyJavaClassVersion(60));
        assertEquals(Opcodes.V17, ProxyUtils.verifyJavaClassVersion(61));
        assertEquals(Opcodes.V18, ProxyUtils.verifyJavaClassVersion(62));
        assertEquals(Opcodes.V19, ProxyUtils.verifyJavaClassVersion(63));
        assertEquals(Opcodes.V20, ProxyUtils.verifyJavaClassVersion(64));
        assertEquals(Opcodes.V21, ProxyUtils.verifyJavaClassVersion(65));
        assertEquals(Opcodes.V22, ProxyUtils.verifyJavaClassVersion(66));
        assertEquals(Opcodes.V23, ProxyUtils.verifyJavaClassVersion(67));
        assertEquals(Opcodes.V24, ProxyUtils.verifyJavaClassVersion(68));
        assertEquals(Opcodes.V25, ProxyUtils.verifyJavaClassVersion(69));
        assertEquals(Opcodes.V26, ProxyUtils.verifyJavaClassVersion(70));
        assertEquals(Opcodes.V27, ProxyUtils.verifyJavaClassVersion(71));
        assertEquals(72, ProxyUtils.verifyJavaClassVersion(72)); // supporting future versions of Java we don't know about yet
    }

    @Test
    public void testVerifyJavaClassVersionBelow8() {
        int V1_0 = 45; // there is no Opcodes.V1_0 constant in ASM
        for (int i = V1_0; i < Opcodes.V1_8; i++) {
            int javaClassVersion = i;
            assertThrows(IllegalArgumentException.class, () -> ProxyUtils.verifyJavaClassVersion(javaClassVersion));
        }
        // special case for V1_1 which is 196653
        assertThrows(IllegalArgumentException.class, () -> ProxyUtils.verifyJavaClassVersion(Opcodes.V1_1));
    }
}

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

import java.math.BigDecimal;

import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for proxy generation.
 */
public class ProxyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyUtils.class);
    /**
     * The Java class version of the current JVM.
     */
    public static final int JAVA_CLASS_VERSION = new BigDecimal(System.getProperty("java.class.version")).intValue();
    private static int weavingJavaVersion = -1; // initialise an invalid number

    /**
     * Get the class file version to be used for weaving.
     *
     * @return the class file version, e.g. 69 for Java 25
     */
    public static int getWeavingJavaVersion() {
        if (weavingJavaVersion == -1) {
            weavingJavaVersion = verifyJavaClassVersion(JAVA_CLASS_VERSION);
        }
        return weavingJavaVersion;
    }

    static int verifyJavaClassVersion(int javaClassVersion) {
        if (javaClassVersion < Opcodes.V1_8 || javaClassVersion == Opcodes.V1_1) {
            throw new IllegalArgumentException("Unsupported Java class version: " + javaClassVersion);
        }
        LOGGER.debug("Weaving to Java {}", javaClassVersion - Opcodes.V1_8 + 8);
        return javaClassVersion;
    }
}

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

public class ProxyUtils
{
  private static Logger LOGGER = LoggerFactory.getLogger(ProxyUtils.class);
  public static int javaClassVersion = new BigDecimal(System.getProperty("java.class.version")).intValue();
  /**
   * Get the java version to be woven at.
   * @return
   */
  public static int getWeavingJavaVersion() {
    
    if (javaClassVersion >= Opcodes.V1_7) {
      LOGGER.debug("Weaving to Java 7");
      return Opcodes.V1_7;
    } else {
      LOGGER.debug("Weaving to Java 6");
      return Opcodes.V1_6;
    }
    
  }
}

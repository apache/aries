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
package org.apache.aries.blueprint.proxy;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public class ProxyTestClassSerializable implements Serializable {
  
  public int value = 0;

  /**
   * We deserialize using this static method to ensure that the right classloader
   * is used when deserializing our object, it will always be the classloader that
   * loaded this class, which might be the JUnit one, or our weaving one.
   * 
   * @param bytes
   * @param value
   * @throws Exception
   */
  public static void checkDeserialization(byte[] bytes, int value) throws Exception {
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
    ProxyTestClassSerializable out = (ProxyTestClassSerializable) ois.readObject();
    assertEquals(value, out.value);
  }
  
}

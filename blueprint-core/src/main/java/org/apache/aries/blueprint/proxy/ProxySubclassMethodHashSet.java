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

import java.util.HashSet;

import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxySubclassMethodHashSet<E> extends HashSet<String>
{
  private static final long serialVersionUID = 7674408912532811084L;

  private static Logger LOGGER = LoggerFactory.getLogger(ProxySubclassMethodHashSet.class);

  public ProxySubclassMethodHashSet(int i)
  {
    super(i);
  }

  public void addMethodArray(java.lang.reflect.Method[] arrayOfEntries)
  {
    LOGGER.debug(AsmInterceptorWrapper.LOG_ENTRY, "addMethodArray", new Object[] { arrayOfEntries });

    for (java.lang.reflect.Method entry : arrayOfEntries) {
      String methodName = entry.getName();

      LOGGER.debug("Method name: {}", methodName);

      Type[] methodArgTypes = Type.getArgumentTypes(entry);
      String argDescriptor = typeArrayToStringArgDescriptor(methodArgTypes);

      LOGGER.debug("Descriptor: {}", argDescriptor);

      boolean added = super.add(methodName + argDescriptor);

      LOGGER.debug("Added: {}", added);
    }

    LOGGER.debug(AsmInterceptorWrapper.LOG_EXIT, "addMethodArray");
  }

  static String typeArrayToStringArgDescriptor(Type[] argTypes)
  {
    StringBuilder descriptor = new StringBuilder();
    for (Type t : argTypes) {
      descriptor.append(t.toString());
    }
    return descriptor.toString();
  }

}

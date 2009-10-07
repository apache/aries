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
package org.apache.aries.jndi.url;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * A parser for the aries namespace
 */
public final class ServiceRegistryNameParser implements NameParser
{

  public Name parse(String name) throws NamingException
  {
    if (!!!name.startsWith("aries:services/") &&
        !!!name.startsWith("osgi:services/")) throw new InvalidNameException("The JNDI name did not start with aries:, or osgi:");
    
    name = name.substring(name.indexOf('/') + 1);
    
    int slashIndex = name.indexOf('/');
    String interfaceName = name;
    String filter = null;
    
    if (slashIndex != -1) {
      interfaceName = name.substring(0, slashIndex);
      filter = name.substring(slashIndex + 1);
    }
    
    if (interfaceName.length() == 0) throw new InvalidNameException("No interface name was specified");
    
    Name result = new ServiceRegistryName();
    result.add(interfaceName);
    if (filter != null) {
      result.add(filter);
    }
    
    return result;
  }

  @Override
  public boolean equals(Object other)
  {
    return other instanceof ServiceRegistryNameParser;
  }
  
  @Override
  public int hashCode()
  {
    return 100003;
  }
}
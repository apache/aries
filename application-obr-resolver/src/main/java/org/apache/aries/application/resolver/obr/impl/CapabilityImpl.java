/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.aries.application.resolver.obr.impl;

import org.osgi.service.obr.Capability;

import java.util.Map;

/**
 * @version $Rev: 910661 $ $Date: 2010-02-16 19:38:51 +0000 (Tue, 16 Feb 2010) $
 */
public class CapabilityImpl implements Capability
{

  private final String name;
  private final Map properties;

  public CapabilityImpl(String name, Map properties)
  {
    this.name = name;
    this.properties = properties;
  }

  public String getName()
  {
    return name;
  }

  public Map getProperties()
  {
    return properties;
  }
  
}

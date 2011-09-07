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
package org.apache.aries.jpa.container.context.transaction.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.Synchronization;

public class TranSyncRegistryMock
{
  private String key;
  
  private Map<String, List<Synchronization>> syncs = new HashMap<String, List<Synchronization>>();
  
  private Map<String, Map<Object,Object>> resources = new HashMap<String, Map<Object,Object>>();
  
  public void setTransactionKey(String s)
  {
    key = s;
  }
  
  public Object getTransactionKey() {
    return key;
  }

  public void registerInterposedSynchronization(Synchronization arg0) {
    List<Synchronization> list = syncs.get(key);
    if(list == null) {
      list = new ArrayList<Synchronization>();
      syncs.put(key, list);
    }
     list.add(arg0);
  }
  
  public Object getResource(Object o) {
    Object toReturn = null;
    Map<Object, Object> map = resources.get(key);
    if(map != null)
      toReturn = map.get(o);
    return toReturn;
  }
  
  public void putResource(Object resourceKey, Object value) {
    Map<Object, Object> map = resources.get(key);
    if(map == null) {
      map = new HashMap<Object, Object>();
      resources.put(key, map);
    }
    map.put(resourceKey, value);
  }
  
  
  public void afterCompletion(String s)
  {
    if(syncs.get(s) != null) {
      for(Synchronization sync : syncs.remove(s))
        sync.afterCompletion(Status.STATUS_COMMITTED);
    }
    resources.remove(s);
  }
}
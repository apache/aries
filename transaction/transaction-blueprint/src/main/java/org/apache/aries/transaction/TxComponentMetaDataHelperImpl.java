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
package org.apache.aries.transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.transaction.TransactionManager;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

public class TxComponentMetaDataHelperImpl implements TxComponentMetaDataHelper {

    private static class TranData
    {
      private final String strategy;
      private final Pattern[] methodNames;
      
      public TranData(String s, String m)
      {
        strategy = s;
        String[] names = m.split("[, \t]");
        methodNames = new Pattern[names.length];
        
        for (int i = 0; i < names.length; i++) {
          methodNames[i] = Pattern.compile(names[i].replaceAll("\\*", ".*"));
        }
      }
      
      public boolean matches(String name)
      {
        for (Pattern p : methodNames) {
          if (p.matcher(name).matches()) {
            return true;
          }
        }
        return false;
      }
    }

    // TODO cope with having multiple tran data per component.
    private final Map<ComponentMetadata, TranData> data = new ConcurrentHashMap<ComponentMetadata, TranData>();
    private TransactionManager tm;
    
    public void setComponentTransactionData(ComponentMetadata component, String value, String method)
    {
      TranData td = new TranData(value, method);
      
      data.put(component, td);
    }

    public TransactionManager getTransactionManager()
    {
      return tm;
    }
    
    public void setTransactionManager(TransactionManager manager)
    {
      tm = manager;
    }


    public String getComponentMethodTxStrategy(ComponentMetadata component, String methodName)
    {
      String result = null;
      TranData td = data.get(component);
      
      if (td != null) {
        if (td.matches(methodName)) {
          result = td.strategy;
        }
     }

      return result;
    }
}

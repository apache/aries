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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.transaction.TransactionManager;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

public class TxComponentMetaDataHelperImpl implements TxComponentMetaDataHelper {

    private static class TranData
    {
      private final Map<Pattern, String> map;
      
      public TranData() {
          map = new LinkedHashMap<Pattern, String>();
      }
      
      public void add(Pattern pattern, String strategy) {
          map.put(pattern, strategy);
      }
      
      public String getStrategy(String name)
      {
        for (Pattern p : map.keySet()) {
          if (p.matcher(name).matches()) {
            return map.get(p);
          }
        }
        return null;
      }
    }
    
    private final Map<ComponentMetadata, TranData> data = new ConcurrentHashMap<ComponentMetadata, TranData>();
    private TransactionManager tm;
    
    public synchronized void setComponentTransactionData(ComponentMetadata component, String value, String method)
    {
      TranData td = data.get(component);
          
      if (td == null) {
          td = new TranData();
          data.put(component, td);
      }
      
      String[] names = method.split("[, \t]");
      
      for (int i = 0; i < names.length; i++) {
          Pattern pattern = Pattern.compile(names[i].replaceAll("\\*", ".*"));
          td.add(pattern, value);
      }
    }

    public String getComponentMethodTxStrategy(ComponentMetadata component, String methodName)
    {
        TranData td = data.get(component);
        String result = null;

        if (td != null)
            result = td.getStrategy(methodName);

        return result;
    }

    public TransactionManager getTransactionManager()
    {
      return tm;
    }
    
    public void setTransactionManager(TransactionManager manager)
    {
      tm = manager;
    }
}

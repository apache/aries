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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

public class TxComponentMetaDataHelperImpl implements TxComponentMetaDataHelper {

    private static class TranData
    {
      private static final Pattern WILDCARD = Pattern.compile("\\Q.*\\E");
      private final Map<Pattern, String> map;
      private final Map<String, String> cache;
      
      public TranData() {
          map = new ConcurrentHashMap<Pattern, String>();
          cache = new ConcurrentHashMap<String, String>();
      }
      
      public void add(Pattern pattern, String txAttribute) {
          map.put(pattern, txAttribute);
      }
      
      public String getAttribute(String name)
      {
        String txAttribute = cache.get(name);
        
        if (txAttribute == null) {
            List<Pattern> matches = findMatches(name);
            int size = matches.size();

            if (size == 0) {
                txAttribute = "Required";
            }
            else if (size == 1) {
                txAttribute = map.get(matches.get(0));
            }
            else {
                matches = selectPatternsWithFewestWildcards(matches);
                size = matches.size();

                if (size == 1) {
                    txAttribute = map.get(matches.get(0));
                }
                else {
                    matches = selectLongestPatterns(matches);
                    size = matches.size();

                    if (size == 1) {
                        txAttribute = map.get(matches.get(0));
                    }
                    else {
                        throw new IllegalStateException("Unable to apply patterns: " + matches);
                    }
                }
            }
            
            cache.put(name, txAttribute);
        }
        
        return txAttribute;
      }
      
      private List<Pattern> findMatches(String name)
      {
        List<Pattern> matches = new ArrayList<Pattern>();
        for (Pattern p : map.keySet()) {
          if (p.matcher(name).matches()) {
            matches.add(p);
          }
        }
        return matches;
      }
      
      private List<Pattern> selectPatternsWithFewestWildcards(List<Pattern> matches) {
          List<Pattern> remainingMatches = new ArrayList<Pattern>();
          int minWildcards = Integer.MAX_VALUE;
          
          for (Pattern p : matches) {
              String pattern = p.pattern();
              Matcher m = WILDCARD.matcher(pattern);
              int count = 0;
              
              while (m.find()) {
                  count++;
              }
              
              if (count < minWildcards) {
                  remainingMatches.clear();
                  remainingMatches.add(p);
                  minWildcards = count;
              }
              else if (count == minWildcards) {
                  remainingMatches.add(p);
              }
          }
          
          return remainingMatches;
      }
      
      private List<Pattern> selectLongestPatterns(List<Pattern> matches) {
          List<Pattern> remainingMatches = new ArrayList<Pattern>();
          int longestLength = 0;
          
          for (Pattern p : matches) {
              String pattern = p.pattern();
              int length = pattern.length();
              
              if (length > longestLength) {
                  remainingMatches.clear();
                  remainingMatches.add(p);
                  longestLength = length;
              }
              else if (length == longestLength) {
                  remainingMatches.add(p);
              }
          }
          
          return remainingMatches;
      }
    }
    
    private final Map<ComponentMetadata, TranData> data = new ConcurrentHashMap<ComponentMetadata, TranData>();
    
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

    public String getComponentMethodTxAttribute(ComponentMetadata component, String methodName)
    {
        TranData td = data.get(component);
        String result = null;

        if (td != null)
            result = td.getAttribute(methodName);

        return result;
    }
}

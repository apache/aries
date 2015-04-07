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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.transaction.annotations.TransactionPropagationType;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxComponentMetaDataHelperImpl implements TxComponentMetaDataHelper {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(TxComponentMetaDataHelperImpl.class);

    private static class TranData
    {
      private final Map<Pattern, String> map;
      private final Map<String, String> cache;
      
      public TranData() {
          map = new ConcurrentHashMap<Pattern, String>();
          cache = new ConcurrentHashMap<String, String>();
      }
      
      public void add(Pattern pattern, TransactionPropagationType txAttribute) {
          map.put(pattern, txAttribute.name());
      }
      
    public TransactionPropagationType getAttribute(String name) {
        String txAttribute = cache.get(name);

        if (txAttribute != null) {
            return getType(txAttribute);
        }
        List<Pattern> matches = findMatches(name);
        int size = matches.size();

        if (size == 0) {
            // we should default to no transaction since we cannot find a match
            return null;
        }

        if (size == 1) {
            txAttribute = map.get(matches.get(0));
        } else {
            matches = selectPatternsWithFewestWildcards(matches);
            size = matches.size();

            if (size == 1) {
                txAttribute = map.get(matches.get(0));
            } else {
                matches = selectLongestPatterns(matches);
                size = matches.size();

                if (size == 1) {
                    txAttribute = map.get(matches.get(0));
                } else {
                    throw new IllegalStateException(
                                                    Constants.MESSAGES
                                                        .getMessage("unable.to.apply.patterns", matches));
                }
            }
        }

        if (txAttribute != null) {
            cache.put(name, txAttribute);
        }

        return getType(txAttribute);
    }
  
    private TransactionPropagationType getType(String typeSt) {
        return typeSt == null || typeSt.length() == 0 ? null : TransactionPropagationType.valueOf(typeSt);
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
              Matcher m = Constants.WILDCARD.matcher(pattern);
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
    
    private static final Map<ComponentMetadata, TranData> data = new ConcurrentHashMap<ComponentMetadata, TranData>();
    // bundle transaction map keeps track of the default transaction behavior for the bundle at the bundle-wide level.
    // this is configured via top level tx:transaction element for the blueprint managed bundle
    private static final ConcurrentHashMap<ComponentDefinitionRegistry, List<BundleWideTxData>> bundleTransactionMap = new ConcurrentHashMap<ComponentDefinitionRegistry, List<BundleWideTxData>>();

    // this gives us a reverse lookup when we need to clean up
    private static final ConcurrentMap<ComponentDefinitionRegistry, Collection<ComponentMetadata>> dataForCDR = 
        new ConcurrentHashMap<ComponentDefinitionRegistry, Collection<ComponentMetadata>>();
    
    public void unregister(ComponentDefinitionRegistry registry) {
        Collection<ComponentMetadata> components = dataForCDR.remove(registry);
        bundleTransactionMap.remove(registry);
        
        if (components != null) {
            for (ComponentMetadata meta : components) data.remove(meta);
        }
    }
    
    public synchronized void setComponentTransactionData(ComponentDefinitionRegistry registry, ComponentMetadata component, TransactionPropagationType value, String method)
    {
      TranData td = data.get(component);
          
      if (td == null) {
          td = new TranData();
          data.put(component, td);

          dataForCDR.putIfAbsent(registry, new HashSet<ComponentMetadata>());
          dataForCDR.get(registry).add(component);
      }
      
      if (method == null || method.length() == 0) {
    	  method = "*";
      }
      if(value == null) {
        value = TransactionPropagationType.Required;
      }
      
      String[] names = method.split("[, \t]");
      
      for (int i = 0; i < names.length; i++) {
          Pattern pattern = Pattern.compile(names[i].replaceAll("\\*", ".*"));
          td.add(pattern, value);
      }
    }

    public TransactionPropagationType getComponentMethodTxAttribute(ComponentMetadata component, String methodName)
    {
    	if (LOGGER.isDebugEnabled()) {
    	    LOGGER.debug("Getting the txAttribute for the component {0} and method {1}", component.getId(), methodName);
    	}
        TranData td = data.get(component);
        TransactionPropagationType result = null;

        if (td != null) {
            // bean level transaction always overwrite bundle wide transaction 
            result = td.getAttribute(methodName);
        } 
        
        if (result != null) {
        	if (LOGGER.isDebugEnabled()) {
        	    LOGGER.debug("Return the txAttribute {0} for the component and method", result);
        	}
            return result;
        } else {
            /* check the bundle wide transaction configuration in the following priority order from (high to low)
             * 1. top level tx w/ method + bean
             * 2. top level tx w/ bean
             * 3. top level tx w/ method
             * 4. top level tx w/ no other attribute
             */
            //result = calculateBundleWideTransaction(component, methodName);
            ComponentDefinitionRegistry cdr = getComponentDefinitionRegistry(component);
            if (cdr == null) {
                // no bundle wide transaction configuration avail
            	result = null;
            } else {
                List<BundleWideTxData> bundleData = bundleTransactionMap.get(cdr);
                result = BundleWideTxDataUtil.getAttribute(component.getId(), methodName, bundleData);
            }
        }

    	if (LOGGER.isDebugEnabled()) {
    	    LOGGER.debug("Return the txAttribute {0} for the component and method", result);
    	}
        return result;
    }
    
    public void populateBundleWideTransactionData(ComponentDefinitionRegistry cdr, TransactionPropagationType value,
            String method, String bean) {
    	if (LOGGER.isDebugEnabled()) {
    	    LOGGER.debug("Start populating bundle wide transaction data value {0} method {1} bean {2} per component definition registry", new Object[]{value, method, bean});
    	}

        BundleWideTxData bundleWideTxData = new BundleWideTxData(value, method, bean);
        List<BundleWideTxData> bundleData = bundleTransactionMap.get(cdr);
        if (bundleData == null) {
            bundleData = new ArrayList<BundleWideTxData>();
            bundleData.add(bundleWideTxData);
            if (LOGGER.isDebugEnabled()) {
        	    LOGGER.debug("Adding component definition registry and bundleData to the bundleTransactionMap", new Object[]{cdr, bundleData});
            }
            bundleTransactionMap.put(cdr, bundleData);

        } else {
            bundleData.add(bundleWideTxData);
        }
        
    }
    
    private ComponentDefinitionRegistry getComponentDefinitionRegistry(ComponentMetadata metadata) {
        Enumeration<ComponentDefinitionRegistry> keys = bundleTransactionMap.keys();
        while (keys.hasMoreElements()) {
            ComponentDefinitionRegistry cdr = keys.nextElement();
            if (cdr.containsComponentDefinition(metadata.getId()) 
                    && metadata.equals(cdr.getComponentDefinition(metadata.getId()))) {
                return cdr;
            }
        }
        
        return null;
    }
}

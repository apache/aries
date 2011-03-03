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
package org.apache.aries.spifly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgRestrictions {
    private final Map<Pair<Integer, String>, List<String>> restrictions = 
            new HashMap<Pair<Integer, String>, List<String>>();
    
    public void addRestriction(int argNumber, String className) {
        addRestriction(argNumber, className, null);
    }

    public void addRestriction(int argNumber, String className, String allowedArgValue) {
        Pair<Integer, String> key = new Pair<Integer, String>(argNumber, className);
        List<String> allowedValues = restrictions.get(key);
        if (allowedArgValue != null) {
            if (allowedValues == null) {
                allowedValues = new ArrayList<String>();
                restrictions.put(key, allowedValues);
            }            
            allowedValues.add(allowedArgValue);            
        }
        restrictions.put(key, allowedValues);
    }

    public String[] getArgClasses() {
        List<String> classes = new ArrayList<String>();
        for (Pair<Integer, String> key : restrictions.keySet()) {
            classes.add(key.getRight());
        }
        
        if (classes.size() == 0)
            return null;
        return classes.toArray(new String [classes.size()]);
    }

    public boolean matches(Map<Pair<Integer, String>, String> args) {
        for (Pair<Integer, String> key : args.keySet()) {
            if (!restrictions.containsKey(key)) {
                return false;
            }
            
            List<String> values = restrictions.get(key);
            if (values != null) {
                String val = args.get(key);
                if (!values.contains(val)) {
                    return false;
                }
            }
        }
        return true;        
    }
}

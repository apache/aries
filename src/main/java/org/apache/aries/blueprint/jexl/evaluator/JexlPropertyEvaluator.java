/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.jexl.evaluator;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JexlPropertyEvaluator implements PropertyEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JexlPropertyEvaluator.class);
    
    private JexlExpressionParser jexlParser;
    private Dictionary<String, String> properties;
    
    public String evaluate(String expression, Dictionary<String, String> properties) {
        JexlExpressionParser parser = getJexlParser();
        this.properties = properties;

        Object obj;
        try {
            obj = parser.evaluate(expression);
            if (obj!=null) {
                return obj.toString();
            }
        } catch (Exception e) {
            LOGGER.info("Could not evaluate expression: {}", expression);
            LOGGER.info("Exception:", e);
        }
        
        return null;
    }
    
    private synchronized JexlExpressionParser getJexlParser() {
        if (jexlParser == null) {
            jexlParser = new JexlExpressionParser(toMap());
        }
        return jexlParser;
    }

    private Map<String, Object> toMap() {
        return new Map<String, Object>() {
            public boolean containsKey(Object o) {
                return properties.get(o) != null;
            }
            
            public Object get(Object o) {
                return properties.get(o);
            }
            
            // following are not important
            public Object put(String s, Object o) {
                throw new UnsupportedOperationException();
            }
            
            public int size() {
                throw new UnsupportedOperationException();
            }

            public boolean isEmpty() {
                throw new UnsupportedOperationException();
            }

            public boolean containsValue(Object o) {
                throw new UnsupportedOperationException();
            }

            public Object remove(Object o) {
                throw new UnsupportedOperationException();
            }

            public void putAll(Map<? extends String, ? extends Object> map) {
                throw new UnsupportedOperationException();
            }

            public void clear() {
                throw new UnsupportedOperationException();
            }

            public Set<String> keySet() {
                throw new UnsupportedOperationException();
            }

            public Collection<Object> values() {
                throw new UnsupportedOperationException();
            }

            public Set<Entry<String, Object>> entrySet() {
                throw new UnsupportedOperationException();
            }
        };
    }
}

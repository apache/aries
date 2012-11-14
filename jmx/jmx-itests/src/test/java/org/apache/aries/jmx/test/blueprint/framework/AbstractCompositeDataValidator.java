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
package org.apache.aries.jmx.test.blueprint.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

abstract public class AbstractCompositeDataValidator {
        
        private CompositeType type;
        private Map<String, Object> expectValues;
                
        protected AbstractCompositeDataValidator(CompositeType type){
            this.type = type;
            expectValues = new HashMap<String, Object>();
        }
                
        void setExpectValue(String key, Object value){
            expectValues.put(key, value);
        }
        
        public void validate(CompositeData target){
            if (!type.equals(target.getCompositeType()))
                fail("Expect type is " + type + ", but target type is " +target.getCompositeType());
            Set<String> keys = expectValues.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()) {
                String key = it.next();
                assertEquals(expectValues.get(key), target.get(key));
            }
            
        }
        
    }
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
package org.apache.aries.blueprint.ext;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.ValueMetadata;

public class AbstractPropertyPlaceholderTest extends AbstractPropertyPlaceholder {
    private final Map<String,String> values = new HashMap<String,String>();
    private LateBindingValueMetadata sut;
    
    @Before
    public void setup() {
        values.clear();
        bind("prop","value");
        bind("prop2","other");
    }
    
    @Test
    public void singleProp() {
        sut = makeProperty("${prop}");
        assertEquals("value", sut.getStringValue());
    }
    
    @Test
    public void multipleProps() {
        sut = makeProperty("the ${prop2} ${prop}");
        assertEquals("the other value", sut.getStringValue());
    }
    
    /*
     * Test helper methods
     */
    
    // Override to simulate actual property retrieval
    protected String getProperty(String prop) {
        return values.get(prop);
    }
    
    private void bind(String prop, String val) {
        values.put(prop, val);
    }
    
    private LateBindingValueMetadata makeProperty(final String prop) {
        return new LateBindingValueMetadata(new ValueMetadata() {
            public String getType() {
                return null;
            }
            
            public String getStringValue() {
                return prop;
            }
        });
    }
}

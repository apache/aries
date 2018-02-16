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

public class PropertyPlaceholderTest extends PropertyPlaceholder {
    private final Map<String,Object> values = new HashMap<String,Object>();
    private LateBindingValueMetadata sut;

    @Before
    public void setup() {
        values.clear();
        bind("prop1","hello");
        bind("prop2","world");
        bind("prop3","10");
        bind("prop4","20");
        bind("prop5","nested");
        bind("prop-nested","hello nested world!");
        bind("prop-recursive-1","${prop-recursive-2}");
        bind("prop-recursive-2","${prop-recursive-3}");
        bind("prop-recursive-3","recursive-3");

        bind("animal", "quick brown fox");
        bind("target", "lazy dog");
        bind("animal.1", "fox");
        bind("animal.2", "mouse");
        bind("species", "2");
    }

    @Test
    public void singleProp() {
        sut = makeProperty("${prop1}");
        assertEquals("hello", sut.getStringValue());
    }

    @Test
    public void multipleProps() {
        sut = makeProperty("say ${prop1} ${prop2}");
        assertEquals("say hello world", sut.getStringValue());
    }

    @Test
    public void nestedProps() {
        sut = makeProperty("${prop-${prop5}}");
        assertEquals("hello nested world!", sut.getStringValue());
    }

    @Test
    public void nestedProps2() {
        sut = makeProperty("The ${animal.${species}} jumps over the ${target}.");
        assertEquals("The mouse jumps over the lazy dog.", sut.getStringValue());
    }

    @Test
    public void nestedProps3() {
        sut = makeProperty("The ${animal.${species}} jumps over the ${target}.");
        bind("species", "1");
        assertEquals("The fox jumps over the lazy dog.", sut.getStringValue());
    }

    @Test
    public void recursiveProps() {
        sut = makeProperty("${prop-recursive-1}");
        assertEquals("recursive-3", sut.getStringValue());
    }

    @Test
    public void plainText() {
        sut = makeProperty("plain text");
        assertEquals("plain text", sut.getStringValue());
    }

//    @Test
//    public void evaluateStringProps() {
//        sut = makeProperty("${prop1+prop2}");
//        assertEquals("helloworld", sut.getStringValue());
//    }
//
//    @Test
//    public void evaluateIntProps() {
//        sut = makeProperty("${prop3+prop4}");
//        assertEquals("30", sut.getStringValue());
//    }



    /*
     * Test helper methods
     */

    // Override to simulate actual property retrieval
    protected Object getProperty(String prop) {
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

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
package org.apache.geronimo.blueprint.reflect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.blueprint.reflect.MapValue;
import org.osgi.service.blueprint.reflect.Value;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class MapValueImpl implements MapValue {

    private String keyType;
    private String valueType;
    private Map<Value, Value> map;

    public MapValueImpl() {
    }

    public MapValueImpl(String keyType, String valueType, Map<Value, Value> map) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.map = map;
    }

    public MapValueImpl(MapValue source) {
        if (source.getMap() != null) {
            map = new HashMap<Value, Value>();
            // both the values and the keys are Value types, so we need to deep copy them
            Iterator i = source.getMap().entrySet().iterator();
            while (i.hasNext()) {
                Entry entry = (Entry)i.next();
                map.put(MetadataUtil.cloneValue((Value)entry.getKey()), MetadataUtil.cloneValue((Value)entry.getValue()));
            }
        }
        valueType = source.getValueType();
        keyType = source.getKeyType();
    }
    
    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public Map<Value, Value> getMap() {
        return map;
    }

    public void setMap(Map<Value, Value> map) {
        this.map = map;
    }
}

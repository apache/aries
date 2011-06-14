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
package org.apache.aries.blueprint.reflect;

import org.apache.aries.blueprint.mutable.MutableMapEntry;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;

/**
 * Implementation of MapEntry
 *
 * @version $Rev$, $Date$
 */
public class MapEntryImpl implements MutableMapEntry {

    private NonNullMetadata key;
    private Metadata value;

    public MapEntryImpl() {
    }

    public MapEntryImpl(NonNullMetadata key, Metadata value) {
        this.key = key;
        this.value = value;
    }

    public MapEntryImpl(MapEntry entry) {
        this.key = (NonNullMetadata) MetadataUtil.cloneMetadata(entry.getKey());
        this.value = MetadataUtil.cloneMetadata(entry.getValue());
    }

    public NonNullMetadata getKey() {
        return key;
    }

    public void setKey(NonNullMetadata key) {
        this.key = key;
    }

    public Metadata getValue() {
        return value;
    }

    public void setValue(Metadata value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MapEntry[" +
                "key=" + key +
                ", value=" + value +
                ']';
    }
}

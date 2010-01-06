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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;

/**
 * Implementation of MapMetadata
 *
 * @version $Rev$, $Date$
 */
public class MapMetadataImpl implements MutableMapMetadata {

    private String keyType;
    private String valueType;
    private List<MapEntry> entries;

    public MapMetadataImpl() {
    }

    public MapMetadataImpl(String keyType, String valueType, List<MapEntry> entries) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.entries = entries;
    }

    public MapMetadataImpl(MapMetadata source) {
        this.valueType = source.getValueType();
        this.keyType = source.getKeyType();
        for (MapEntry entry : source.getEntries()) {
            addEntry(new MapEntryImpl(entry));
        }
    }

    public String getKeyType() {
        return this.keyType;
    }

    public void setKeyType(String keyTypeName) {
        this.keyType = keyTypeName;
    }

    public String getValueType() {
        return this.valueType;
    }

    public void setValueType(String valueTypeName) {
        this.valueType = valueTypeName;
    }

    public List<MapEntry> getEntries() {
        if (this.entries == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(this.entries);
        }
    }

    public void setEntries(List<MapEntry> entries) {
        this.entries = entries != null ? new ArrayList<MapEntry>(entries) : null;
    }

    public void addEntry(MapEntry entry) {
        if (this.entries == null) {
            this.entries = new ArrayList<MapEntry>();
        }
        this.entries.add(entry);
    }

    public MapEntry addEntry(NonNullMetadata key, Metadata value) {
        MapEntry entry = new MapEntryImpl(key, value);
        addEntry(entry);
        return entry;
    }

    public void removeEntry(MapEntry entry) {
        if (this.entries != null) {
            this.entries.remove(entry);
        }
    }

    @Override
    public String toString() {
        return "MapMetadata[" +
                "keyType='" + keyType + '\'' +
                ", valueType='" + valueType + '\'' +
                ", entries=" + entries +
                ']';
    }
}

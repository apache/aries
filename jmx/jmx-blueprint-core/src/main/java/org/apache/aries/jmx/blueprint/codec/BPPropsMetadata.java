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
package org.apache.aries.jmx.blueprint.codec;

import java.util.HashMap;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.PropsMetadata;

public class BPPropsMetadata implements BPNonNullMetadata {
    BPMapEntry[] entries;

    public BPPropsMetadata(CompositeData props) {
        CompositeData[] cd_entries = (CompositeData[]) props.get(BlueprintMetadataMBean.ENTRIES);
        entries = new BPMapEntry[cd_entries.length];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new BPMapEntry(cd_entries[i]);
        }
    }

    public BPPropsMetadata(PropsMetadata props) {
        entries = new BPMapEntry[props.getEntries().size()];
        int i = 0;
        for (Object arg : props.getEntries()) {
            entries[i++] = new BPMapEntry((MapEntry) arg);
        }
    }

    public CompositeData asCompositeData() {
        CompositeData[] cd_entries = new CompositeData[entries.length];
        for (int i = 0; i < entries.length; i++) {
            cd_entries[i] = entries[i].asCompositeData();
        }
        HashMap<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintMetadataMBean.ENTRIES, cd_entries);

        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.PROPS_METADATA_TYPE, items);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public BPMapEntry[] getEntries() {
        return entries;
    }
}

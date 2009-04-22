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

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class PropsMetadataImpl implements PropsMetadata {

    private List<MapEntry> entries;

    public PropsMetadataImpl() {
    }

    public PropsMetadataImpl(List<MapEntry> entries) {
        this.entries = entries;
    }

    public PropsMetadataImpl(PropsMetadata source) {
        for (MapEntry entry : source.getEntries()) {
            addEntry(new MapEntryImpl(entry));
        }
    }

    public List<MapEntry> getEntries() {
        if (this.entries == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(this.entries);
        }
    }

    public void setEntries(List<MapEntry> entries) {
        this.entries = entries;
    }

    public void addEntry(MapEntry entry) {
        if (this.entries == null) {
            this.entries = new ArrayList<MapEntry>();
        }
        this.entries.add(entry);
    }

}

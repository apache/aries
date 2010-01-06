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

import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

/**
 * Implementation of CollectionMetadata
 *
 * @version $Rev$, $Date$
 */
public class CollectionMetadataImpl implements MutableCollectionMetadata {

    private Class collectionClass;
    private String valueType;
    private List<Metadata> values;

    public CollectionMetadataImpl() {
    }

    public CollectionMetadataImpl(Class collectionClass, String valueType, List<Metadata> values) {
        this.collectionClass = collectionClass;
        this.valueType = valueType;
        this.values = values;
    }
    
    public CollectionMetadataImpl(CollectionMetadata source) {
        this.collectionClass = source.getCollectionClass();
        this.valueType = source.getValueType();
        for (Metadata value : source.getValues()) {
            addValue(MetadataUtil.cloneMetadata(value));
        }
    }

    public Class getCollectionClass() {
        return collectionClass;
    }

    public void setCollectionClass(Class collectionClass) {
        this.collectionClass = collectionClass;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public List<Metadata> getValues() {
        if (this.values == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(this.values);
        }
    }

    public void setValues(List<Metadata> values) {
        this.values = values != null ? new ArrayList<Metadata>(values) : null;
    }

    public void addValue(Metadata value) {
        if (this.values == null) {
            this.values = new ArrayList<Metadata>();
        }
        this.values.add(value);
    }

    public void removeValue(Metadata value) {
        if (this.values != null) {
            this.values.remove(value);
        }
    }

    @Override
    public String toString() {
        return "CollectionMetadata[" +
                "collectionClass=" + collectionClass +
                ", valueType='" + valueType + '\'' +
                ", values=" + values +
                ']';
    }
}

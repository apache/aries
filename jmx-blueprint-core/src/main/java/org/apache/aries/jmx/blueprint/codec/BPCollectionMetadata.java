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
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

public class BPCollectionMetadata implements BPNonNullMetadata {
    private String collectionClass;

    private String valueType;

    private BPMetadata[] values;

    public BPCollectionMetadata(CompositeData collection) {
        collectionClass = (String) collection.get(BlueprintMetadataMBean.COLLECTION_CLASS);
        valueType = (String) collection.get(BlueprintMetadataMBean.VALUE_TYPE);

        Byte[][] arrays = (Byte[][]) collection.get(BlueprintMetadataMBean.VALUES);
        values = new BPMetadata[arrays.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Util.boxedBinary2BPMetadata((Byte[]) arrays[i]);
        }
    }

    public BPCollectionMetadata(CollectionMetadata collection) {
        collectionClass = collection.getCollectionClass().getCanonicalName();
        valueType = collection.getValueType();

        values = new BPMetadata[collection.getValues().size()];
        int i = 0;
        for (Object value : collection.getValues()) {
            values[i++] = Util.metadata2BPMetadata((Metadata) value);
        }
    }

    public CompositeData asCompositeData() {
        HashMap<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintMetadataMBean.COLLECTION_CLASS, collectionClass);
        items.put(BlueprintMetadataMBean.VALUE_TYPE, valueType);

        Byte[][] arrays = new Byte[values.length][];
        for (int i = 0; i < arrays.length; i++) {
            arrays[i] = Util.bpMetadata2BoxedBinary(values[i]);
        }
        items.put(BlueprintMetadataMBean.VALUES, arrays);

        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.COLLECTION_METADATA_TYPE, items);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCollectionClass() {
        return collectionClass;
    }

    public String getValueType() {
        return valueType;
    }

    public BPMetadata[] getValues() {
        return values;
    }
}

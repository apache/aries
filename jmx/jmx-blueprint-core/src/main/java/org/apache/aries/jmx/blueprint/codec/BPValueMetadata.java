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
import org.osgi.service.blueprint.reflect.ValueMetadata;

public class BPValueMetadata implements BPNonNullMetadata {
    private String stringValue;

    private String type;

    public BPValueMetadata(CompositeData value) {
        stringValue = (String) value.get(BlueprintMetadataMBean.STRING_VALUE);
        type = (String) value.get(BlueprintMetadataMBean.TYPE);
    }

    public BPValueMetadata(ValueMetadata value) {
        stringValue = value.getStringValue();
        type = value.getType();
    }

    public CompositeData asCompositeData() {
        HashMap<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintMetadataMBean.STRING_VALUE, stringValue);
        items.put(BlueprintMetadataMBean.TYPE, type);

        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.VALUE_METADATA_TYPE, items);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public String getStringValue() {
        return stringValue;
    }

    public String getType() {
        return type;
    }
}

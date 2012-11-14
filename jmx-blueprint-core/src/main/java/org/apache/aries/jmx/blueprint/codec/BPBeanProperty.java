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
import org.osgi.service.blueprint.reflect.BeanProperty;

public class BPBeanProperty implements TransferObject {
    private String name;
    private BPMetadata value;

    public BPBeanProperty(CompositeData property) {
        name = (String) property.get(BlueprintMetadataMBean.NAME);

        Byte[] buf = (Byte[]) property.get(BlueprintMetadataMBean.VALUE);
        value = Util.boxedBinary2BPMetadata(buf);
    }

    public BPBeanProperty(BeanProperty property) {
        name = property.getName();

        value = Util.metadata2BPMetadata(property.getValue());
    }

    public CompositeData asCompositeData() {
        HashMap<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintMetadataMBean.NAME, name);
        items.put(BlueprintMetadataMBean.VALUE, Util.bpMetadata2BoxedBinary(value));

        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.BEAN_PROPERTY_TYPE, items);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public BPMetadata getValue() {
        return value;
    }
}

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
import org.osgi.service.blueprint.reflect.BeanArgument;

public class BPBeanArgument implements TransferObject {
    private int index;

    private BPMetadata value;

    private String valueType;

    public BPBeanArgument(CompositeData argument) {
        index = (Integer) argument.get(BlueprintMetadataMBean.INDEX);
        Byte[] buf = (Byte[]) argument.get(BlueprintMetadataMBean.VALUE);
        value = Util.boxedBinary2BPMetadata(buf);
        valueType = (String) argument.get(BlueprintMetadataMBean.VALUE_TYPE);
    }

    public BPBeanArgument(BeanArgument argument) {
        index = argument.getIndex();

        value = Util.metadata2BPMetadata(argument.getValue());

        valueType = argument.getValueType();
    }

    public CompositeData asCompositeData() {
        HashMap<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintMetadataMBean.INDEX, index);
        items.put(BlueprintMetadataMBean.VALUE, Util.bpMetadata2BoxedBinary(value));
        items.put(BlueprintMetadataMBean.VALUE_TYPE, valueType);

        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.BEAN_ARGUMENT_TYPE, items);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public int getIndex() {
        return index;
    }

    public BPMetadata getValue() {
        return value;
    }

    public String getValueType() {
        return valueType;
    }
}

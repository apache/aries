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
import org.osgi.service.blueprint.reflect.ReferenceListener;

public class BPReferenceListener implements TransferObject {
    private String bindMethod;

    private String unbindMethod;

    private BPTarget listenerComponent;

    public BPReferenceListener(CompositeData listener) {
        bindMethod = (String) listener.get(BlueprintMetadataMBean.BIND_METHOD);
        unbindMethod = (String) listener.get(BlueprintMetadataMBean.UNBIND_METHOD);

        Byte[] buf = (Byte[]) listener.get(BlueprintMetadataMBean.LISTENER_COMPONENT);
        listenerComponent = (BPTarget) Util.boxedBinary2BPMetadata(buf);
    }

    public BPReferenceListener(ReferenceListener listener) {
        bindMethod = listener.getBindMethod();
        unbindMethod = listener.getUnbindMethod();

        listenerComponent = (BPTarget) Util.metadata2BPMetadata(listener.getListenerComponent());
    }

    public CompositeData asCompositeData() {
        HashMap<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintMetadataMBean.BIND_METHOD, bindMethod);
        items.put(BlueprintMetadataMBean.UNBIND_METHOD, unbindMethod);

        items.put(BlueprintMetadataMBean.LISTENER_COMPONENT, Util.bpMetadata2BoxedBinary(listenerComponent));

        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.REFERENCE_LISTENER_TYPE, items);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public String getBindMethod() {
        return bindMethod;
    }

    public BPTarget getListenerComponent() {
        return listenerComponent;
    }

    public String getUnbindMethod() {
        return unbindMethod;
    }
}

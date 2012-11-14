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
import java.util.Map;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

public abstract class BPComponentMetadata implements BPNonNullMetadata {
    private int activation;

    private String[] dependsOn;

    private String id;

    @SuppressWarnings("boxing")
    protected BPComponentMetadata(CompositeData component) {
        activation = (Integer) component.get(BlueprintMetadataMBean.ACTIVATION);
        dependsOn = (String[]) component.get(BlueprintMetadataMBean.DEPENDS_ON);
        id = (String) component.get(BlueprintMetadataMBean.ID);
    }

    protected BPComponentMetadata(ComponentMetadata component) {
        activation = component.getActivation();
        dependsOn =  (String[])component.getDependsOn().toArray(new String[0]);
        id = (String) component.getId();
    }

    protected Map<String, Object> getItemsMap() {
        HashMap<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintMetadataMBean.ACTIVATION, activation);
        items.put(BlueprintMetadataMBean.DEPENDS_ON, dependsOn);
        items.put(BlueprintMetadataMBean.ID, id);

        return items;
    }

    public int getActivation() {
        return activation;
    }

    public String[] getDependsOn() {
        return dependsOn;
    }

    public String getId() {
        return id;
    }
}

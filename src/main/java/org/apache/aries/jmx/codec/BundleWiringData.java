/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx.codec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.jmx.framework.BundleRevisionsStateMBean;

public class BundleWiringData {
    private final long bundleId;
    private final List<BundleCapability> capabilities;
    private final List<BundleRequirement> requirements;
    private List<BundleWire> requiredWires;

    public BundleWiringData(long bundleId, List<BundleCapability> capabilities, List<BundleRequirement> requirements, List<BundleWire> requiredWires) {
        this.bundleId = bundleId;
        this.capabilities = capabilities;
        this.requirements = requirements;
        this.requiredWires = requiredWires;
    }

    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(BundleRevisionsStateMBean.BUNDLE_ID, bundleId);
            items.put(BundleRevisionsStateMBean.BUNDLE_REVISION_ID, null); // TODO

            items.put(BundleRevisionsStateMBean.REQUIREMENTS, getRequirements());
            items.put(BundleRevisionsStateMBean.CAPABILITIES, getCapabilities());
            items.put(BundleRevisionsStateMBean.BUNDLE_WIRES_TYPE, getRequiredWires());

            return new CompositeDataSupport(BundleRevisionsStateMBean.BUNDLE_WIRING_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData" + e);
        }
    }

    private CompositeData[] getCapabilities() throws OpenDataException {
        CompositeData[] capData = new CompositeData[capabilities.size()];
        for (int i=0; i < capabilities.size(); i++) {
            BundleCapability capability = capabilities.get(i);
            capData[i] = getCapReqCompositeData(BundleRevisionsStateMBean.BUNDLE_CAPABILITY_TYPE,
                capability.getNamespace(), capability.getAttributes().entrySet(), capability.getDirectives().entrySet());
        }
        return capData;
    }

    private CompositeData[] getRequirements() throws OpenDataException {
        CompositeData [] reqData = new CompositeData[requirements.size()];
        for (int i=0; i < requirements.size(); i++) {
            BundleRequirement requirement = requirements.get(i);
            reqData[i] = getCapReqCompositeData(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT_TYPE,
                requirement.getNamespace(), requirement.getAttributes().entrySet(), requirement.getDirectives().entrySet());
        }
        return reqData;
    }

    private CompositeData getCapReqCompositeData(CompositeType type, String namespace, Set<Map.Entry<String,Object>> attributeSet, Set<Map.Entry<String,String>> directiveSet) throws OpenDataException {
        Map<String, Object> reqItems = new HashMap<String, Object>();

        TabularData attributes = new TabularDataSupport(BundleRevisionsStateMBean.ATTRIBUTES_TYPE);
        for (Map.Entry<String, Object> entry : attributeSet) {
            PropertyData<?> pd = PropertyData.newInstance(entry.getKey(), entry.getValue());
            attributes.put(pd.toCompositeData());
        }
        reqItems.put(BundleRevisionsStateMBean.ATTRIBUTES, attributes);

        TabularData directives = new TabularDataSupport(BundleRevisionsStateMBean.DIRECTIVES_TYPE);
        for (Map.Entry<String, String> entry : directiveSet) {
            CompositeData directive = new CompositeDataSupport(BundleRevisionsStateMBean.DIRECTIVE_TYPE,
                new String[] { BundleRevisionsStateMBean.KEY, BundleRevisionsStateMBean.VALUE },
                new Object[] { entry.getKey(), entry.getValue() });
            directives.put(directive);
        }
        reqItems.put(BundleRevisionsStateMBean.DIRECTIVES, directives);
        reqItems.put(BundleRevisionsStateMBean.NAMESPACE, namespace);

        CompositeData req = new CompositeDataSupport(type, reqItems);
        return req;
    }

    private CompositeData[] getRequiredWires() throws OpenDataException {
        CompositeData[] reqWiresData = new CompositeData[requiredWires.size()];
        for (int i=0; i < requiredWires.size(); i++) {
            BundleWire requiredWire = requiredWires.get(i);
            Map<String, Object> wireItems = new HashMap<String, Object>();

            BundleCapability capability = requiredWire.getCapability();
            wireItems.put(BundleRevisionsStateMBean.PROVIDER_BUNDLE_ID, capability.getRevision().getBundle().getBundleId());
            wireItems.put(BundleRevisionsStateMBean.PROVIDER_BUNDLE_REVISION_ID, null); // TODO
            wireItems.put(BundleRevisionsStateMBean.BUNDLE_CAPABILITY,
                    getCapReqCompositeData(BundleRevisionsStateMBean.BUNDLE_CAPABILITY_TYPE,
                    capability.getNamespace(), capability.getAttributes().entrySet(), capability.getDirectives().entrySet()));

            BundleRequirement requirement = requiredWire.getRequirement();
            wireItems.put(BundleRevisionsStateMBean.REQUIRER_BUNDLE_ID, requirement.getRevision().getBundle().getBundleId());
            wireItems.put(BundleRevisionsStateMBean.REQUIRER_BUNDLE_REVISION_ID, null); // TODO

            wireItems.put(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT,
                getCapReqCompositeData(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT_TYPE,
                requirement.getNamespace(), requirement.getAttributes().entrySet(), requirement.getDirectives().entrySet()));

            CompositeData wireData = new CompositeDataSupport(BundleRevisionsStateMBean.BUNDLE_WIRE_TYPE, wireItems);
            reqWiresData[i] = wireData;
        }
        return reqWiresData;
    }
}

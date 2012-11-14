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
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.jmx.framework.wiring.BundleWiringStateMBean;

public class BundleWiringData {
    private final long bundleId;
    private final int revisionId;
    private final List<BundleCapability> capabilities;
    private final List<BundleRequirement> requirements;
    private final List<BundleWire> providedWires;
    private final List<BundleWire> requiredWires;
    private final Map<BundleRevision, Integer> revisionIDMap;

    public BundleWiringData(long bundleId, int revisionId, List<BundleCapability> capabilities, List<BundleRequirement> requirements,
            List<BundleWire> providedWires, List<BundleWire> requiredWires, Map<BundleRevision, Integer> revisionIDMap) {
        this.bundleId = bundleId;
        this.revisionId = revisionId;
        this.capabilities = capabilities;
        this.requirements = requirements;
        this.providedWires = providedWires;
        this.requiredWires = requiredWires;
        this.revisionIDMap = revisionIDMap;
    }

    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(BundleWiringStateMBean.BUNDLE_ID, bundleId);
            items.put(BundleWiringStateMBean.BUNDLE_REVISION_ID, revisionId);

            items.put(BundleWiringStateMBean.REQUIREMENTS, getRequirements(requirements));
            items.put(BundleWiringStateMBean.CAPABILITIES, getCapabilities(capabilities));
            items.put(BundleWiringStateMBean.REQUIRED_WIRES, getRequiredWires());
            items.put(BundleWiringStateMBean.PROVIDED_WIRES, getProvidedWires());

            return new CompositeDataSupport(BundleWiringStateMBean.BUNDLE_WIRING_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData", e);
        }
    }

    private static CompositeData[] getCapabilities(List<BundleCapability> capabilityList) throws OpenDataException {
        CompositeData[] capData = new CompositeData[capabilityList.size()];
        for (int i=0; i < capabilityList.size(); i++) {
            BundleCapability capability = capabilityList.get(i);
            capData[i] = getCapReqCompositeData(BundleWiringStateMBean.BUNDLE_CAPABILITY_TYPE,
                capability.getNamespace(), capability.getAttributes().entrySet(), capability.getDirectives().entrySet());
        }
        return capData;
    }

    private static CompositeData[] getRequirements(List<BundleRequirement> requirementList) throws OpenDataException {
        CompositeData [] reqData = new CompositeData[requirementList.size()];
        for (int i=0; i < requirementList.size(); i++) {
            BundleRequirement requirement = requirementList.get(i);
            reqData[i] = getCapReqCompositeData(BundleWiringStateMBean.BUNDLE_REQUIREMENT_TYPE,
                requirement.getNamespace(), requirement.getAttributes().entrySet(), requirement.getDirectives().entrySet());
        }
        return reqData;
    }

    public static CompositeData getRevisionCapabilities(int revisionId, List<BundleCapability> bundleCapabilities) {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(BundleWiringStateMBean.BUNDLE_REVISION_ID, revisionId);
            items.put(BundleWiringStateMBean.CAPABILITIES, getCapabilities(bundleCapabilities));
            return new CompositeDataSupport(BundleWiringStateMBean.REVISION_CAPABILITIES_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData", e);
        }
    }

    public static CompositeData getRevisionRequirements(int revisionId, List<BundleRequirement> bundleRequirements) {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(BundleWiringStateMBean.BUNDLE_REVISION_ID, revisionId);
            items.put(BundleWiringStateMBean.REQUIREMENTS, getRequirements(bundleRequirements));
            return new CompositeDataSupport(BundleWiringStateMBean.REVISION_REQUIREMENTS_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData", e);
        }
    }

    public static CompositeData[] getCapabilitiesCompositeData(List<BundleCapability> bundleCapabilities) {
        try {
            CompositeData[] data = new CompositeData[bundleCapabilities.size()];

            for (int i=0; i < bundleCapabilities.size(); i++) {
                BundleCapability requirement = bundleCapabilities.get(i);

                CompositeData cd = BundleWiringData.getCapReqCompositeData(BundleWiringStateMBean.BUNDLE_CAPABILITY_TYPE,
                    requirement.getNamespace(), requirement.getAttributes().entrySet(), requirement.getDirectives().entrySet());
                data[i] = cd;
            }

            return data;
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData", e);
        }
    }

    public static CompositeData[] getRequirementsCompositeData(List<BundleRequirement> bundleRequirements) {
        try {
            CompositeData[] data = new CompositeData[bundleRequirements.size()];

            for (int i=0; i < bundleRequirements.size(); i++) {
                BundleRequirement requirement = bundleRequirements.get(i);

                CompositeData cd = BundleWiringData.getCapReqCompositeData(BundleWiringStateMBean.BUNDLE_REQUIREMENT_TYPE,
                    requirement.getNamespace(), requirement.getAttributes().entrySet(), requirement.getDirectives().entrySet());
                data[i] = cd;
            }

            return data;
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData", e);
        }
    }

    private static CompositeData getCapReqCompositeData(CompositeType type, String namespace, Set<Map.Entry<String,Object>> attributeSet, Set<Map.Entry<String,String>> directiveSet) throws OpenDataException {
        Map<String, Object> reqItems = new HashMap<String, Object>();

        TabularData attributes = new TabularDataSupport(BundleWiringStateMBean.ATTRIBUTES_TYPE);
        for (Map.Entry<String, Object> entry : attributeSet) {
            PropertyData<?> pd = PropertyData.newInstance(entry.getKey(), entry.getValue());
            attributes.put(pd.toCompositeData());
        }
        reqItems.put(BundleWiringStateMBean.ATTRIBUTES, attributes);

        TabularData directives = new TabularDataSupport(BundleWiringStateMBean.DIRECTIVES_TYPE);
        for (Map.Entry<String, String> entry : directiveSet) {
            CompositeData directive = new CompositeDataSupport(BundleWiringStateMBean.DIRECTIVE_TYPE,
                new String[] { BundleWiringStateMBean.KEY, BundleWiringStateMBean.VALUE },
                new Object[] { entry.getKey(), entry.getValue() });
            directives.put(directive);
        }
        reqItems.put(BundleWiringStateMBean.DIRECTIVES, directives);
        reqItems.put(BundleWiringStateMBean.NAMESPACE, namespace);

        CompositeData req = new CompositeDataSupport(type, reqItems);
        return req;
    }

    private CompositeData[] getProvidedWires() throws OpenDataException {
        return getWiresCompositeData(providedWires);
    }

    private CompositeData[] getRequiredWires() throws OpenDataException {
        return getWiresCompositeData(requiredWires);
    }

    private CompositeData[] getWiresCompositeData(List<BundleWire> wires) throws OpenDataException {
        CompositeData[] reqWiresData = new CompositeData[wires.size()];
        for (int i=0; i < wires.size(); i++) {
            BundleWire requiredWire = wires.get(i);
            Map<String, Object> wireItems = new HashMap<String, Object>();

            BundleCapability capability = requiredWire.getCapability();
            wireItems.put(BundleWiringStateMBean.PROVIDER_BUNDLE_ID, capability.getRevision().getBundle().getBundleId());
            wireItems.put(BundleWiringStateMBean.PROVIDER_BUNDLE_REVISION_ID, revisionIDMap.get(capability.getRevision()));
            wireItems.put(BundleWiringStateMBean.BUNDLE_CAPABILITY,
                    getCapReqCompositeData(BundleWiringStateMBean.BUNDLE_CAPABILITY_TYPE,
                    capability.getNamespace(), capability.getAttributes().entrySet(), capability.getDirectives().entrySet()));

            BundleRequirement requirement = requiredWire.getRequirement();
            wireItems.put(BundleWiringStateMBean.REQUIRER_BUNDLE_ID, requirement.getRevision().getBundle().getBundleId());
            wireItems.put(BundleWiringStateMBean.REQUIRER_BUNDLE_REVISION_ID, revisionIDMap.get(requirement.getRevision()));

            wireItems.put(BundleWiringStateMBean.BUNDLE_REQUIREMENT,
                getCapReqCompositeData(BundleWiringStateMBean.BUNDLE_REQUIREMENT_TYPE,
                requirement.getNamespace(), requirement.getAttributes().entrySet(), requirement.getDirectives().entrySet()));

            CompositeData wireData = new CompositeDataSupport(BundleWiringStateMBean.BUNDLE_WIRE_TYPE, wireItems);
            reqWiresData[i] = wireData;
        }
        return reqWiresData;
    }
}

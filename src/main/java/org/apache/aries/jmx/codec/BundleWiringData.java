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

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
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
    private final String namespace;
    private final List<BundleRequirement> requirements;

    public BundleWiringData(long bundleId, String namespace, List<BundleCapability> capabilities, List<BundleRequirement> requirements, List<BundleWire> requiredWires) {
        this.bundleId = bundleId;
        this.namespace = namespace;
        this.capabilities = capabilities;
        this.requirements = requirements;
    }

    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(BundleRevisionsStateMBean.BUNDLE_ID, bundleId);
            items.put(BundleRevisionsStateMBean.BUNDLE_REVISION_ID, null);


            CompositeData [] reqData = new CompositeData[requirements.size()];
            for (int i=0; i < requirements.size(); i++) {
                BundleRequirement requirement = requirements.get(i);
                Map<String, Object> reqItems = new HashMap<String, Object>();

                TabularData attributes = new TabularDataSupport(BundleRevisionsStateMBean.ATTRIBUTES_TYPE);
                for (Map.Entry<String, Object> entry : requirement.getAttributes().entrySet()) {
                    PropertyData<?> pd = PropertyData.newInstance(entry.getKey(), entry.getValue());
                    attributes.put(pd.toCompositeData());
                }
                reqItems.put(BundleRevisionsStateMBean.ATTRIBUTES, attributes);

                TabularData directives = new TabularDataSupport(BundleRevisionsStateMBean.DIRECTIVES_TYPE);
                for (Map.Entry<String, String> entry : requirement.getDirectives().entrySet()) {
                    CompositeData directive = new CompositeDataSupport(BundleRevisionsStateMBean.DIRECTIVE_TYPE,
                        new String[] { BundleRevisionsStateMBean.KEY, BundleRevisionsStateMBean.VALUE },
                        new Object[] { entry.getKey(), entry.getValue() });
                    directives.put(directive);
                }
                reqItems.put(BundleRevisionsStateMBean.DIRECTIVES, directives);
                reqItems.put(BundleRevisionsStateMBean.NAMESPACE, requirement.getNamespace());

                CompositeData req = new CompositeDataSupport(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT_TYPE, reqItems);
                reqData[i] = req;
            }

//            CompositeDataSupport directive = new CompositeDataSupport(BundleRevisionsStateMBean.DIRECTIVE_TYPE, new String [] {"Key", "Value"}, new Object [] {"Foo", "Bar"});
//            directives.put(directive);
//            reqItems.put(BundleRevisionsStateMBean.DIRECTIVES, directives);
//            reqItems.put(BundleRevisionsStateMBean.NAMESPACE, namespace);
//            CompositeDataSupport requirements = new CompositeDataSupport(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT_TYPE, reqItems);

            items.put(BundleRevisionsStateMBean.REQUIREMENTS, reqData);
            items.put(BundleRevisionsStateMBean.CAPABILITIES, null);
            items.put(BundleRevisionsStateMBean.BUNDLE_WIRES_TYPE, null);

            return new CompositeDataSupport(BundleRevisionsStateMBean.BUNDLE_WIRING_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData" + e);
        }
    }
}

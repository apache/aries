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
package org.apache.aries.jmx.framework;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.codec.BundleWiringData;
import org.apache.aries.jmx.util.FrameworkUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.jmx.framework.wiring.BundleRevisionsStateMBean;

public class BundleRevisionsState implements BundleRevisionsStateMBean {
    private final BundleContext bundleContext;
    private final Logger logger;


    public BundleRevisionsState(BundleContext bundleContext, Logger logger) {
        this.bundleContext = bundleContext;
        this.logger = logger;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getCurrentRevisionDeclaredRequirements(long, java.lang.String)
     */
    public CompositeData[] getCurrentRevisionDeclaredRequirements(long bundleId, String namespace) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(bundleContext, bundleId);
        BundleRevision revision = bundle.adapt(BundleRevision.class);

        return BundleWiringData.getRequirementsCompositeData(revision.getDeclaredRequirements(namespace));
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getCurrentRevisionDeclaredCapabilities(long, java.lang.String)
     */
    public ArrayType getCurrentRevisionDeclaredCapabilities(long bundleId, String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getCurrentWiring(long, java.lang.String)
     */
    public CompositeData getCurrentWiring(long bundleId, String namespace) throws IOException {
        Bundle bundle = FrameworkUtils.resolveBundle(bundleContext, bundleId);
        BundleRevision currentRevision = bundle.adapt(BundleRevision.class);
        Map<BundleRevision, Integer> revisionIDMap = getCurrentRevisionTransitiveRevisionsClosure(bundleId, namespace);
        return getRevisionWiring(currentRevision, 0, namespace, revisionIDMap);
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getCurrentWiringClosure(long)
     */
    public TabularData getCurrentWiringClosure(long rootBundleId, String namespace) throws IOException {
        Map<BundleRevision, Integer> revisionIDMap = getCurrentRevisionTransitiveRevisionsClosure(rootBundleId, namespace);

        TabularData td = new TabularDataSupport(BundleRevisionsStateMBean.BUNDLE_WIRING_CLOSURE_TYPE);
        for (Map.Entry<BundleRevision, Integer> entry : revisionIDMap.entrySet()) {
            td.put(getRevisionWiring(entry.getKey(), entry.getValue(), namespace, revisionIDMap));
        }

        return td;
    }

    // The current revision being passed in always gets assigned revision ID 0
    // All the other revision IDs unique, but don't increase monotonous.
    private Map<BundleRevision, Integer> getCurrentRevisionTransitiveRevisionsClosure(long rootBundleId, String namespace) throws IOException {
        Map<BundleRevision, Integer> revisionIDMap = new HashMap<BundleRevision, Integer>();

        Bundle rootBundle = FrameworkUtils.resolveBundle(bundleContext, rootBundleId);
        BundleRevision rootRevision = rootBundle.adapt(BundleRevision.class);
        populateTransitiveRevisions(namespace, rootRevision, revisionIDMap);

        // Set the root revision ID to 0,
        // TODO check if there is already a revision with ID 0 and if so swap them. Quite a small chance that this will be needed
        revisionIDMap.put(rootRevision, 0);
        return revisionIDMap;
    }

    private void populateTransitiveRevisions(String namespace, BundleRevision rootRevision, Map<BundleRevision, Integer> allRevisions) {
        allRevisions.put(rootRevision, rootRevision.hashCode());
        BundleWiring wiring = rootRevision.getWiring();
        List<BundleWire> wires = wiring.getRequiredWires(namespace);
        for (BundleWire wire : wires) {
            BundleRevision revision = wire.getCapability().getRevision();
            if (!allRevisions.containsKey(revision)) {
                populateTransitiveRevisions(namespace, revision, allRevisions);
            }
        }
    }

    private CompositeData getRevisionWiring(BundleRevision revision, int revisionID, String namespace, Map<BundleRevision, Integer> revisionIDMap) {
        BundleWiring wiring = revision.getWiring();
        List<BundleCapability> capabilities = wiring.getCapabilities(namespace);
        List<BundleRequirement> requirements = wiring.getRequirements(namespace);
        List<BundleWire> providedWires = wiring.getProvidedWires(namespace);
        List<BundleWire> requiredWires = wiring.getRequiredWires(namespace);

        BundleWiringData data = new BundleWiringData(wiring.getBundle().getBundleId(), revisionID, capabilities, requirements, providedWires, requiredWires, revisionIDMap);
        return data.toCompositeData();
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getRevisionsDeclaredRequirements(long, java.lang.String, boolean)
     */
    public ArrayType getRevisionsDeclaredRequirements(long bundleId, String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getRevisionsDeclaredCapabilities(long, java.lang.String, boolean)
     */
    public ArrayType getRevisionsDeclaredCapabilities(long bundleId, String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getRevisionsWiring(long, java.lang.String)
     */
    public ArrayType getRevisionsWiring(long bundleId, String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getWiringClosure(long, java.lang.String)
     */
    public ArrayType getWiringClosure(long rootBundleId, String namespace) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#matches(javax.management.openmbean.CompositeType, javax.management.openmbean.CompositeType)
     */
    public boolean matches(CompositeType provider, CompositeType requirer) {
        // TODO Auto-generated method stub
        return false;
    }
}

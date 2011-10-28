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

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import org.apache.aries.jmx.Logger;
import org.apache.aries.jmx.codec.BundleWiringData;
import org.apache.aries.jmx.util.FrameworkUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.jmx.framework.BundleRevisionsStateMBean;

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
    public ArrayType getCurrentRevisionDeclaredRequirements(long bundleId, String namespace) {
        // TODO Auto-generated method stub
        return null;
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

        System.out.println("******** getCurrentWiring: " + bundle);
        BundleWiringData data = new BundleWiringData(bundle.getBundleId());
        CompositeData compositeData = data.toCompositeData();
        System.out.println("######## " + compositeData);
        return compositeData;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getCurrentWiringClosure(long)
     */
    public CompositeData getCurrentWiringClosure(long rootBundleId) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getRevisionsDeclaredRequirements(long, java.lang.String, boolean)
     */
    public ArrayType getRevisionsDeclaredRequirements(long bundleId, String namespace, boolean inUse) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.osgi.jmx.framework.BundleRevisionsStateMBean#getRevisionsDeclaredCapabilities(long, java.lang.String, boolean)
     */
    public ArrayType getRevisionsDeclaredCapabilities(long bundleId, String namespace, boolean inUse) {
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

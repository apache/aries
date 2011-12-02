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

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.PropertyData;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.container.def.options.VMOption;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.TimeoutOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.jmx.framework.BundleRevisionsStateMBean;
import org.osgi.jmx.framework.PackageStateMBean;

public class BundleRevisionsStateMBeanTest extends AbstractIntegrationTest {
    @Configuration
    public static Option[] configuration() {
        return testOptions(
            new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
            new TimeoutOption( 0 ),

            PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
            CoreOptions.equinox().version("3.7.0.v20110613"),
            paxLogging("INFO"),

            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
            mavenBundle("org.osgi", "org.osgi.compendium"),
            mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx"),
            mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.api"),
            mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.whiteboard"),
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            provision(newBundle()
                    .add(org.apache.aries.jmx.test.bundlea.Activator.class)
                    .add(org.apache.aries.jmx.test.bundlea.api.InterfaceA.class)
                    .add(org.apache.aries.jmx.test.bundlea.impl.A.class)
                    .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea")
                    .set(Constants.BUNDLE_VERSION, "2.0.0")
                    .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api;version=2.0.0")
                    .set(Constants.IMPORT_PACKAGE,
                            "org.osgi.framework;version=1.5.0,org.osgi.util.tracker,org.apache.aries.jmx.test.bundleb.api;version=1.1.0;resolution:=optional" +
                            ",org.osgi.service.cm")
                    .set(Constants.BUNDLE_ACTIVATOR,
                            org.apache.aries.jmx.test.bundlea.Activator.class.getName())
                    .build(withBnd())),
            provision(newBundle()
                    .add(org.apache.aries.jmx.test.bundleb.Activator.class)
                    .add(org.apache.aries.jmx.test.bundleb.api.InterfaceB.class)
                    .add(org.apache.aries.jmx.test.bundleb.api.MSF.class)
                    .add(org.apache.aries.jmx.test.bundleb.impl.B.class)
                    .set(Constants.BUNDLE_SYMBOLICNAME,"org.apache.aries.jmx.test.bundleb")
                    .set(Constants.BUNDLE_VERSION, "1.0.0")
                    .set(Constants.EXPORT_PACKAGE,"org.apache.aries.jmx.test.bundleb.api;version=1.1.0")
                    .set(Constants.IMPORT_PACKAGE,"org.osgi.framework;version=1.5.0,org.osgi.util.tracker," +
                            "org.osgi.service.cm,org.apache.aries.jmx.test.fragmentc")
                    .set(Constants.BUNDLE_ACTIVATOR,
                            org.apache.aries.jmx.test.bundleb.Activator.class.getName())
                    .build(withBnd())),
            provision(newBundle()
                    .add(org.apache.aries.jmx.test.fragmentc.C.class)
                    .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.fragc")
                    .set(Constants.FRAGMENT_HOST, "org.apache.aries.jmx.test.bundlea")
                    .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.fragmentc")
                    .build(withBnd())),
            provision(newBundle()
                    .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundled")
                    .set(Constants.BUNDLE_VERSION, "3.0.0")
                    .set(Constants.REQUIRE_BUNDLE, "org.apache.aries.jmx.test.bundlea;bundle-version=2.0.0")
                    .build(withBnd()))
            );

    }

    @Override
    public void doSetUp() throws Exception {
        waitForMBean(new ObjectName(PackageStateMBean.OBJECTNAME));
    }

    @Test
    public void testGetCurrentRevisionDeclaredRequirements() throws IOException {
        BundleRevisionsStateMBean brsMBean = getMBean(BundleRevisionsStateMBean.OBJECTNAME, BundleRevisionsStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        BundleWiring bw = a.adapt(BundleWiring.class);

        List<BundleRequirement> requirements = bw.getRequirements(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData[] jmxRequirements = brsMBean.getCurrentRevisionDeclaredRequirements(a.getBundleId(), BundleRevisionsStateMBean.PACKAGE_NAMESPACE);
        Assert.assertEquals(requirements.size(), jmxRequirements.length);

    }

    @Test
    public void testGetCurrentWiring() throws IOException {
        BundleRevisionsStateMBean brsMBean = getMBean(BundleRevisionsStateMBean.OBJECTNAME, BundleRevisionsStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        CompositeData jmxWiring = brsMBean.getCurrentWiring(a.getBundleId(), BundleRevisionsStateMBean.PACKAGE_NAMESPACE);

        Assert.assertEquals(BundleRevisionsStateMBean.BUNDLE_WIRING_TYPE, jmxWiring.getCompositeType());
        Assert.assertEquals(a.getBundleId(), jmxWiring.get(BundleRevisionsStateMBean.BUNDLE_ID));

        BundleWiring bw = a.adapt(BundleWiring.class);
        CompositeData[] jmxCapabilities = (CompositeData[]) jmxWiring.get(BundleRevisionsStateMBean.CAPABILITIES);
        List<BundleCapability> capabilities = bw.getCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(capabilities.size(), jmxCapabilities.length);

        Map<Map<String, Object>, Map<String, String>> expectedCapabilities = capabilitiesToMap(capabilities);
        Map<Map<String, Object>, Map<String, String>> actualCapabilities = jmxCapReqToMap(jmxCapabilities);
        Assert.assertEquals(expectedCapabilities, actualCapabilities);

        CompositeData[] jmxRequirements = (CompositeData[]) jmxWiring.get(BundleRevisionsStateMBean.REQUIREMENTS);
        List<BundleRequirement> requirements = bw.getRequirements(BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(requirements.size(), jmxRequirements.length);

        Map<Map<String, Object>, Map<String, String>> expectedRequirements = requirementsToMap(requirements);
        Map<Map<String, Object>, Map<String, String>> actualRequirements = jmxCapReqToMap(jmxRequirements);
        Assert.assertEquals(expectedRequirements, actualRequirements);

        List<BundleWire> requiredWires = bw.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData[] jmxRequiredWires = (CompositeData[]) jmxWiring.get(BundleRevisionsStateMBean.REQUIRED_WIRES);
        Assert.assertEquals(requiredWires.size(), jmxRequiredWires.length);

        Set<List<Object>> expectedRequiredWires = new HashSet<List<Object>>();
        for (BundleWire wire : requiredWires) {
            List<Object> data = new ArrayList<Object>();

            data.add(wire.getCapability().getRevision().getBundle().getBundleId());
            data.add(wire.getCapability().getAttributes());
            data.add(wire.getCapability().getDirectives());
            data.add(wire.getRequirement().getRevision().getBundle().getBundleId());
            data.add(wire.getRequirement().getAttributes());
            data.add(wire.getRequirement().getDirectives());
            expectedRequiredWires.add(data);
        }

        Set<List<Object>> actualRequiredWires = new HashSet<List<Object>>();
        for (CompositeData wire : jmxRequiredWires) {
            List<Object> data = new ArrayList<Object>();
            data.add(wire.get(BundleRevisionsStateMBean.PROVIDER_BUNDLE_ID));
            // TODO bundle revision id
            data.add(getJmxAttributes((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_CAPABILITY)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_CAPABILITY)));
            data.add(wire.get(BundleRevisionsStateMBean.REQUIRER_BUNDLE_ID));
            data.add(getJmxAttributes((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT)));
            actualRequiredWires.add(data);
        }
        Assert.assertEquals(expectedRequiredWires, actualRequiredWires);

        List<BundleWire> providedWires = bw.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData[] jmxProvidedWires = (CompositeData []) jmxWiring.get(BundleRevisionsStateMBean.PROVIDED_WIRES);
        Assert.assertEquals(providedWires.size(), jmxProvidedWires.length);

        HashSet<List<Object>> expectedProvidedWires = new HashSet<List<Object>>();
        for (BundleWire wire : providedWires) {
            List<Object> data = new ArrayList<Object>();
            data.add(wire.getCapability().getRevision().getBundle().getBundleId());
            data.add(wire.getCapability().getAttributes());
            data.add(wire.getCapability().getDirectives());
            data.add(wire.getRequirement().getRevision().getBundle().getBundleId());
            data.add(wire.getRequirement().getAttributes());
            data.add(wire.getRequirement().getDirectives());
            expectedProvidedWires.add(data);
        }

        Set<List<Object>> actualProvidedWires = new HashSet<List<Object>>();
        for (CompositeData wire : jmxProvidedWires) {
            List<Object> data = new ArrayList<Object>();
            data.add(wire.get(BundleRevisionsStateMBean.PROVIDER_BUNDLE_ID));
            // TODO bundle revision id
            data.add(getJmxAttributes((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_CAPABILITY)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_CAPABILITY)));
            data.add(wire.get(BundleRevisionsStateMBean.REQUIRER_BUNDLE_ID));
            data.add(getJmxAttributes((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleRevisionsStateMBean.BUNDLE_REQUIREMENT)));
            actualProvidedWires.add(data);
        }
        Assert.assertEquals(expectedProvidedWires, actualProvidedWires);
    }

    private Map<Map<String, Object>, Map<String, String>> capabilitiesToMap(List<BundleCapability> capabilities) {
        Map<Map<String, Object>, Map<String, String>> map = new HashMap<Map<String,Object>, Map<String,String>>();
        for (BundleCapability cap : capabilities) {
            map.put(cap.getAttributes(), cap.getDirectives());
        }
        return map;
    }

    private Map<Map<String, Object>, Map<String, String>> requirementsToMap(List<BundleRequirement> requirements) {
        Map<Map<String, Object>, Map<String, String>> map = new HashMap<Map<String,Object>, Map<String,String>>();
        for (BundleRequirement req : requirements) {
            map.put(req.getAttributes(), req.getDirectives());
        }
        return map;
    }

    private Map<Map<String, Object>, Map<String, String>> jmxCapReqToMap(CompositeData[] jmxCapabilitiesOrRequirements) {
        Map<Map<String, Object>, Map<String, String>> actualCapabilities = new HashMap<Map<String,Object>, Map<String,String>>();
        for (CompositeData jmxCapReq : jmxCapabilitiesOrRequirements) {
            Map<String, Object> aMap = getJmxAttributes(jmxCapReq);
            Map<String, String> dMap = getJmxDirectives(jmxCapReq);
            actualCapabilities.put(aMap, dMap);
        }
        return actualCapabilities;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJmxAttributes(CompositeData jmxCapReq) {
        TabularData jmxAttributes = (TabularData) jmxCapReq.get(BundleRevisionsStateMBean.ATTRIBUTES);
        Map<String, Object> aMap = new HashMap<String, Object>();
        for (CompositeData jmxAttr : (Collection<CompositeData>) jmxAttributes.values()) {
            PropertyData<Object> pd = PropertyData.from(jmxAttr);
            aMap.put(pd.getKey(), pd.getValue());
        }
        return aMap;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getJmxDirectives(CompositeData jmxCapReq) {
        TabularData jmxDirectives = (TabularData) jmxCapReq.get(BundleRevisionsStateMBean.DIRECTIVES);
        Map<String, String> dMap = new HashMap<String, String>();
        for (CompositeData jmxDir : (Collection<CompositeData>) jmxDirectives.values()) {
            dMap.put((String) jmxDir.get(BundleRevisionsStateMBean.KEY), (String) jmxDir.get(BundleRevisionsStateMBean.VALUE));
        }
        return dMap;
    }
}

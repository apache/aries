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
package org.apache.aries.jmx.framework.wiring;

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.jmx.framework.wiring.BundleWiringStateMBean;

public class BundleWiringStateMBeanTest extends AbstractIntegrationTest {
    @Configuration
    public static Option[] configuration() {
        return testOptions(
            // new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
            // new TimeoutOption( 0 ),

            PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
            CoreOptions.equinox().version("3.8.0.V20120529-1548"),
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
        waitForMBean(new ObjectName(BundleWiringStateMBean.OBJECTNAME));
    }

    @Test
    public void testObjectName() throws Exception {
        Set<ObjectName> names = mbeanServer.queryNames(new ObjectName(BundleWiringStateMBean.OBJECTNAME + ",*"), null);
        assertEquals(1, names.size());
        ObjectName name = names.iterator().next();
        Hashtable<String, String> props = name.getKeyPropertyList();
        assertEquals(context().getProperty(Constants.FRAMEWORK_UUID), props.get("uuid"));
        assertEquals(context().getBundle(0).getSymbolicName(), props.get("framework"));
    }

    @Test
    public void testGetCurrentRevisionDeclaredRequirements() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        BundleRevision br = a.adapt(BundleRevision.class);

        List<BundleRequirement> requirements = br.getDeclaredRequirements(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData[] jmxRequirements = brsMBean.getCurrentRevisionDeclaredRequirements(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(requirements.size(), jmxRequirements.length);

        Map<Map<String, Object>, Map<String, String>> expectedRequirements = requirementsToMap(requirements);
        Map<Map<String, Object>, Map<String, String>> actualRequirements = jmxCapReqToMap(jmxRequirements);
        Assert.assertEquals(expectedRequirements, actualRequirements);
    }

    @Test
    public void testGetCurrentRevisionDeclaredCapabilities() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        BundleRevision br = a.adapt(BundleRevision.class);

        List<BundleCapability> capabilities = br.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData[] jmxCapabilities = brsMBean.getCurrentRevisionDeclaredCapabilities(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(capabilities.size(), jmxCapabilities.length);

        Map<Map<String, Object>, Map<String, String>> expectedCapabilities = capabilitiesToMap(capabilities);
        Map<Map<String, Object>, Map<String, String>> actualCapabilities = jmxCapReqToMap(jmxCapabilities);
        Assert.assertEquals(expectedCapabilities, actualCapabilities);
    }

    @Test
    public void testGetRevisionsDeclaredRequirements() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        BundleRevisions revisions = a.adapt(BundleRevisions.class);

        Assert.assertEquals("Precondition", 1, revisions.getRevisions().size());

        TabularData jmxRequirementsTable = brsMBean.getRevisionsDeclaredRequirements(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(1, jmxRequirementsTable.size());

        List<BundleRequirement> requirements = revisions.getRevisions().iterator().next().getDeclaredRequirements(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData jmxRevRequirements = (CompositeData) jmxRequirementsTable.values().iterator().next();
        CompositeData[] jmxRequirements = (CompositeData[]) jmxRevRequirements.get(BundleWiringStateMBean.REQUIREMENTS);

        Map<Map<String, Object>, Map<String, String>> expectedRequirements = requirementsToMap(requirements);
        Map<Map<String, Object>, Map<String, String>> actualRequirements = jmxCapReqToMap(jmxRequirements);
        Assert.assertEquals(expectedRequirements, actualRequirements);
    }

    @Test
    public void testGetRevisionsDeclaredCapabilities() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        BundleRevisions revisions = a.adapt(BundleRevisions.class);

        Assert.assertEquals("Precondition", 1, revisions.getRevisions().size());

        TabularData jmxCapabilitiesTable = brsMBean.getRevisionsDeclaredCapabilities(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(1, jmxCapabilitiesTable.size());

        List<BundleCapability> capabilities = revisions.getRevisions().iterator().next().getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData jmxRevCapabilities = (CompositeData) jmxCapabilitiesTable.values().iterator().next();
        CompositeData[] jmxCapabilities = (CompositeData[]) jmxRevCapabilities.get(BundleWiringStateMBean.CAPABILITIES);

        Map<Map<String, Object>, Map<String, String>> expectedCapabilities = capabilitiesToMap(capabilities);
        Map<Map<String, Object>, Map<String, String>> actualCapabilities = jmxCapReqToMap(jmxCapabilities);
        Assert.assertEquals(expectedCapabilities, actualCapabilities);
    }

    @Test
    public void testGetCurrentWiring() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        CompositeData jmxWiring = brsMBean.getCurrentWiring(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);

        Assert.assertEquals(BundleWiringStateMBean.BUNDLE_WIRING_TYPE, jmxWiring.getCompositeType());
        Assert.assertEquals(a.getBundleId(), jmxWiring.get(BundleWiringStateMBean.BUNDLE_ID));

        BundleWiring bw = a.adapt(BundleWiring.class);
        assertBundleWiring(bw, jmxWiring);
    }

    @Test
    public void testRevisionsWiring() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        TabularData jmxWiringTable = brsMBean.getRevisionsWiring(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);

        Assert.assertEquals(1, jmxWiringTable.size());
        CompositeData jmxWiring = (CompositeData) jmxWiringTable.values().iterator().next();
        Assert.assertEquals(BundleWiringStateMBean.BUNDLE_WIRING_TYPE, jmxWiring.getCompositeType());
        Assert.assertEquals(a.getBundleId(), jmxWiring.get(BundleWiringStateMBean.BUNDLE_ID));

        BundleWiring bw = a.adapt(BundleWiring.class);
        assertBundleWiring(bw, jmxWiring);
    }

    private void assertBundleWiring(BundleWiring bundleWiring, CompositeData jmxWiring) {
        CompositeData[] jmxCapabilities = (CompositeData[]) jmxWiring.get(BundleWiringStateMBean.CAPABILITIES);
        List<BundleCapability> capabilities = bundleWiring.getCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(capabilities.size(), jmxCapabilities.length);

        Map<Map<String, Object>, Map<String, String>> expectedCapabilities = capabilitiesToMap(capabilities);
        Map<Map<String, Object>, Map<String, String>> actualCapabilities = jmxCapReqToMap(jmxCapabilities);
        Assert.assertEquals(expectedCapabilities, actualCapabilities);

        CompositeData[] jmxRequirements = (CompositeData[]) jmxWiring.get(BundleWiringStateMBean.REQUIREMENTS);
        List<BundleRequirement> requirements = bundleWiring.getRequirements(BundleRevision.PACKAGE_NAMESPACE);
        Assert.assertEquals(requirements.size(), jmxRequirements.length);

        Map<Map<String, Object>, Map<String, String>> expectedRequirements = requirementsToMap(requirements);
        Map<Map<String, Object>, Map<String, String>> actualRequirements = jmxCapReqToMap(jmxRequirements);
        Assert.assertEquals(expectedRequirements, actualRequirements);

        List<BundleWire> requiredWires = bundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData[] jmxRequiredWires = (CompositeData[]) jmxWiring.get(BundleWiringStateMBean.REQUIRED_WIRES);
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
            data.add(wire.get(BundleWiringStateMBean.PROVIDER_BUNDLE_ID));
            // TODO bundle revision id
            data.add(getJmxAttributes((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_CAPABILITY)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_CAPABILITY)));
            data.add(wire.get(BundleWiringStateMBean.REQUIRER_BUNDLE_ID));
            data.add(getJmxAttributes((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_REQUIREMENT)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_REQUIREMENT)));
            actualRequiredWires.add(data);
        }
        Assert.assertEquals(expectedRequiredWires, actualRequiredWires);

        List<BundleWire> providedWires = bundleWiring.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE);
        CompositeData[] jmxProvidedWires = (CompositeData []) jmxWiring.get(BundleWiringStateMBean.PROVIDED_WIRES);
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
            data.add(wire.get(BundleWiringStateMBean.PROVIDER_BUNDLE_ID));
            data.add(getJmxAttributes((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_CAPABILITY)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_CAPABILITY)));
            data.add(wire.get(BundleWiringStateMBean.REQUIRER_BUNDLE_ID));
            data.add(getJmxAttributes((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_REQUIREMENT)));
            data.add(getJmxDirectives((CompositeData) wire.get(BundleWiringStateMBean.BUNDLE_REQUIREMENT)));
            actualProvidedWires.add(data);
        }
        Assert.assertEquals(expectedProvidedWires, actualProvidedWires);
    }

    @Test
    public void testCurrentWiringClosure() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        TabularData jmxWiringClosure = brsMBean.getCurrentWiringClosure(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);

        CompositeData jmxWiringA = jmxWiringClosure.get(new Object [] {a.getBundleId(), 0});
        assertBundleWiring(a.adapt(BundleWiring.class), jmxWiringA);

        Bundle b = context().getBundleByName("org.apache.aries.jmx.test.bundleb");
        int bRevID = findRevisionID(jmxWiringA, b);
        CompositeData jmxWiringB = jmxWiringClosure.get(new Object [] {b.getBundleId(), bRevID});
        assertBundleWiring(b.adapt(BundleWiring.class), jmxWiringB);

        Bundle cm = context().getBundleByName("org.apache.felix.configadmin");
        int cmRevID = findRevisionID(jmxWiringA, cm);
        CompositeData jmxWiringCM = jmxWiringClosure.get(new Object [] {cm.getBundleId(), cmRevID});
        assertBundleWiring(cm.adapt(BundleWiring.class), jmxWiringCM);

        Bundle sb = context().getBundle(0);
        int sbRevID = findRevisionID(jmxWiringA, sb);
        CompositeData jmxWiringSB = jmxWiringClosure.get(new Object [] {sb.getBundleId(), sbRevID});
        assertBundleWiring(sb.adapt(BundleWiring.class), jmxWiringSB);
    }

    @Test
    public void testRevisionsWiringClosure() throws Exception {
        BundleWiringStateMBean brsMBean = getMBean(BundleWiringStateMBean.OBJECTNAME, BundleWiringStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        TabularData jmxWiringClosure = brsMBean.getRevisionsWiringClosure(a.getBundleId(), BundleRevision.PACKAGE_NAMESPACE);

        CompositeData jmxWiringA = jmxWiringClosure.get(new Object [] {a.getBundleId(), 0});
        assertBundleWiring(a.adapt(BundleWiring.class), jmxWiringA);

        Bundle b = context().getBundleByName("org.apache.aries.jmx.test.bundleb");
        int bRevID = findRevisionID(jmxWiringA, b);
        CompositeData jmxWiringB = jmxWiringClosure.get(new Object [] {b.getBundleId(), bRevID});
        assertBundleWiring(b.adapt(BundleWiring.class), jmxWiringB);

        Bundle cm = context().getBundleByName("org.apache.felix.configadmin");
        int cmRevID = findRevisionID(jmxWiringA, cm);
        CompositeData jmxWiringCM = jmxWiringClosure.get(new Object [] {cm.getBundleId(), cmRevID});
        assertBundleWiring(cm.adapt(BundleWiring.class), jmxWiringCM);

        Bundle sb = context().getBundle(0);
        int sbRevID = findRevisionID(jmxWiringA, sb);
        CompositeData jmxWiringSB = jmxWiringClosure.get(new Object [] {sb.getBundleId(), sbRevID});
        assertBundleWiring(sb.adapt(BundleWiring.class), jmxWiringSB);
    }

    private int findRevisionID(CompositeData jmxWiring, Bundle bundle) {
        CompositeData[] requiredWires = (CompositeData []) jmxWiring.get(BundleWiringStateMBean.REQUIRED_WIRES);
        for (CompositeData req : requiredWires) {
            if (new Long(bundle.getBundleId()).equals(req.get(BundleWiringStateMBean.PROVIDER_BUNDLE_ID))) {
                return (Integer) req.get(BundleWiringStateMBean.PROVIDER_BUNDLE_REVISION_ID);
            }
        }
        return -1;
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
        TabularData jmxAttributes = (TabularData) jmxCapReq.get(BundleWiringStateMBean.ATTRIBUTES);
        Map<String, Object> aMap = new HashMap<String, Object>();
        for (CompositeData jmxAttr : (Collection<CompositeData>) jmxAttributes.values()) {
            PropertyData<Object> pd = PropertyData.from(jmxAttr);
            Object val = pd.getValue();
            if (val instanceof Object[]) {
                val = Arrays.asList((Object [])val);
            }
            aMap.put(pd.getKey(), val);
        }
        return aMap;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getJmxDirectives(CompositeData jmxCapReq) {
        TabularData jmxDirectives = (TabularData) jmxCapReq.get(BundleWiringStateMBean.DIRECTIVES);
        Map<String, String> dMap = new HashMap<String, String>();
        for (CompositeData jmxDir : (Collection<CompositeData>) jmxDirectives.values()) {
            dMap.put((String) jmxDir.get(BundleWiringStateMBean.KEY), (String) jmxDir.get(BundleWiringStateMBean.VALUE));
        }
        return dMap;
    }
}

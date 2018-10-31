/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.aries.spifly.HeaderParser.PathElement;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

public class ConsumerHeaderProcessor {
    private static final Dictionary<String, String> PROCESSOR_FILTER_MATCH;

    static {
        PROCESSOR_FILTER_MATCH = new Hashtable<String, String>();
        PROCESSOR_FILTER_MATCH.put(SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE, SpiFlyConstants.PROCESSOR_EXTENDER_NAME);
    }

    /**
     * Parses headers of the following syntax:
     * <ul>
     * <li><tt>org.acme.MyClass#myMethod</tt> - apply the weaving to all overloads of <tt>myMethod()</tt>
     * in <tt>MyClass</tt>
     * <li><tt>org.acme.MyClass#myMethod(java.lang.String, java.util.List)</tt> - apply the weaving only
     * to the <tt>myMethod(String, List)</tt> overload in <tt>MyClass</tt>
     * <li><tt>org.acme.MyClass#myMethod()</tt> - apply the weaving only to the noarg overload of
     * <tt>myMethod()</tt>
     * <li><b>true</b> - equivalent to <tt>java.util.ServiceLoader#load(java.lang.Class)</tt>
     * </ul>
     * Additionally, it registers the consumer's constraints with the consumer registry in the activator, if the
     * consumer is only constrained to a certain set of bundles.<p>
     *
     * The following attributes are supported:
     * <ul>
     * <li><tt>bundle</tt> - restrict wiring to the bundle with the specifies Symbolic Name. The attribute value
     * is a list of bundle identifiers separated by a '|' sign. The bundle identifier starts with the Symbolic name
     * and can optionally contain a version suffix. E.g. bundle=impl2:version=1.2.3 or bundle=impl2|impl4.
     * <li><tt>bundleId</tt> - restrict wiring to the bundle with the specified bundle ID. Typically used when
     * the service should be forcibly picked up from the system bundle (<tt>bundleId=0</tt>). Multiple bundle IDs
     * can be specified separated by a '|' sign.
     * </ul>
     *
     * @param consumerHeaderName the name of the header (either Require-Capability or SPI-Consumer)
     * @param consumerHeader the <tt>SPI-Consumer</tt> header.
     * @return an instance of the {@link WeavingData} class.
     * @throws Exception when a header cannot be parsed.
     */
    public static Set<WeavingData> processHeader(String consumerHeaderName, String consumerHeader) throws Exception {
        if (SpiFlyConstants.REQUIRE_CAPABILITY.equals(consumerHeaderName)) {
            return processRequireCapabilityHeader(consumerHeader);
        }

        Set<WeavingData> weavingData = new HashSet<WeavingData>();

        for (PathElement element : HeaderParser.parseHeader(consumerHeader)) {
            List<BundleDescriptor> allowedBundles = new ArrayList<BundleDescriptor>();
            String name = element.getName().trim();

            String className;
            String methodName;
            MethodRestriction methodRestriction;
            boolean serviceLoader = false;

            int hashIdx = name.indexOf('#');
            if (hashIdx > 0) {
                className = name.substring(0, hashIdx);
                int braceIdx = name.substring(hashIdx).indexOf('(');
                if (braceIdx > 0) {
                    methodName = name.substring(hashIdx + 1, hashIdx + braceIdx);
                    ArgRestrictions argRestrictions = new ArgRestrictions();
                    int closeIdx = name.substring(hashIdx).indexOf(')');
                    if (closeIdx > 0) {
                        String classes = name.substring(hashIdx + braceIdx + 1, hashIdx + closeIdx).trim();
                        if (classes.length() > 0) {
                            if (classes.indexOf('[') > 0) {
                                int argNumber = 0;
                                for (String s : classes.split(",")) {
                                    int idx = s.indexOf('[');
                                    int end = s.indexOf(']', idx);
                                    if (idx > 0 && end > idx) {
                                        argRestrictions.addRestriction(argNumber, s.substring(0, idx), s.substring(idx + 1, end));
                                    } else {
                                        argRestrictions.addRestriction(argNumber, s);
                                    }
                                    argNumber++;
                                }
                            } else {
                                String[] classNames = classes.split(",");
                                for (int i = 0; i < classNames.length; i++) {
                                    argRestrictions.addRestriction(i, classNames[i]);
                                }
                            }
                        } else {
                            argRestrictions = null;
                        }
                    }
                    methodRestriction = new MethodRestriction(methodName, argRestrictions);
                } else {
                    methodName = name.substring(hashIdx + 1);
                    methodRestriction = new MethodRestriction(methodName);
                }
            } else {
                if ("*".equalsIgnoreCase(name)) {
                    serviceLoader = true;
                    className = ServiceLoader.class.getName();
                    methodName = "load";
                    ArgRestrictions argRestrictions = new ArgRestrictions();
                    argRestrictions.addRestriction(0, Class.class.getName());
                    methodRestriction = new MethodRestriction(methodName, argRestrictions);
                } else {
                    throw new IllegalArgumentException("Must at least specify class name and method name: " + name);
                }
            }


            String bsn = element.getAttribute("bundle");
            if (bsn != null) {
                bsn = bsn.trim();
                if (bsn.length() > 0) {
                    for (String s : bsn.split("\\|")) {
                        int colonIdx = s.indexOf(':');
                        if (colonIdx > 0) {
                            String sn = s.substring(0, colonIdx);
                            String versionSfx = s.substring(colonIdx + 1);
                            if (versionSfx.startsWith("version=")) {
                                allowedBundles.add(new BundleDescriptor(sn,
                                        Version.parseVersion(versionSfx.substring("version=".length()))));
                            } else {
                                allowedBundles.add(new BundleDescriptor(sn));
                            }
                        } else {
                            allowedBundles.add(new BundleDescriptor(s));
                        }
                    }
                }
            }

            String bid = element.getAttribute("bundleId");
            if (bid != null) {
                bid = bid.trim();
                if (bid.length() > 0) {
                    for (String s : bid.split("\\|")) {
                        allowedBundles.add(new BundleDescriptor(Long.parseLong(s)));
                    }
                }
            }

            weavingData.add(createWeavingData(className, methodName, methodRestriction, allowedBundles));

            if (serviceLoader) {
                className = ServiceLoader.class.getName();
                methodName = "load";
                ArgRestrictions argRestrictions = new ArgRestrictions();
                argRestrictions.addRestriction(0, Class.class.getName());
                argRestrictions.addRestriction(1, ClassLoader.class.getName());
                methodRestriction = new MethodRestriction(methodName, argRestrictions);
                weavingData.add(createWeavingData(className, methodName, methodRestriction, allowedBundles));
            }
        }
        return weavingData;
    }

    private static Set<WeavingData> processRequireCapabilityHeader(String consumerHeader) throws InvalidSyntaxException {
        Set<WeavingData> weavingData = new HashSet<WeavingData>();

        Parameters requirements = OSGiHeader.parseHeader(consumerHeader);
        Entry<String, Attrs> extenderRequirement = findRequirement(requirements, SpiFlyConstants.EXTENDER_CAPABILITY_NAMESPACE, SpiFlyConstants.PROCESSOR_EXTENDER_NAME);
        Collection<Entry<String, Attrs>> serviceLoaderRequirements = findAllMetadata(requirements, SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);

        if (extenderRequirement != null) {
            List<BundleDescriptor> allowedBundles = new ArrayList<BundleDescriptor>();
            for (Entry<String, Attrs> req : serviceLoaderRequirements) {
                String slFilterString = req.getValue().get(SpiFlyConstants.FILTER_DIRECTIVE);
                if (slFilterString != null) {
                    Filter slFilter = FrameworkUtil.createFilter(slFilterString);
                    allowedBundles.add(new BundleDescriptor(slFilter));
                }
            }

            // ServiceLoader.load(Class)
            {
                ArgRestrictions ar = new ArgRestrictions();
                ar.addRestriction(0, Class.class.getName());
                MethodRestriction mr = new MethodRestriction("load", ar);
                weavingData.add(createWeavingData(ServiceLoader.class.getName(), "load", mr, allowedBundles));
            }

            // ServiceLoader.load(Class, ClassLoader)
            {
                ArgRestrictions ar = new ArgRestrictions();
                ar.addRestriction(0, Class.class.getName());
                ar.addRestriction(1, ClassLoader.class.getName());
                MethodRestriction mr = new MethodRestriction("load", ar);
                weavingData.add(createWeavingData(ServiceLoader.class.getName(), "load", mr, allowedBundles));
            }
        }

        return weavingData;
    }

    private static WeavingData createWeavingData(String className, String methodName,
            MethodRestriction methodRestriction, List<BundleDescriptor> allowedBundles) {
        ConsumerRestriction restriction = new ConsumerRestriction(className, methodRestriction);

        // TODO is this correct? Why is it added to a set?
        Set<ConsumerRestriction> restrictions = new HashSet<ConsumerRestriction>();
        restrictions.add(restriction);

        // TODO this can be done in the WeavingData itself?
        String[] argClasses = restriction.getMethodRestriction(methodName).getArgClasses();

        return new WeavingData(className, methodName, argClasses, restrictions,
                allowedBundles.size() == 0 ? null : allowedBundles);
    }

    static Entry<String, Attrs> findCapability(Parameters capabilities, String namespace, String spiName) {
        for (Entry<String, Attrs> cap : capabilities.entrySet()) {
            String key = removeDuplicateMarker(cap.getKey());
            if (namespace.equals(key)) {
                if (spiName.equals(cap.getValue().get(namespace))) {
                    return cap;
                }
            }
        }
        return null;
    }

    static Entry<String, Attrs> findRequirement(Parameters requirements, String namespace, String type) throws InvalidSyntaxException {
        Dictionary<String, Object> nsAttr = new Hashtable<>();
        nsAttr.put(namespace, type);
        nsAttr.put("version", SpiFlyConstants.SPECIFICATION_VERSION);

        for (Entry<String, Attrs> req : requirements.entrySet()) {
            String key = removeDuplicateMarker(req.getKey());
            if (namespace.equals(key)) {
                String filterString = req.getValue().get(SpiFlyConstants.FILTER_DIRECTIVE);
                if (filterString != null) {
                    Filter filter = FrameworkUtil.createFilter(filterString);
                    if (filter.match(nsAttr)) {
                        return req;
                    }
                }
            }
        }
        return null;
    }

    static Collection<Entry<String, Attrs>> findAllMetadata(Parameters requirementsOrCapabilities, String namespace) {
        List<Entry<String, Attrs>> reqsCaps = new ArrayList<>();
        for (Entry<String, Attrs> reqCap : requirementsOrCapabilities.entrySet()) {
            String key = removeDuplicateMarker(reqCap.getKey());
            if (namespace.equals(key)) {
                reqsCaps.add(reqCap);
            }
        }
        return reqsCaps;
    }

    static String removeDuplicateMarker(String key) {
        int i = key.length() - 1;
        while (i >= 0 && key.charAt(i) == '~')
            --i;

        return key.substring(0, i + 1);
    }

}

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
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.aries.spifly.HeaderParser.PathElement;
import org.osgi.framework.Version;

public class ConsumerHeaderProcessor {
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
     * consumer is only constrained to a certain set of bundles.<p/>
     * 
     * The following attributes are supported:
     * <ul>
     * <li><tt>bundle</tt> - restrict wiring to the bundle with the specifies Symbolic Name. The attribute value 
     * is a list of bundle identifiers separated by a '|' sign. The bundle identifier starts with the Symbolic name
     * and can optionally contain a version suffix. E.g. bundle=impl2:version=1.2.3 or bundle=impl2|impl4.  
     * <li><tt>bundleId</tt> - restrict wiring to the bundle with the specified bundle ID. Typically used when 
     * the service should be forceably picked up from the system bundle (<tt>bundleId=0</tt>). Multiple bundle IDs 
     * can be specified separated by a '|' sign. 
     * </ul>
     * 
     * @param consumerBundle the consuming bundle.
     * @param consumerHeader the <tt>SPI-Consumer</tt> header.
     * @return an instance of the {@link WeavingData} class.
     */
    public static Set<WeavingData> processHeader(/* Bundle consumerBundle, */String consumerHeader) {
        Set<WeavingData> weavingData = new HashSet<WeavingData>();
        
        for (PathElement element : HeaderParser.parseHeader(consumerHeader)) {
            List<BundleDescriptor> allowedBundles = new ArrayList<BundleDescriptor>();
            String name = element.getName().trim();

            String className;
            String methodName;
            MethodRestriction methodRestriction;
            
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
                    className = ServiceLoader.class.getName();
                    methodName = "load";
                    ArgRestrictions argRestrictions = new ArgRestrictions();
                    argRestrictions.addRestriction(0, Class.class.getName());
                    methodRestriction = new MethodRestriction(methodName, argRestrictions);
                } else {
                    throw new IllegalArgumentException("Must at least specify class name and method name: " + name);
                }
            }  
            ConsumerRestriction restriction = new ConsumerRestriction(className, methodRestriction);

            Set<ConsumerRestriction> restrictions = new HashSet<ConsumerRestriction>();
            restrictions.add(restriction);
                
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
                        
            // TODO this cannot be done this way with static weaving...
            /*
            String bid = element.getAttribute("bundleId");
            if (bid != null) {
                bid = bid.trim();
                if (bid.length() > 0) {
                    for (String s : bid.split("\\|")) {
                        for (Bundle b : consumerBundle.getBundleContext().getBundles()) {
                            if (("" + b.getBundleId()).equals(s)) {                       
                                allowedBundles.add(new BundleDescriptor(b.getSymbolicName()));
                                break;                        
                            }
                        }                        
                    }
                }
            }
            */
                                    
//            Activator.activator.registerConsumerBundle(consumerBundle, restrictions, 
//                    allowedBundles.size() == 0 ? null : allowedBundles);
            // TODO this can be done in the WeavingData itself?
            String[] argClasses = restriction.getMethodRestriction(methodName).getArgClasses();

            weavingData.add(new WeavingData(className, methodName, argClasses, restrictions, 
                    allowedBundles.size() == 0 ? null : allowedBundles));
        }
        return weavingData;
    }
}

/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application.resolver.obr.impl;


import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.resolver.obr.OBRAriesResolver;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;


public class AriesResolverTest extends OBRAriesResolver
{
  Resolver resolver;
  ResolverMock resolverMock;
  
  
  public AriesResolverTest() {
    super(Skeleton.newMock(RepositoryAdmin.class));
  }
  static class ResolverMock {
    private final List<Resource> resources = new ArrayList<Resource>();
    private final Map<String, List<Requirement>> requirements = new HashMap<String, List<Requirement>>();
    private String curRes;
    private ResourceMock curResMock;
    private RequirementMock curReqMock;
    
    public Reason[] getReason(Resource r)
    {
      Requirement[] reqs = requirements.get(r.getSymbolicName() + "_" + r.getVersion()).toArray(new Requirement[0]);
      
      Reason[] reasons = new Reason[reqs.length];
      int i=0;
      for (Requirement req : reqs) {
        
        reasons[i++] = new ReasonMock(r, req);
      }
      return reasons;
    }
    
    public Resource[] getRequiredResources()
    {
      return resources.toArray(new Resource[0]);
    }
    
    public ResolverMock res(String name, String version)
    {
      curRes = name + "_" + version;
      curResMock = new ResourceMock(name,version);
      resources.add(Skeleton.newMock(curResMock, Resource.class));
      requirements.put(curRes, new ArrayList<Requirement>());
      return this;
    }
    
    public ResolverMock optional() 
    {
      curReqMock.optional = true;
      return this;
    }
    
    public ResolverMock req(String name)
    {
      // requirements are based on String, so that we have valid equals and hashCode implementations
      curReqMock = new RequirementMock(name);
      requirements.get(curRes).add(Skeleton.newMock(curReqMock, Requirement.class));
      curResMock.addCapability(name);
      return this;
    }
  }
  
  static class CapabilityMock {
    private final String cap;
    
    CapabilityMock(String cap) {
      this.cap = cap;
    }
    
    @Override
    public String toString() {
      return cap;
    }
  }
  
  static class RequirementMock {
    private final String req;
    public boolean optional = false;
    
    RequirementMock(String req) {
      this.req = req;
    }
    
    public boolean isSatisfied(Capability c) {
      return c.toString().equals(req);
    }
    
    public boolean isOptional() {
      return optional;
    }
  }
  static class ReasonMock implements Reason{
    private final Resource res;
    private final Requirement req;
    ReasonMock (Resource res, Requirement req) {
      this.res = res;
      this.req = req;
    }
    
    public Resource getResource() {
      return this.res;
    }
    public Requirement getRequirement()
    {
      return this.req;
    }
  }
  static class ResourceMock {
    private final String name;
    private final Version version;
    private final List<Capability> capabilities;
    
    ResourceMock(String name, String version) { 
      this.name = name; 
      this.version = new Version(version); 
      capabilities = new ArrayList<Capability>();
    }
    
    public void addCapability(String cap) {
      capabilities.add(Skeleton.newMock(new CapabilityMock(cap), Capability.class));
    }
    
    public Capability[] getCapabilities() {
      return capabilities.toArray(new Capability[0]);
    }
    
    public Version getVersion() { return version; }
    public String getSymbolicName() { return name; }
  }
  
  @Before
  public void before()
  {
    resolverMock = new ResolverMock();
    resolver = Skeleton.newMock(resolverMock, Resolver.class);
  }
  
  @Test
  public void testIncompatible()
  {
    resolverMock
      .res("com.ibm.test", "0.0.0")
        .req("a")
        .req("b")
      .res("com.ibm.test", "1.0.0")
        .req("a")
        .req("c");
    
    List<Resource> res = retrieveRequiredResources(resolver);
    assertEquals(2, res.size());
    assertResource(res.get(0), "com.ibm.test", "0.0.0");
    assertResource(res.get(1), "com.ibm.test", "1.0.0");
  }
  
  @Test 
  public void testLeftRedundant()
  {
    resolverMock
      .res("com.ibm.test", "0.0.0")
        .req("a")
        .req("b")
      .res("com.ibm.test", "1.0.0")
        .req("a")
        .req("b")
        .req("c");

    List<Resource> res = retrieveRequiredResources(resolver);
    assertEquals(1, res.size());
    assertResource(res.get(0), "com.ibm.test", "1.0.0");
  }
  
  @Test
  public void testRightRedundant()
  {
    resolverMock
      .res("com.ibm.test", "0.0.0")
        .req("a")
        .req("b")
        .req("c")
      .res("com.ibm.test", "1.0.0")
        .req("a")
        .req("c");

    List<Resource> res = retrieveRequiredResources(resolver);
    assertEquals(1, res.size());
    assertResource(res.get(0), "com.ibm.test", "0.0.0");
  }
  
  @Test
  public void testEquivalent()
  {
    resolverMock
      .res("com.ibm.test", "0.0.0")
        .req("a")
        .req("b")
      .res("com.ibm.test", "2.0.0")
        .req("a")
        .req("b")
      .res("com.ibm.test", "1.0.0")
        .req("a")
        .req("b");
  
    List<Resource> res = retrieveRequiredResources(resolver);
    assertEquals(1, res.size());
    assertResource(res.get(0), "com.ibm.test", "2.0.0");
  }
  
  @Test
  public void testEquivalentWithOptionals()
  {
    // 1.1.0 and 1.0.0 are incompatible if we leave aside that "c" is optional. 
    // "bundle" is the downgrade dependency on 1.0.0, "c" is the optional service requirement for the CommentService
    resolverMock
      .res("com.ibm.test", "1.0.0")
        .req("a")
        .req("b")
        .req("bundle")
      .res("com.ibm.test", "1.1.0")
        .req("a")
        .req("b")
        .req("c").optional();
    
    List<Resource> res = retrieveRequiredResources(resolver);
    assertEquals(1, res.size());
    assertResource(res.get(0), "com.ibm.test", "1.0.0");    
  }
  
  private void assertResource(Resource r, String name, String version)
  {
    assertEquals(name, r.getSymbolicName());
    assertEquals(version, r.getVersion().toString());
  }
}

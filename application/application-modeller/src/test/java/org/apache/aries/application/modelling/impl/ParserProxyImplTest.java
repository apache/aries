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

package org.apache.aries.application.modelling.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.application.modelling.ParserProxy;
import org.apache.aries.application.modelling.WrappedReferenceMetadata;
import org.apache.aries.application.modelling.WrappedServiceMetadata;
import org.apache.aries.blueprint.ParserService;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry;
import org.apache.aries.blueprint.container.ParserServiceImpl;
import org.apache.aries.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class ParserProxyImplTest {

  static ParserProxy _parserProxy;
  static ModellingManager _modellingManager;
  
  @BeforeClass
  public static void setup() { 
    BundleContext mockCtx = Skeleton.newMock(new BundleContextMock(), BundleContext.class);
    NamespaceHandlerRegistry nhri = new NamespaceHandlerRegistryImpl (mockCtx);
    ParserService parserService = new ParserServiceImpl(nhri);
    mockCtx.registerService(ParserService.class.getName(), parserService, new Hashtable<String, String>());
    _parserProxy = new ParserProxyImpl();
    ((ParserProxyImpl)_parserProxy).setParserService(parserService);
    ((ParserProxyImpl)_parserProxy).setBundleContext(mockCtx);
    _modellingManager = new ModellingManagerImpl();
    ((ParserProxyImpl)_parserProxy).setModellingManager(_modellingManager);
  }
  
  
  
  @AfterClass
  public static void teardown() { 
    BundleContextMock.clear();
  }
  
  
  
  @Test
  public void basicTest1() throws Exception { 
    File bpXml = new File ("../src/test/resources", "appModeller/test1.eba/bundle1.jar/OSGI-INF/blueprint/bp.xml");
    File bp2Xml = new File ("../src/test/resources", "appModeller/test1.eba/bundle1.jar/OSGI-INF/blueprint/bp2.xml");
    List<URL> urls = new ArrayList<URL>();
    urls.add ((bpXml.toURI()).toURL());
    urls.add ((bp2Xml.toURI()).toURL());
    
    List<? extends WrappedServiceMetadata> results = _parserProxy.parse(urls);
    assertTrue ("Four results expected, not " + results.size(), results.size() == 4);
    
    Set<WrappedServiceMetadata> resultSet = new HashSet<WrappedServiceMetadata>(results);
    Set<WrappedServiceMetadata> expectedResults = getTest1ExpectedResults();
    assertEquals ("Blueprint parsed xml is not as expected: " + resultSet.toString() + " != " + expectedResults,
        resultSet, expectedResults);
  }
  
  @Test
  public void testParseAllServiceElements() throws Exception { 
    File bpXml = new File ("../src/test/resources", "appModeller/test1.eba/bundle1.jar/OSGI-INF/blueprint/bp.xml");
    File bp2Xml = new File ("../src/test/resources", "appModeller/test1.eba/bundle1.jar/OSGI-INF/blueprint/bp2.xml");
    
    List<WrappedServiceMetadata> services = new ArrayList<WrappedServiceMetadata>();
    List<WrappedReferenceMetadata> references = new ArrayList<WrappedReferenceMetadata>();
    
    FileInputStream fis = new FileInputStream (bpXml);
    ParsedServiceElements bpelem = _parserProxy.parseAllServiceElements(fis); 
    services.addAll(bpelem.getServices());
    references.addAll(bpelem.getReferences());
    
    fis = new FileInputStream (bp2Xml);
    bpelem = _parserProxy.parseAllServiceElements(fis); 
    services.addAll(bpelem.getServices());
    references.addAll(bpelem.getReferences());
    
    // We expect:
    // bp.xml: 3 services and a reference
    // bp2.xml: 3 services and a reference list
    //
    assertTrue ("Six services expected, not " + services.size(), services.size() == 6);
    assertTrue ("Two references expected, not " + references.size(), references.size() == 2);
    
    Set<WrappedServiceMetadata> expectedServices = getTest2ExpectedServices();
    // ServiceResultSet will contain some services with autogenerated names starting '.' so we can't 
    // use a straight Set.equals(). We could add the autogenerated names to the expected results but instead
    // let's test that differsOnlyByName() works
    int serviceMatchesFound = 0;
    for (WrappedServiceMetadata result : services) { 
      Iterator<WrappedServiceMetadata> it = expectedServices.iterator();
      while (it.hasNext()) { 
        WrappedServiceMetadata next = it.next();
        if (result.equals(next) || result.identicalOrDiffersOnlyByName(next)) { 
          serviceMatchesFound++;
          it.remove();
        }
      }
    }
    
    assertEquals ("Parsed services are wrong: " + expectedServices + " unmatched ",
        6, serviceMatchesFound);
    
    Set<WrappedReferenceMetadata> expectedReferences = getTest2ExpectedReferences();
    Set<WrappedReferenceMetadata> results = new HashSet<WrappedReferenceMetadata>(references);
    assertTrue ("Parsed references are not as we'd expected: " + results.toString() + " != " + expectedReferences,
        results.equals(expectedReferences));
  }
  
  @Test
  public void checkMultiValues() throws Exception { 
    File bpXml = new File ("../src/test/resources", "appModeller/test1.eba/bundle1.jar/OSGI-INF/blueprint/bpMultiValues.xml");
    List<WrappedServiceMetadata> services = new ArrayList<WrappedServiceMetadata>();
    FileInputStream fis = new FileInputStream (bpXml);
    ParsedServiceElements bpelem = _parserProxy.parseAllServiceElements(fis); 
    services.addAll(bpelem.getServices());
    
    assertEquals ("Multi valued service not parsed correctly", services.size(), 1);
    
    WrappedServiceMetadata wsm = services.get(0);
    Map<String, Object> props = wsm.getServiceProperties();
    String [] intents = (String[]) props.get("service.intents");
    
    assertEquals ("Service.intents[0] wrong", intents[0], "propagatesTransaction");
    assertEquals ("Service.intents[1] wrong", intents[1], "confidentiality");
    
  }
  
  // model
  // <reference id="fromOutside" interface="foo.bar.MyInjectedService"/>
  // <reference-list id="refList1" interface="my.logging.services" filter="(active=true)"/>
  //
  private Set<WrappedReferenceMetadata> getTest2ExpectedReferences() throws Exception { 
    Set<WrappedReferenceMetadata> expectedResults = new HashSet<WrappedReferenceMetadata>();
         
    expectedResults.add(_modellingManager.getImportedService(false, "foo.bar.MyInjectedService", null, 
        null, "fromOutside", false));
    expectedResults.add(_modellingManager.getImportedService(false, "my.logging.service", null, "(&(trace=on)(debug=true))", "refList1", true));
    
    return expectedResults;
  }
  
  // Test 2 includes anonymous services: the expected results are a superset of test1
  private Set<WrappedServiceMetadata> getTest2ExpectedServices() { 
    Set<WrappedServiceMetadata> expectedResults = getTest1ExpectedResults();
        
    expectedResults.add(_modellingManager.getExportedService("", 0, Arrays.asList("foo.bar.AnonService"), null));
    expectedResults.add(_modellingManager.getExportedService("", 0, Arrays.asList("foo.bar.NamedInnerBeanService"), null));
    return expectedResults;
  }
  
  private Set<WrappedServiceMetadata> getTest1ExpectedResults() { 
    Set<WrappedServiceMetadata> expectedResults = new HashSet<WrappedServiceMetadata>();
    Map<String, Object> props = new HashMap<String, Object>();
    props.put ("priority", "9");
    props.put("volume", "11");
    props.put("osgi.service.blueprint.compname", "myBean");
    expectedResults.add(_modellingManager.getExportedService("myService", 0, Arrays.asList("foo.bar.MyService"), props));

    props = new HashMap<String, Object>();
    props.put ("priority", "7");
    props.put ("volume", "11");
    props.put ("osgi.service.blueprint.compname", "bean1");
    expectedResults.add(_modellingManager.getExportedService("service1.should.be.exported", 0, Arrays.asList("foo.bar.MyService"), props));
 
    props = new HashMap<String, Object>();
    props.put ("customer", "pig");
    props.put ("osgi.service.blueprint.compname", "bean2");
    expectedResults.add(_modellingManager.getExportedService("service2.should.not.be.exported", 0, Arrays.asList("com.acme.Delivery"), props));
        
    props = new HashMap<String, Object>();
    props.put ("customer", "pig");
    props.put ("target", "rabbit");
    props.put ("payload", "excessive");
    props.put ("osgi.service.blueprint.compname", "bean3");
    expectedResults.add(_modellingManager.getExportedService("bean3", 0, Arrays.asList("com.acme.Delivery"), props));
       
    return expectedResults;
  } 
}

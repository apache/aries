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
package org.apache.aries.application.modelling.utils;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.application.modelling.WrappedServiceMetadata;
import org.apache.aries.application.modelling.impl.ExportedServiceImpl;
import org.junit.Test;
public class ExportedServiceTest
{

  
  @Test
  public void checkEquality() { 
    
    //   public ExportedService (String name, int ranking, Collection<String> ifaces, 
    // Map<String, String> serviceProperties ) { 
    Map<String, Object> props = new HashMap<String, Object>();
    props.put ("away", "www.away.com");
    props.put ("home", "www.home.net");
    WrappedServiceMetadata wsm1 = new ExportedServiceImpl (null, 0, Arrays.asList("a.b.c", "d.e.f"), props); 
    WrappedServiceMetadata wsm2 = new ExportedServiceImpl (null, 0, Arrays.asList("d.e.f", "a.b.c"), props);
    
    assertTrue ("Basic equality test", wsm1.equals(wsm2));
    assertTrue ("Basic equality test", wsm2.equals(wsm1));
    assertTrue ("Hashcodes equal", wsm1.hashCode() == wsm2.hashCode());

    
        
    wsm2 = new ExportedServiceImpl (null, 0, Arrays.asList("d.e.f", "a.b.c", "g.e.f"), props);
    assertFalse ("Adding an interface makes them different", wsm1.equals(wsm2));
    assertFalse ("Adding an interface makes them different", wsm2.equals(wsm1));
    assertFalse ("Hashcodes should differ", wsm1.hashCode() == wsm2.hashCode());
    
    props = new HashMap<String, Object>(props);
    props.put("interim", "w3.interim.org");
    
    wsm1 = new ExportedServiceImpl (null, 0, Arrays.asList("a.b.c","d.e.f", "g.e.f"), props);
    
    assertFalse ("Adding a service property makes them different", wsm1.equals(wsm2));
    assertFalse ("Adding a service property makes them different", wsm2.equals(wsm1));
    assertFalse ("Hashcodes still different", wsm1.hashCode() == wsm2.hashCode());
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.blueprint.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;

@RunWith(JMock.class)
public class BlueprintMetadataTest {

    private BlueprintMetadata metadata;
    private Mockery mockery = new JUnit4Mockery();
    
    private BundleContext mockContext;
    private Bundle  mockBundle;
    private ServiceReference[] mockServiceReferences = new ServiceReference[1];
    //private ServiceReference mockServiceReference;
    private BlueprintContainer mockContainer;
    private ServiceMetadata mockServiceMetadata;
    private BeanMetadata mockBeanMetadata;
    
    @Before
    public void setUp() throws Exception {
        mockContext = mockery.mock(BundleContext.class);
        mockBundle = mockery.mock(Bundle.class);
        mockServiceReferences[0] = mockery.mock(ServiceReference.class);
        mockContainer = mockery.mock(BlueprintContainer.class);
        mockServiceMetadata = mockery.mock(ServiceMetadata.class);
        mockBeanMetadata = mockery.mock(BeanMetadata.class);
        
        metadata = new BlueprintMetadata(mockContext);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void validGetBlueprintContainerServiceId() throws Exception {
        final long bundleId = 12;
        final long serviceId = 7117;
        
        mockery.checking(new Expectations() {
            {
                allowing(mockContext).getBundle(bundleId);
                will(returnValue(mockBundle));
                oneOf(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                will(returnValue(mockServiceReferences));
            }
        });
        // is there any need?
        mockery.checking(new Expectations() {
            {
                allowing(mockBundle).getSymbolicName();
                will(returnValue("org.apache.geronimo.blueprint.testXXX"));
                allowing(mockBundle).getVersion();
                will(returnValue(Version.emptyVersion));
            }
        });
        mockery.checking(new Expectations() {
            {
                allowing(mockServiceReferences[0]).getProperty(Constants.SERVICE_ID);
                will(returnValue(serviceId));
            }
        });

        assertEquals(serviceId, metadata.getBlueprintContainerServiceId(bundleId));
    }

    @Test
    public void invalidParaInGetBlueprintContainerServiceId() throws Exception
    {
        mockery.checking(new Expectations() {
            {
                allowing(mockContext).getBundle(with(any(Long.class)));
                will(returnValue(null));
            }
        });
        try{
            metadata.getBlueprintContainerServiceId(-10);
        } catch(Exception ex)
        {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }
    
    @Test
    public void cannotFindAssociatedContainerServiceId() throws Exception
    {
        final long bundleId = 12;
        
        mockery.checking(new Expectations() {
            {
                allowing(mockContext).getBundle(bundleId);
                will(returnValue(mockBundle));
                oneOf(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                //return null if no services are registered which satisfy the search
                will(returnValue(null));
            }
        });
        // is there any need?
        mockery.checking(new Expectations() {
            {
                allowing(mockBundle).getSymbolicName();
                will(returnValue("org.apache.geronimo.blueprint.testXXX"));
                allowing(mockBundle).getVersion();
                will(returnValue(Version.emptyVersion));
            }
        });
        assertEquals(-1, metadata.getBlueprintContainerServiceId(bundleId));
    }
    
    @Test
    public void normalBlueprintContainerServiceIds() throws Exception {
        final long serviceId = 7117;
        final long [] serviceIds = new long[]{serviceId};
        
        mockery.checking(new Expectations() {
            {
                oneOf(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                will(returnValue(mockServiceReferences));
            }
        });
        mockery.checking(new Expectations() {
            {
                allowing(mockServiceReferences[0]).getProperty(Constants.SERVICE_ID);
                will(returnValue(serviceId));
            }
        });
        
        assertArrayEquals(serviceIds, metadata.getBlueprintContainerServiceIds());
    }

    @Test 
    public void noBlueprintContainerServiceIds() throws Exception
    {//It is impossible according to osgi spec, here just test the robustness of code
        mockery.checking(new Expectations() {
            {
                oneOf(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                //return null if no services are registered which satisfy the search
                will(returnValue(null));
            }
        });
        assertNull(metadata.getBlueprintContainerServiceIds());
    }
    
    @Test
    public void nomalGetComponentIds() throws Exception {
        final long serviceId = 7117;
        final Set cidset = getAsSet(new String[]{".component-1", ".component-2", ".component-5"});
        
        mockery.checking(new Expectations(){
            {
                oneOf(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                will(returnValue(mockServiceReferences));
                oneOf(mockContext).getService(mockServiceReferences[0]);
                will(returnValue(mockContainer));
            }
        });
        mockery.checking(new Expectations(){
            {
                oneOf(mockContainer).getComponentIds();
                will(returnValue(cidset));
            }
        });
        
        assertEquals(cidset, getAsSet(metadata.getComponentIds(serviceId)));
    }

    @Test
    public void normalGetComponentIdsByType() throws Exception {
        final long serviceId = 7117;
        final String [] cidarray = new String[]{".component-1"};
        final Collection cMetadatas = new ArrayList();
        cMetadatas.add(mockServiceMetadata);
        
        mockery.checking(new Expectations(){
            {
                oneOf(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                will(returnValue(mockServiceReferences));
                oneOf(mockContext).getService(mockServiceReferences[0]);
                will(returnValue(mockContainer));
            }
        });
        mockery.checking(new Expectations(){
            {
                oneOf(mockContainer).getMetadata(ServiceMetadata.class);
                will(returnValue(cMetadatas));
            }
        });
        mockery.checking(new Expectations(){
            {
                oneOf(mockServiceMetadata).getId();
                will(returnValue(cidarray[0]));
            }
        });
        
        assertArrayEquals(cidarray, 
                metadata.getComponentIdsByType(serviceId, BlueprintMetadataMBean.SERVICE_METADATA));
    }

    public void invalidParaInGetComponentIdsByType() throws Exception {
        final long serviceId = 7117;
                
        mockery.checking(new Expectations(){
            {
                allowing(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                will(returnValue(mockServiceReferences));
                allowing(mockContext).getService(mockServiceReferences[0]);
                will(returnValue(mockContainer));
            }
        });
        
        try {
            metadata.getComponentIdsByType(serviceId, null);
        }catch(Exception ex)
        {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        try {
            metadata.getComponentIdsByType(serviceId, BlueprintMetadataMBean.COMPONENT_METADATA);
        }catch(Exception ex)
        {
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }
    @Test
    public void testGetComponentMetadata() throws Exception {
        final long serviceId = 7117;
        final String componentId = ".component-1";
        final String [] cidarray = new String[]{componentId};
        final List emptyList = new ArrayList();
        
        mockery.checking(new Expectations(){
            {
                oneOf(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                will(returnValue(mockServiceReferences));
                oneOf(mockContext).getService(mockServiceReferences[0]);
                will(returnValue(mockContainer));
            }
        });
        mockery.checking(new Expectations(){
            {
                oneOf(mockContainer).getComponentMetadata(componentId);
                will(returnValue(mockBeanMetadata));
            }
        });
        mockery.checking(new Expectations(){
            {
                allowing(mockBeanMetadata).getDependsOn();
                will(returnValue(emptyList));

                allowing(mockBeanMetadata).getArguments();
                will(returnValue(emptyList));
                allowing(mockBeanMetadata).getFactoryComponent();
                will(returnValue(null));
                allowing(mockBeanMetadata).getProperties();
                will(returnValue(emptyList));
                ignoring(mockBeanMetadata);
            }
        });
        metadata.getComponentMetadata(serviceId, componentId);
        mockery.assertIsSatisfied();
    }
    
    @Test
    public void fail2GetBlueprintContainer() throws Exception
    {
        final long serviceId = 7117;
        mockery.checking(new Expectations(){
            {
                exactly(3).of(mockContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                will(returnValue(null));
            }
        });
        
        try{
            metadata.getComponentIds(serviceId);
        }catch(Exception ex)
        {
            assertTrue(ex instanceof IOException);
        }
        
        try{
            metadata.getComponentIdsByType(serviceId, BlueprintMetadataMBean.SERVICE_METADATA);
        }catch(Exception ex)
        {
            assertTrue(ex instanceof IOException);
        }
        
        try{
            metadata.getComponentMetadata(serviceId, "xxxx");
        }catch(Exception ex)
        {
            assertTrue(ex instanceof IOException);
        }
    }

    private Set getAsSet(String[] data) {
        Set dataSet = new HashSet();
        dataSet.addAll(Arrays.asList(data));
        return dataSet;
    }
        
}

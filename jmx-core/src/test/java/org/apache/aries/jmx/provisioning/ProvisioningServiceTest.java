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
package org.apache.aries.jmx.provisioning;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.jmx.JmxConstants.PROPERTIES_TYPE;
import static org.osgi.service.provisioning.ProvisioningService.PROVISIONING_AGENT_CONFIG;
import static org.osgi.service.provisioning.ProvisioningService.PROVISIONING_REFERENCE;
import static org.osgi.service.provisioning.ProvisioningService.PROVISIONING_RSH_SECRET;
import static org.osgi.service.provisioning.ProvisioningService.PROVISIONING_SPID;
import static org.osgi.service.provisioning.ProvisioningService.PROVISIONING_UPDATE_COUNT;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.zip.ZipInputStream;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.codec.PropertyData;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
public class ProvisioningServiceTest {

   
    @Test
    public void testAddInformationFromZip() throws Exception {

        org.osgi.service.provisioning.ProvisioningService provService = mock(org.osgi.service.provisioning.ProvisioningService.class);
        ProvisioningService mbean = new ProvisioningService(provService);
        ProvisioningService spiedMBean = spy(mbean);
        
        InputStream is = mock(InputStream.class);
        doReturn(is).when(spiedMBean).createStream("file://prov.zip");
        
        spiedMBean.addInformationFromZip("file://prov.zip");
        verify(provService).addInformation(any(ZipInputStream.class));
        verify(is).close();
        
    }

    
    @Test
    @SuppressWarnings("unchecked")
    public void testAddInformationWithTabularData() throws Exception {
        
        org.osgi.service.provisioning.ProvisioningService provService = mock(org.osgi.service.provisioning.ProvisioningService.class);
        ProvisioningService mbean = new ProvisioningService(provService);
        
        TabularData data = new TabularDataSupport(PROPERTIES_TYPE);
        PropertyData<byte[]> p1 = PropertyData.newInstance(PROVISIONING_AGENT_CONFIG, new byte[] { 20, 30, 40 });
        data.put(p1.toCompositeData());
        PropertyData<String> p2 = PropertyData.newInstance(PROVISIONING_SPID, "x.test");
        data.put(p2.toCompositeData());
        
        mbean.addInformation(data);
        ArgumentCaptor<Dictionary> dictionaryArgument = ArgumentCaptor.forClass(Dictionary.class);
        verify(provService).addInformation(dictionaryArgument.capture());
        
        Dictionary<String, Object> info = dictionaryArgument.getValue();
        assertEquals(2, info.size() );
        assertArrayEquals(new byte[] { 20, 30, 40 }, (byte[]) info.get(PROVISIONING_AGENT_CONFIG));
        assertEquals("x.test", info.get(PROVISIONING_SPID));
        
    }

    
    @Test
    public void testListInformation() throws Exception {

        org.osgi.service.provisioning.ProvisioningService provService = mock(org.osgi.service.provisioning.ProvisioningService.class);
        ProvisioningService mbean = new ProvisioningService(provService);
        
        Dictionary<String, Object> info = new Hashtable<String, Object>();
        info.put(PROVISIONING_AGENT_CONFIG, new byte[] { 20, 30, 40 });
        info.put(PROVISIONING_SPID, "x.test");
        info.put(PROVISIONING_REFERENCE, "rsh://0.0.0.0/provX");
        info.put(PROVISIONING_RSH_SECRET, new byte[] { 15, 25, 35 });
        info.put(PROVISIONING_UPDATE_COUNT, 1);
        
        when(provService.getInformation()).thenReturn(info);
        
        TabularData provData = mbean.listInformation();
        assertNotNull(provData);
        assertEquals(PROPERTIES_TYPE, provData.getTabularType());
        assertEquals(5, provData.values().size());
        PropertyData<byte[]> agentConfig = PropertyData.from(provData.get(new Object[]{ PROVISIONING_AGENT_CONFIG }));
        assertArrayEquals(new byte[] { 20, 30, 40 }, agentConfig.getValue());
        PropertyData<String> spid = PropertyData.from(provData.get(new Object[] { PROVISIONING_SPID }));
        assertEquals("x.test", spid.getValue());
        PropertyData<String> ref = PropertyData.from(provData.get(new Object[] { PROVISIONING_REFERENCE }));
        assertEquals("rsh://0.0.0.0/provX", ref.getValue());
        PropertyData<byte[]> sec = PropertyData.from(provData.get(new Object[] { PROVISIONING_RSH_SECRET }));
        assertArrayEquals(new byte[] { 15, 25, 35 }, sec.getValue());
        PropertyData<Integer> count = PropertyData.from(provData.get(new Object[] { PROVISIONING_UPDATE_COUNT }));
        assertEquals(new Integer(1), count.getValue());
        
    }

   
    @Test
    @SuppressWarnings("unchecked")
    public void testSetInformation() throws Exception {
      
        org.osgi.service.provisioning.ProvisioningService provService = mock(org.osgi.service.provisioning.ProvisioningService.class);
        ProvisioningService mbean = new ProvisioningService(provService);
        
        TabularData data = new TabularDataSupport(PROPERTIES_TYPE);
        PropertyData<String> p1 = PropertyData.newInstance(PROVISIONING_REFERENCE, "rsh://0.0.0.0/provX");
        data.put(p1.toCompositeData());
        PropertyData<String> p2 = PropertyData.newInstance(PROVISIONING_SPID, "x.test");
        data.put(p2.toCompositeData());
        
        mbean.setInformation(data);
        
        ArgumentCaptor<Dictionary> dictionaryArgument = ArgumentCaptor.forClass(Dictionary.class);
        verify(provService).setInformation(dictionaryArgument.capture());
        
        Dictionary<String, Object> info = dictionaryArgument.getValue();
        assertEquals(2, info.size() );
        assertEquals("rsh://0.0.0.0/provX", info.get(PROVISIONING_REFERENCE));
        assertEquals("x.test", info.get(PROVISIONING_SPID));
        
    }

}

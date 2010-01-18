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

import junit.framework.TestCase;

import org.apache.aries.spifly.Activator;
import org.apache.aries.spifly.SPIBundleTracker;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

public class ActivatorTest extends TestCase {
    public void testActivatorAndLogFunctionality() throws Exception {
        LogService ls = EasyMock.createMock(LogService.class);
        EasyMock.replay(ls);
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(sr);
        
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.createFilter((String) EasyMock.anyObject())).andAnswer(new IAnswer<Filter>() {
            public Filter answer() throws Throwable {
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        });
        EasyMock.expect(bc.getService(sr)).andReturn(ls);
        EasyMock.replay(bc);
        
        Activator a = new Activator();
        a.start(bc);
        
        assertTrue(a.bt instanceof SPIBundleTracker);
        assertNotNull(a.lst);
        assertEquals(0, a.logServices.size());
        
        a.log(LogService.LOG_ERROR, "yo");
        a.log(LogService.LOG_WARNING, "help", new Throwable());
        EasyMock.verify(ls);
        
        a.lst.addingService(sr);
        assertEquals(1, a.logServices.size());
        
        EasyMock.reset(ls);
        ls.log(EasyMock.anyInt(), (String) EasyMock.anyObject());
        ls.log(EasyMock.anyInt(), (String) EasyMock.anyObject(), (Throwable) EasyMock.anyObject());
        EasyMock.replay(ls);
        
        a.log(LogService.LOG_ERROR, "ha");
        a.log(LogService.LOG_WARNING, "hey", new Throwable());
        EasyMock.verify(ls);
        
        a.lst.removedService(EasyMock.createMock(ServiceReference.class), new Object());
        assertEquals(1, a.logServices.size());
        
        a.lst.removedService(sr, ls);
        assertEquals(0, a.logServices.size());
    }
    
    public void testActivatorStop() throws Exception {
        Activator a = new Activator();
        
        a.bt = EasyMock.createMock(BundleTracker.class);
        a.bt.close();
        EasyMock.replay(a.bt);
        
        a.lst = EasyMock.createMock(ServiceTracker.class);
        a.lst.close();
        EasyMock.replay(a.lst);

        a.stop(null);
        EasyMock.verify(a.bt);
        EasyMock.verify(a.lst);
    }
}

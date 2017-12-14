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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.whiteboard;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;

public class JmxWhiteboardSupportTest {
    @Test
    public void testRegisterMBean() throws Exception {
        final Logger ml = Mockito.mock(Logger.class);

        JmxWhiteboardSupport s = new JmxWhiteboardSupport() {
            @Override
            MBeanHolder createMBeanHolder(Object mbean, ObjectName objectName) {
                MBeanHolder mbh = super.createMBeanHolder(mbean, objectName);
                setPrivateField(mbh, "log", ml);
                return mbh;
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("jmx.objectname", "test:key=val");

        ServiceReference<?> sref = Mockito.mock(ServiceReference.class);
        Mockito.when(sref.getProperty(Mockito.anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return props.get(invocation.getArguments()[0].toString());
            }
        });

        MBeanServer ms = mockMBeanServer();

        s.addMBeanServer(ms);
        Mockito.verify(ml, Mockito.never()).error(Mockito.anyString(), Mockito.isA(Throwable.class));
        Mockito.verify(ml, Mockito.never()).warn(Mockito.anyString(), Mockito.isA(Throwable.class));
        s.registerMBean(Mockito.mock(DynamicMBean.class), sref);
        Mockito.verify(ml, Mockito.never()).error(Mockito.anyString(), Mockito.isA(Throwable.class));
        Mockito.verify(ml, Mockito.never()).warn(Mockito.anyString(), Mockito.isA(Throwable.class));
    }

    @Test
    public void testRegisterMBeanError() throws Exception {
        final Logger ml = Mockito.mock(Logger.class);

        JmxWhiteboardSupport s = new JmxWhiteboardSupport() {
            @Override
            MBeanHolder createMBeanHolder(Object mbean, ObjectName objectName) {
                MBeanHolder mbh = super.createMBeanHolder(mbean, objectName);
                setPrivateField(mbh, "log", ml);
                return mbh;
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("jmx.objectname", "test:key=val");

        ServiceReference<?> sref = Mockito.mock(ServiceReference.class);
        Mockito.when(sref.getProperty(Mockito.anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return props.get(invocation.getArguments()[0].toString());
            }
        });

        MBeanServer ms = Mockito.mock(MBeanServer.class);
        Mockito.when(ms.registerMBean(Mockito.any(), Mockito.eq(new ObjectName("test:key=val")))).
        thenThrow(new InstanceAlreadyExistsException());

        s.addMBeanServer(ms);
        Mockito.verify(ml, Mockito.never()).error(Mockito.anyString(), Mockito.isA(Throwable.class));
        Mockito.verify(ml, Mockito.never()).warn(Mockito.anyString(), Mockito.isA(Throwable.class));
        s.registerMBean(Mockito.mock(DynamicMBean.class), sref);
        Mockito.verify(ml, Mockito.times(1)).error(Mockito.anyString(), Mockito.isA(Throwable.class));
        Mockito.verify(ml, Mockito.never()).warn(Mockito.anyString(), Mockito.isA(Throwable.class));
    }

    @Test
    public void testRegisterMBeanWarning() throws Exception {
        final Logger ml = Mockito.mock(Logger.class);

        JmxWhiteboardSupport s = new JmxWhiteboardSupport() {
            @Override
            MBeanHolder createMBeanHolder(Object mbean, ObjectName objectName) {
                MBeanHolder mbh = super.createMBeanHolder(mbean, objectName);
                setPrivateField(mbh, "log", ml);
                return mbh;
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("jmx.objectname", "test:key=val");
        props.put("warning.exceptions", new String [] {InstanceAlreadyExistsException.class.getName(),
                MBeanRegistrationException.class.getName()});

        ServiceReference<?> sref = Mockito.mock(ServiceReference.class);
        Mockito.when(sref.getProperty(Mockito.anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return props.get(invocation.getArguments()[0].toString());
            }
        });

        MBeanServer ms = Mockito.mock(MBeanServer.class);
        Mockito.when(ms.registerMBean(Mockito.any(), Mockito.eq(new ObjectName("test:key=val")))).
        thenThrow(new InstanceAlreadyExistsException());

        s.addMBeanServer(ms);
        Mockito.verify(ml, Mockito.never()).error(Mockito.anyString(), Mockito.isA(Throwable.class));
        Mockito.verify(ml, Mockito.never()).warn(Mockito.anyString(), Mockito.isA(Throwable.class));
        s.registerMBean(Mockito.mock(DynamicMBean.class), sref);
        Mockito.verify(ml, Mockito.never()).error(Mockito.anyString(), Mockito.isA(Throwable.class));
        Mockito.verify(ml, Mockito.times(1)).warn(Mockito.anyString(), Mockito.isA(Throwable.class));
    }

    private MBeanServer mockMBeanServer()
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        MBeanServer ms = Mockito.mock(MBeanServer.class);
        Mockito.when(ms.registerMBean(Mockito.any(), Mockito.isA(ObjectName.class))).then(
            new Answer<ObjectInstance>() {
                @Override
                public ObjectInstance answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    return new ObjectInstance(((ObjectName) args[1]).toString(), args[0].getClass().getName());
                }
        });
        return ms;
    }

    private void setPrivateField(Object obj, String name, Object val) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

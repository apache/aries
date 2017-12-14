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
import java.util.Map;

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
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MBeanHolderTest {
    @Test
    public void testPlainRegistration() throws Exception {
        MBeanHolder mbh = new MBeanHolder(new Object(), new ObjectName("foo:bar=123"));
        Map<?,?> registrations = (Map<?,?>) getPrivateField(mbh, "registrations");
        Logger ml = Mockito.mock(Logger.class);
        setPrivateField(mbh, "log", ml);

        MBeanServer ms = mockMBeanServer();

        assertEquals(0, registrations.size());
        mbh.register(ms, new String [] {});
        assertEquals(1, registrations.size());
        assertEquals(new ObjectName("foo:bar=123"), registrations.get(ms));
        Mockito.verify(ml, Mockito.never()).error(Mockito.anyString(), Mockito.isA(Throwable.class));
    }

    @Test
    public void testRegistrationException() throws Exception {
        MBeanHolder mbh = new MBeanHolder(new Object(), new ObjectName("foo:bar=123"));
        Map<?,?> registrations = (Map<?,?>) getPrivateField(mbh, "registrations");
        Logger ml = Mockito.mock(Logger.class);
        setPrivateField(mbh, "log", ml);

        MBeanServer ms = Mockito.mock(MBeanServer.class);
        Mockito.when(ms.registerMBean(Mockito.any(), Mockito.eq(new ObjectName("foo:bar=123")))).
            thenThrow(new InstanceAlreadyExistsException());

        assertEquals(0, registrations.size());
        mbh.register(ms, new String [] {});
        assertEquals(0, registrations.size());
        Mockito.verify(ml, Mockito.times(1)).error(Mockito.anyString(), Mockito.isA(InstanceAlreadyExistsException.class));
    }

    @Test
    public void testExceptionsAsWarnings() throws Exception {
        MBeanHolder mbh = new MBeanHolder(new Object(), new ObjectName("foo:bar=123"));
        Map<?,?> registrations = (Map<?,?>) getPrivateField(mbh, "registrations");
        Logger ml = Mockito.mock(Logger.class);
        setPrivateField(mbh, "log", ml);

        MBeanServer ms = Mockito.mock(MBeanServer.class);
        Mockito.when(ms.registerMBean(Mockito.any(), Mockito.eq(new ObjectName("foo:bar=123")))).
            thenThrow(new InstanceAlreadyExistsException());

        assertEquals(0, registrations.size());
        mbh.register(ms, new String [] {InstanceAlreadyExistsException.class.getName()});
        assertEquals(0, registrations.size());
        Mockito.verify(ml, Mockito.times(1)).warn(Mockito.anyString(), Mockito.isA(InstanceAlreadyExistsException.class));
    }

    @Test
    public void testExceptionsAsWarnings2() throws Exception {
        MBeanHolder mbh = new MBeanHolder(new Object(), new ObjectName("foo:bar=123"));
        Map<?,?> registrations = (Map<?,?>) getPrivateField(mbh, "registrations");
        Logger ml = Mockito.mock(Logger.class);
        setPrivateField(mbh, "log", ml);

        MBeanServer ms = Mockito.mock(MBeanServer.class);
        Mockito.when(ms.registerMBean(Mockito.any(), Mockito.eq(new ObjectName("foo:bar=123")))).
            thenThrow(new NullPointerException());

        assertEquals(0, registrations.size());
        mbh.register(ms, new String [] {InstanceAlreadyExistsException.class.getName(), NullPointerException.class.getName()});
        assertEquals(0, registrations.size());
        Mockito.verify(ml, Mockito.times(1)).warn(Mockito.anyString(), Mockito.isA(NullPointerException.class));
    }

    @Test
    public void testThrowRuntimeException() throws Exception {
        MBeanHolder mbh = new MBeanHolder(new Object(), new ObjectName("foo:bar=123"));
        Map<?,?> registrations = (Map<?,?>) getPrivateField(mbh, "registrations");
        Logger ml = Mockito.mock(Logger.class);
        setPrivateField(mbh, "log", ml);

        MBeanServer ms = Mockito.mock(MBeanServer.class);
        Mockito.when(ms.registerMBean(Mockito.any(), Mockito.eq(new ObjectName("foo:bar=123")))).
            thenThrow(new NullPointerException());

        assertEquals(0, registrations.size());
        try {
            mbh.register(ms, new String [] {});
            fail("Should have thrown a NullPointerException");
        } catch (NullPointerException npe) {
            // good!
        }
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

    private Object getPrivateField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }

    private void setPrivateField(Object obj, String name, Object val)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, val);
    }
}

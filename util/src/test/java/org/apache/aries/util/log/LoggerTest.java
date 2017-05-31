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
package org.apache.aries.util.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

@SuppressWarnings("rawtypes")
public class LoggerTest {
    private BundleContext ctx;

    @Before
    public void setup() {
        ctx = Skeleton.newMock(new BundleContextMock(), BundleContext.class);
    }

    @After
    public void teardown() {
        BundleContextMock.clear();
    }

    @Test
    public void testLogger() {
        Logger logger = new Logger(ctx);
        logger.open();

        logger.log(LogService.LOG_INFO, "This message will disappear");

        TestLogService tls = new TestLogService();
        ctx.registerService(LogService.class.getName(), tls, null);

        Assert.assertEquals("Precondition", 0, tls.logData.size());
        logger.log(LogService.LOG_INFO, "Hi");

        Assert.assertEquals(1, tls.logData.size());
        LogData ld = tls.logData.get(0);
        Assert.assertEquals(LogService.LOG_INFO, ld.level);
        Assert.assertEquals("Hi", ld.message);

        logger.close();

        logger.log(LogService.LOG_INFO, "This message will disappear too");
        Assert.assertEquals("The logger was closed so log messages shouldn't appear in the LogService any more",
                1, tls.logData.size());
    }

    @Test
    public void testLogger2() {
        Logger logger = new Logger(ctx);
        logger.open();
        TestLogService tls = new TestLogService();
        ctx.registerService(LogService.class.getName(), tls, null);

        Assert.assertEquals("Precondition", 0, tls.logData.size());
        Throwable th = new Throwable();
        logger.log(LogService.LOG_INFO, "Hi", th);

        Assert.assertEquals(1, tls.logData.size());
        LogData ld = tls.logData.get(0);
        Assert.assertEquals(LogService.LOG_INFO, ld.level);
        Assert.assertEquals("Hi", ld.message);
        Assert.assertEquals(th, ld.throwable);
    }

    @Test
    public void testLogger3() {
        Logger logger = new Logger(ctx);
        logger.open();
        TestLogService tls = new TestLogService();
        ctx.registerService(LogService.class.getName(), tls, null);

        Assert.assertEquals("Precondition", 0, tls.logData.size());
        ServiceReference sr = new MockServiceReference();
        logger.log(sr, LogService.LOG_INFO, "Hi");

        Assert.assertEquals(1, tls.logData.size());
        LogData ld = tls.logData.get(0);
        Assert.assertSame(sr, ld.serviceReference);
        Assert.assertEquals(LogService.LOG_INFO, ld.level);
        Assert.assertEquals("Hi", ld.message);
    }

    @Test
    public void testLogger4() {
        Logger logger = new Logger(ctx);
        logger.open();
        TestLogService tls = new TestLogService();
        ctx.registerService(LogService.class.getName(), tls, null);

        Assert.assertEquals("Precondition", 0, tls.logData.size());
        ServiceReference sr = new MockServiceReference();
        Throwable th = new Throwable();
        logger.log(sr, LogService.LOG_INFO, "Hi", th);

        Assert.assertEquals(1, tls.logData.size());
        LogData ld = tls.logData.get(0);
        Assert.assertSame(sr, ld.serviceReference);
        Assert.assertEquals(LogService.LOG_INFO, ld.level);
        Assert.assertEquals("Hi", ld.message);
        Assert.assertEquals(th, ld.throwable);
    }

    private class TestLogService implements LogService {
        List<LogData> logData = Collections.synchronizedList(new ArrayList<LogData>());

        public void log(int level, String message) {
            logData.add(new LogData(null, level, message, null));
        }

        public void log(int level, String message, Throwable exception) {
            logData.add(new LogData(null, level, message, exception));
        }

        public void log(ServiceReference sr, int level, String message) {
            logData.add(new LogData(sr, level, message, null));
        }

        public void log(ServiceReference sr, int level, String message, Throwable exception) {
            logData.add(new LogData(sr, level, message, exception));
        }
    }

    private static class LogData {
        private final ServiceReference serviceReference;
        private final int level;
        private final String message;
        private final Throwable throwable;

        private LogData(ServiceReference sr, int level, String message, Throwable th) {
            this.serviceReference = sr;
            this.level = level;
            this.message = message;
            this.throwable = th;
        }
    }

    private static class MockServiceReference implements ServiceReference {
        public Object getProperty(String key) {
            return null;
        }

        public String[] getPropertyKeys() {
            return null;
        }

        public Bundle getBundle() {
            return null;
        }

        public Bundle[] getUsingBundles() {
            return null;
        }

        public boolean isAssignableTo(Bundle bundle, String className) {
            return false;
        }

        public int compareTo(Object reference) {
            return 0;
        }
    }
}

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

package org.apache.aries.transaction.internal;

import java.io.File;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.geronimo.transaction.manager.NamedXAResource;
import org.apache.geronimo.transaction.manager.NamedXAResourceFactory;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LogTest {

    int minThreads = 100;
    int maxThreads = 100;
    int minTxPerThread = 1000;
    int maxTxPerThread = 1000;

    @Before
    public void setUp() {
        System.setProperty("org.slf4j.simplelogger.defaultlog", "error");
    }

    @Test
    @Ignore
    public void testGeronimo() throws Exception {
        System.err.println("Geronimo");
        XidFactory xidFactory = new XidFactoryImpl("hi".getBytes());
        HOWLLog txLog = new HOWLLog("org.objectweb.howl.log.BlockLogBuffer",
                4,
                true,
                true,
                50,
                new File(".").getAbsolutePath(),
                "log",
                "geronimo",
                512,
                0,
                2,
                4,
                -1,
                true,
                xidFactory,
                null);
        txLog.doStart();
        GeronimoTransactionManager tm = new GeronimoTransactionManager(600, xidFactory, txLog);
        XAResource xar1 = new TestXAResource("res1");
        XAResource xar2 = new TestXAResource("res2");
        tm.registerNamedXAResourceFactory(new TestXAResourceFactory("res1"));
        tm.registerNamedXAResourceFactory(new TestXAResourceFactory("res2"));
        for (int i = minThreads; i <= maxThreads; i *= 10) {
            for (int j = minTxPerThread; j <= maxTxPerThread; j *= 10) {
                long ms = testThroughput(tm, xar1, xar2, i, j);
                System.err.println("TPS (" + i + " threads, " + j + " tx) = " + ((i * j) / (ms / 1000.0)));
            }
        }
        txLog.doStop();
        System.err.println();
        System.err.flush();
    }

    public long testThroughput(final TransactionManager tm, final XAResource xar1, final XAResource xar2, final int nbThreads, final int nbTxPerThread) throws Exception {
        Thread[] threads = new Thread[nbThreads];
        for (int thIdx = 0; thIdx < nbThreads; thIdx++) {
            threads[thIdx] = new Thread() {
                @Override
                public void run() {
                    try {
                        for (int txIdx = 0; txIdx < nbTxPerThread; txIdx++) {
                            tm.begin();
                            Transaction tx = tm.getTransaction();
                            tx.enlistResource(xar1);
                            tx.enlistResource(xar2);
                            tx.commit();
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            };
        }
        long t0 = System.currentTimeMillis();
        for (int thIdx = 0; thIdx < nbThreads; thIdx++) {
            threads[thIdx].start();
        }
        for (int thIdx = 0; thIdx < nbThreads; thIdx++) {
            threads[thIdx].join();
        }
        long t1 = System.currentTimeMillis();
        return t1 - t0;
    }

    public static class TestXAResourceFactory implements NamedXAResourceFactory {
        private final String name;
        public TestXAResourceFactory(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public NamedXAResource getNamedXAResource() throws SystemException {
            return new TestXAResource(name);
        }
        public void returnNamedXAResource(NamedXAResource namedXAResource) {
        }
    }

    public static class TestXAResource implements XAResource, NamedXAResource {
        private final String name;
        public TestXAResource(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public void commit(Xid xid, boolean b) throws XAException {
        }
        public void end(Xid xid, int i) throws XAException {
        }
        public void forget(Xid xid) throws XAException {
        }
        public int getTransactionTimeout() throws XAException {
            return 0;
        }
        public boolean isSameRM(XAResource xaResource) throws XAException {
            return xaResource instanceof TestXAResource && ((TestXAResource) xaResource).name.equals(name);
        }
        public int prepare(Xid xid) throws XAException {
            return 0;
        }
        public Xid[] recover(int i) throws XAException {
            return new Xid[0];
        }
        public void rollback(Xid xid) throws XAException {
        }
        public boolean setTransactionTimeout(int i) throws XAException {
            return false;
        }
        public void start(Xid xid, int i) throws XAException {
        }
    }

}

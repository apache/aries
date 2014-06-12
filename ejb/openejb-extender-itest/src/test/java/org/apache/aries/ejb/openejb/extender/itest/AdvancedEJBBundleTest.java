/*  Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.aries.ejb.openejb.extender.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;

import beans.integration.Tx;
import beans.integration.impl.JPASingleton;
import beans.jpa.Laptop;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AdvancedEJBBundleTest extends AbstractOpenEJBTest {

    @Configuration
    public static Option[] jpaConfig() {
        return options(
                mavenBundle("org.apache.derby", "derby").versionAsInProject(),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api").versionAsInProject(),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container").versionAsInProject(),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context").versionAsInProject(),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp").versionAsInProject(),
                mavenBundle("org.apache.openjpa", "openjpa").versionAsInProject(),
                mavenBundle("commons-pool", "commons-pool").versionAsInProject()
        );
    }

    @Test
    @Ignore
    public void testTransactionalEJB() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "TX_MANIFEST.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "beans/integration/impl/TxSingleton.class");
        addToZip(zos, "beans/integration/Tx.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();

            Object bean = context().getService(context().getServiceReference(Tx.class.getName()));
            UserTransaction ut = context().getService(UserTransaction.class);

            Method no = bean.getClass().getMethod("getNoTransactionId");
            Method maybe = bean.getClass().getMethod("getMaybeTransactionId");
            Method tx = bean.getClass().getMethod("getTransactionId");
            Method newTx = bean.getClass().getMethod("getNewTransactionId");

            assertNull(no.invoke(bean));
            assertNull(maybe.invoke(bean));
            assertNotNull(tx.invoke(bean));
            assertNotNull(newTx.invoke(bean));

            ut.begin();
            try {
                Object key = context().getService(TransactionSynchronizationRegistry.class).
                        getTransactionKey();

                assertNotNull(key);
                assertNull(no.invoke(bean));
                assertSame(key, maybe.invoke(bean));
                assertSame(key, tx.invoke(bean));
                Object o = newTx.invoke(bean);
                assertNotNull(o);
                assertNotSame(key, o);
            } finally {
                ut.commit();
            }
            test.stop();
        } finally {
            test.uninstall();
        }
    }

    @Test
    @Ignore
    public void testJPAContextSharing() throws Exception {

        System.setProperty("openejb.validation.output.level", "VERBOSE");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "JPA_MANIFEST.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "persistence.xml", "META-INF/persistence.xml");
        addToZip(zos, "beans/integration/impl/JPASingleton.class");
        addToZip(zos, "beans/jpa/Laptop.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();

            PersistenceContextProvider provider = context().getService(PersistenceContextProvider.class);

            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
            provider.registerContext("ejb-test", context().getBundle(), props);


            Object bean = context().getService(context().getServiceReference(JPASingleton.class.getName()));
            UserTransaction ut = context().getService(UserTransaction.class);

            Method m = bean.getClass().getMethod("editEntity", String.class);

            EntityManager em = context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=ejb-test)("
                    + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true)" +
                    "(" + PersistenceContextProvider.PROXY_FACTORY_EMF_ATTRIBUTE + "=*))").createEntityManager();

            ut.begin();
            try {

                Object e = test.loadClass(Laptop.class.getName()).newInstance();

                e.getClass().getMethod("setSerialNumber", String.class).invoke(e, "ABC123");
                e.getClass().getMethod("setNumberOfCores", int.class).invoke(e, 1);

                em.persist(e);

                m.invoke(bean, "ABC123");

                assertEquals(4, e.getClass().getMethod("getNumberOfCores").invoke(e));
                assertEquals(Integer.MAX_VALUE, e.getClass().getMethod("getHardDiskSize").invoke(e));

            } finally {
                ut.commit();
            }
            test.stop();
        } finally {
            test.uninstall();
        }
    }

}

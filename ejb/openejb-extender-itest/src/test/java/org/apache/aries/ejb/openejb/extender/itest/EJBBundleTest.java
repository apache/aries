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
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import beans.StatelessSessionBean;
import beans.xml.LocalIface;
import beans.xml.RemoteIface;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class EJBBundleTest extends AbstractOpenEJBTest {

    private void assertXML(Bundle test, boolean exists) throws Exception {
        ServiceReference[] local = context().getAllServiceReferences(LocalIface.class.getName(),
                "(&(ejb.name=XML)(ejb.type=Singleton))");
        if (exists) {
            assertNotNull(local);
            assertEquals(1, local.length);
            Object svc = context().getService(local[0]);
            assertNotNull(svc);
            assertEquals("A Local Call", svc.getClass().getMethod("getLocalString").invoke(svc));
        } else {
            assertNull(local);
        }

        ServiceReference[] remote = context().getAllServiceReferences(RemoteIface.class.getName(),
                "(&(ejb.name=XML)(ejb.type=Singleton))");
        if (exists) {
            assertNotNull(remote);
            assertEquals(1, remote.length);
            Object svc = context().getService(remote[0]);
            assertNotNull(svc);
            assertEquals("A Remote Call", svc.getClass().getMethod("getRemoteString").invoke(svc));
        } else {
            assertNull(remote);
        }
    }

    private void assertAnnotations(Bundle test, boolean exists) throws Exception {
        ServiceReference[] stateless = context().getAllServiceReferences(StatelessSessionBean.class.getName(),
                "(&(ejb.name=Annotated)(ejb.type=Stateless))");
        if (exists) {
            assertNotNull(stateless);
            assertEquals(1, stateless.length);
            Object svc = context().getService(stateless[0]);
            assertNotNull(svc);
            assertEquals("A Stateless Call", svc.getClass().getMethod("getStatelessString").invoke(svc));
        } else {
            assertNull(stateless);
        }
    }

    @Test
    public void testEJBJARInZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_1.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            test.stop();
            assertXML(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARAndAnnotatedInZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_1.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            assertAnnotations(test, true);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testAnnotatedOnlyInZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_1.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, false);
            assertAnnotations(test, true);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARAndAnnotatedNotOnClasspathInZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_2.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class", "yes/beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class", "yes/beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class", "yes/beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class", "no/beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class", "no/beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            assertAnnotations(test, false);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARAndAnnotatedOnClasspathInZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_2.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class", "yes/beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class", "yes/beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class", "yes/beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class", "yes/beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class", "yes/beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            assertAnnotations(test, true);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARInWebZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            assertAnnotations(test, false);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARInWrongPlaceWebZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "META-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, false);
            assertAnnotations(test, false);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARAndAnnotatedInWebZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            assertAnnotations(test, true);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testAnnotatedOnlyInWebZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_3.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, false);
            assertAnnotations(test, true);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARAndAnnotatedNotOnClasspathInWebZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_4.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class", "yes/beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class", "yes/beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class", "yes/beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class", "no/beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class", "no/beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            assertAnnotations(test, false);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

    @Test
    public void testEJBJARAndAnnotatedOnClasspathInWebZip() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addToZip(zos, "MANIFEST_4.MF", "META-INF/MANIFEST.MF");
        addToZip(zos, "ejb-jar.xml", "WEB-INF/ejb-jar.xml");
        addToZip(zos, "beans/xml/LocalIface.class", "yes/beans/xml/LocalIface.class");
        addToZip(zos, "beans/xml/RemoteIface.class", "yes/beans/xml/RemoteIface.class");
        addToZip(zos, "beans/xml/XMLBean.class", "yes/beans/xml/XMLBean.class");
        addToZip(zos, "beans/StatelessSessionBean.class", "yes/beans/StatelessSessionBean.class");
        addToZip(zos, "beans/StatefulSessionBean.class", "yes/beans/StatefulSessionBean.class");
        zos.close();

        Bundle test = context().installBundle("", new ByteArrayInputStream(baos.toByteArray()));

        try {
            test.start();
            assertXML(test, true);
            assertAnnotations(test, true);
            test.stop();
            assertXML(test, false);
            assertAnnotations(test, false);

        } finally {
            test.uninstall();
        }
    }

}

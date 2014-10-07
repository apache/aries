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
package org.apache.aries.jpa.quiesce.itest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.apache.aries.quiesce.manager.QuiesceCallback;
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.junit.After;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@ExamReactorStrategy(PerClass.class)
public abstract class AbstractQuiesceJPATest extends AbstractJPAItest {
    protected static final int WAIT_TIME = 200;
    protected static final String JPA_CONTAINER = "org.apache.aries.jpa.container";
    protected static final String JPA_CONTEXT = "org.apache.aries.jpa.container.context";
    protected static final String TEST_BUNDLE = "org.apache.aries.jpa.org.apache.aries.jpa.container.itest.bundle";
    
    protected static Logger LOG = LoggerFactory.getLogger(AbstractQuiesceJPATest.class);

    @Inject
    UserTransaction tm;

    //This is load bearing. we have to wait to create the EntityManager until the DataSource is available
    @Inject
    DataSource ds;
    

    @Configuration
    public Option[] configuration() {
        return options(
                baseOptions(),
                ariesJpa(),
                openJpa(),
                testDs(),
                testBundle()
                );
    }

    @After
    public void restartTestBundles() throws BundleException {
        restartTestBundle();
        restartBundle(JPA_CONTAINER);
        restartBundle(JPA_CONTEXT);
        try {
            tm.rollback();
        } catch (Exception e) {
            // Ignore
        }
    }

    protected List<Bundle> getListForTestBundle() {
        return Collections.singletonList(
                context().getBundleByName(TEST_BUNDLE));
    }

    protected void assertFinished(TestQuiesceCallback callback) {
        assertTrue("Quiesce not finished", callback.bundleClearedUp());
    }

    protected void assertNotFinished(TestQuiesceCallback... callbacks)
            throws InterruptedException {
        Thread.sleep(WAIT_TIME);
        for (TestQuiesceCallback callback : callbacks) {
            assertFalse("Quiesce finished ", callback.bundleClearedUp());
        }
    }

    protected void assertNoEMFForTestUnit() throws InvalidSyntaxException {
        assertNull("No unit should exist", getEMFRefs(TEST_UNIT));
    }

    protected void assertNoProxyEMFForTestUnit() throws InvalidSyntaxException {
        assertNull("No context should exist", getProxyEMFRefs(TEST_UNIT));
    }

    protected TestQuiesceCallback quiesce(String participantName, String bundleName) throws InvalidSyntaxException {
        QuiesceParticipant participant = getParticipant(participantName);
        TestQuiesceCallback callback = new TestQuiesceCallback();
        participant.quiesce(callback, Collections.singletonList(context().getBundleByName(bundleName)));
        return callback;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected QuiesceParticipant getParticipant(String bundleName) throws InvalidSyntaxException {
        ServiceReference[] refs = bundleContext.getServiceReferences(QuiesceParticipant.class.getName(), null);

        if(refs != null) {
            for(ServiceReference ref : refs) {
                if(ref.getBundle().getSymbolicName().equals(bundleName))
                    return (QuiesceParticipant) bundleContext.getService(ref);
            }
        }


        return null;
    }

    protected void restartTestBundle() throws BundleException {
        restartBundle(TEST_BUNDLE_NAME);
    }

    protected void restartBundle(String bundleName) throws BundleException {
        Bundle b = context().getBundleByName(bundleName);
        b.stop();
        b.start();
    }

    protected class TestQuiesceCallback implements QuiesceCallback{
        protected int calls = 0;

        public void bundleQuiesced(Bundle... bundles) {
            for (Bundle bundle : bundles) {
                LOG.info("Bundle quiesced " + bundle.getSymbolicName());
            }
            calls++;
        }

        public boolean bundleClearedUp() {
            return calls == 1;
        }
    }

    protected class MultiQuiesceCallback extends TestQuiesceCallback implements QuiesceCallback{
        private boolean contextFirst = true;

        public void bundleQuiesced(Bundle... bundles) {
            if (++calls == 1) {
                try {
                    getEMF(TEST_UNIT);
                } catch (Throwable t){
                    contextFirst = false;
                    if(t instanceof RuntimeException)
                        throw (RuntimeException) t;
                    else if (t instanceof Error)
                        throw (Error) t;
                    else
                        throw new RuntimeException(t);
                }
            }
        }

        public boolean bundleClearedUp()
        {
            return calls == 2 && contextFirst;
        }
    }

}

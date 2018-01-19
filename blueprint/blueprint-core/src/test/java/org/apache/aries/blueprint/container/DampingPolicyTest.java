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
package org.apache.aries.blueprint.container;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.blueprint.ExtendedReferenceMetadata;
import org.apache.aries.blueprint.container.SatisfiableRecipe.SatisfactionListener;
import org.apache.aries.blueprint.reflect.ReferenceMetadataImpl;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

public class DampingPolicyTest {

    @Test
    public void testGreedy() throws InvalidSyntaxException {
        ExtendedBlueprintContainer container = EasyMock.createMock(ExtendedBlueprintContainer.class);
        BundleContext containerContext = EasyMock.createMock(BundleContext.class);

        ReferenceMetadataImpl metadata = new ReferenceMetadataImpl();
        metadata.setInterface("my.interface");
        metadata.setDamping(ExtendedReferenceMetadata.DAMPING_GREEDY);

        final AtomicReference<ServiceReference> currentReference = new AtomicReference<ServiceReference>();

        ReferenceRecipe recipe = new ReferenceRecipe(
                "myref",
                container,
                metadata,
                null, null, null
        ) {
            @Override
            protected void bind(ServiceReference ref) {
                currentReference.set(ref);
                super.bind(ref);
            }
        };

        SatisfactionListener listener = new SatisfactionListener() {
            @Override
            public void notifySatisfaction(SatisfiableRecipe satisfiable) {

            }
        };
        ServiceReference svcRef1 = EasyMock.createMock(ServiceReference.class);

        EasyMock.expect(container.getBundleContext()).andReturn(containerContext).anyTimes();
        containerContext.addServiceListener(recipe, "(objectClass=my.interface)");
        EasyMock.expectLastCall();
        EasyMock.expect(containerContext.getServiceReferences((String) null, "(objectClass=my.interface)"))
                .andReturn(new ServiceReference[] { svcRef1 });
        EasyMock.replay(container, containerContext, svcRef1);

        recipe.start(listener);
        Assert.assertSame(svcRef1, currentReference.get());
        EasyMock.verify(container, containerContext, svcRef1);

        EasyMock.reset(container, containerContext, svcRef1);


        ServiceReference svcRef2 = EasyMock.createMock(ServiceReference.class);
        ServiceEvent event2 = new ServiceEvent(ServiceEvent.REGISTERED, svcRef2);

        EasyMock.expect(svcRef1.getProperty(Constants.SERVICE_ID)).andReturn(0L).anyTimes();
        EasyMock.expect(svcRef1.getProperty(Constants.SERVICE_RANKING)).andReturn(0).anyTimes();
        EasyMock.expect(svcRef2.getProperty(Constants.SERVICE_ID)).andReturn(1L).anyTimes();
        EasyMock.expect(svcRef2.getProperty(Constants.SERVICE_RANKING)).andReturn(1).anyTimes();
        EasyMock.replay(container, containerContext, svcRef1, svcRef2);

        recipe.serviceChanged(event2);
        Assert.assertSame(svcRef2, currentReference.get());
        EasyMock.verify(container, containerContext, svcRef1, svcRef2);

    }
}

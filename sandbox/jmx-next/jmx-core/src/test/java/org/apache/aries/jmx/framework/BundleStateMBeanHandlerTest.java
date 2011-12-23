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
package org.apache.aries.jmx.framework;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.aries.jmx.Logger;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class BundleStateMBeanHandlerTest {


    @Test
    public void testOpenAndClose() throws Exception {

        BundleContext context = mock(BundleContext.class);
        when(context.getProperty(Constants.FRAMEWORK_UUID)).thenReturn("some-uuid");

        Logger logger = mock(Logger.class);

        Bundle mockSystemBundle = mock(Bundle.class);
        when(mockSystemBundle.getSymbolicName()).thenReturn("the.sytem.bundle");
        when(context.getBundle(0)).thenReturn(mockSystemBundle);

        ServiceReference packageAdminRef = mock(ServiceReference.class);
        PackageAdmin packageAdmin = mock(PackageAdmin.class);
        when(context.getServiceReference(PackageAdmin.class.getName())).thenReturn(packageAdminRef);
        when(context.getService(packageAdminRef)).thenReturn(packageAdmin);
        ServiceReference startLevelRef = mock(ServiceReference.class);
        StartLevel startLevel = mock(StartLevel.class);
        when(context.getServiceReference(StartLevel.class.getName())).thenReturn(startLevelRef);
        when(context.getService(startLevelRef)).thenReturn(startLevel);

        BundleStateMBeanHandler handler = new BundleStateMBeanHandler(context, logger);
        handler.open();

        assertNotNull(handler.getMbean());

        handler.close();
        verify(context).ungetService(packageAdminRef);
        verify(context).ungetService(startLevelRef);

    }

}

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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.util;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.util.internal.BundleToClassLoaderAdapter;
import org.junit.Test;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;

public class BundleToClassLoaderAdapterTest {
    @Test(expected=ClassNotFoundException.class)
    public void testInheritance() throws Exception {
        ClassLoader testLoader = new ClassLoader(makeSUT(false)) {
        };
        
        testLoader.loadClass(Bundle.class.getName());
    }
    
    @Test
    public void testInheritancePositive() throws Exception {
        ClassLoader testLoader = new ClassLoader(makeSUT(true)) {
        };

        assertEquals(Bundle.class, testLoader.loadClass(Bundle.class.getName()));
    }
    
    @Test
    public void testStraightLoadClass() throws Exception {
        assertEquals(Bundle.class, makeSUT(true).loadClass(Bundle.class.getName()));
    }
    
    @Test(expected=ClassNotFoundException.class)
    public void testLoadClassFailure() throws Exception {
        makeSUT(false).loadClass(Bundle.class.getName());        
    }
    
    @Test
    public void testLoadWithResolve() throws Exception {
        assertEquals(Bundle.class, makeSUT(true).loadClass(Bundle.class.getName(), true));
    }
    
    private BundleToClassLoaderAdapter makeSUT(boolean includeBundleClass) {
        Bundle bundle = Skeleton.newMock(Bundle.class);
        if (includeBundleClass) {
            Skeleton.getSkeleton(bundle).setReturnValue(new MethodCall(Bundle.class, "loadClass", Bundle.class.getName()), Bundle.class);
        } else {
            Skeleton.getSkeleton(bundle).setThrows(new MethodCall(Bundle.class, "loadClass", Bundle.class.getName()), new ClassNotFoundException());
        }
        
        return new BundleToClassLoaderAdapter(bundle);
    }
}

/**
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
package org.apache.aries.spifly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.BundleContext;

public class ServiceLoaderProviderFileTest {
    @Test
    public void parsesUtf8CommentsAndDuplicatesUsingServiceLoaderRules() {
        URL first = getClass().getResource(
                "/provider-config/first/META-INF/services/org.example.Service");
        URL second = getClass().getResource(
                "/provider-config/second/META-INF/services/org.example.Service");
        assertNotNull(first);
        assertNotNull(second);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(
                new BaseActivator() {
                    @Override
                    public void start(BundleContext context) throws Exception {}
                }, null);
        Map<String, List<String>> providers = customizer.readServiceProviderFiles(
                Arrays.asList(first, second));

        assertEquals(1, providers.size());
        assertEquals(Arrays.asList(
                "org.example.First",
                "org.example.Žluťoučký",
                "org.example.Second"), providers.get("org.example.Service"));
    }
}

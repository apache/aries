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
package org.apache.aries.tx.control.resource.common.impl;

import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationDefinedResourceFactoryTest {

	private final class ConfigurationDefinedResourceFactoryExtension extends ConfigurationDefinedResourceFactory {
		private ConfigurationDefinedResourceFactoryExtension(BundleContext context) {
			super(context);
		}

		@Override
		public String getName() {
			return "Test";
		}

		@Override
		protected LifecycleAware getConfigurationDrivenResource(BundleContext context, String pid,
				Map<String, Object> properties) throws Exception {
			switch(pid) {
				case "a": return resourceA;
				case "b": return resourceB;
				default: throw new IllegalArgumentException(pid);
			}
		}
	}

	@Mock
	BundleContext ctx;
	
	@Mock
	LifecycleAware resourceA;
	
	@Mock
	LifecycleAware resourceB;
	
	@Test
	public void testLifecycleStop() throws Exception {
		ConfigurationDefinedResourceFactory cdrf = new ConfigurationDefinedResourceFactoryExtension(ctx);
		
		cdrf.updated("a", new Hashtable<>());
		Mockito.verify(resourceA).start();
		
		cdrf.stop();
		Mockito.verify(resourceA).stop();
	}
	
	@Test
	public void testLifecycleDelete() throws Exception {
		ConfigurationDefinedResourceFactory cdrf = new ConfigurationDefinedResourceFactoryExtension(ctx);
		
		cdrf.updated("a", new Hashtable<>());
		Mockito.verify(resourceA).start();
		
		cdrf.deleted("a");
		
		Mockito.verify(resourceA).stop();
		
		cdrf.stop();
		Mockito.verify(resourceA).stop();
	}

	@Test
	public void testLifecycleUpdate() throws Exception {
		
		Mockito.when(resourceB.update(anyMapOf(String.class, Object.class))).thenReturn(true);
		
		ConfigurationDefinedResourceFactory cdrf = new ConfigurationDefinedResourceFactoryExtension(ctx);
		
		cdrf.updated("a", new Hashtable<>());
		Mockito.verify(resourceA).start();
		cdrf.updated("b", new Hashtable<>());
		Mockito.verify(resourceB).start();
		
		cdrf.updated("a", new Hashtable<>());
		Mockito.verify(resourceA).stop();
		Mockito.verify(resourceA, times(2)).start();
		
		cdrf.updated("b", new Hashtable<>());
		Mockito.verify(resourceB, never()).stop();

		
		cdrf.stop();
		Mockito.verify(resourceA, times(2)).stop();
		Mockito.verify(resourceB).stop();
	}
}

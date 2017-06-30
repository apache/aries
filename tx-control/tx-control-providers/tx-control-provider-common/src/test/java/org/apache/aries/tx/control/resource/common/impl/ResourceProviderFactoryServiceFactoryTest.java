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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;

@RunWith(MockitoJUnitRunner.class)
public class ResourceProviderFactoryServiceFactoryTest {

	@Mock
	Bundle b1;

	@Mock
	Bundle b2;
	
	List<TrackingResourceProviderFactory<AutoCloseable>> factories = new ArrayList<>();

	@Mock
	TrackingResourceProviderFactory<AutoCloseable> factory2;

	@Test
	public void testLifecycleClose() throws Exception {
		ResourceProviderFactoryServiceFactory<?,?> rpfsf = new ResourceProviderFactoryServiceFactory<
				AutoCloseable, TrackingResourceProviderFactory<AutoCloseable>>() {

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					protected TrackingResourceProviderFactory<AutoCloseable> getTrackingResourceManagerProviderFactory() {
						TrackingResourceProviderFactory mock = Mockito.mock(TrackingResourceProviderFactory.class);
						factories.add(mock);
						return mock;
					}
		};
		
		rpfsf.getService(b1, null);
		rpfsf.getService(b2, null);
		
		
		rpfsf.close();
		
		for(TrackingResourceProviderFactory<AutoCloseable> t : factories) {
			Mockito.verify(t).closeAll();
		}
	}

	@Test
	public void testLifecycleCloseWithUnget() throws Exception {
		ResourceProviderFactoryServiceFactory<AutoCloseable, 
				TrackingResourceProviderFactory<AutoCloseable>> rpfsf = 
				new ResourceProviderFactoryServiceFactory<
				AutoCloseable, TrackingResourceProviderFactory<AutoCloseable>>() {
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			protected TrackingResourceProviderFactory<AutoCloseable> getTrackingResourceManagerProviderFactory() {
				TrackingResourceProviderFactory mock = Mockito.mock(TrackingResourceProviderFactory.class);
				factories.add(mock);
				return mock;
			}
		};
		
		rpfsf.getService(b1, null);
		TrackingResourceProviderFactory<AutoCloseable> tf = rpfsf.getService(b2, null);
		
		rpfsf.ungetService(b2, null, tf);
		
		Mockito.verify(tf).closeAll();
		
		factories.remove(tf);
		
		rpfsf.close();
		
		for(TrackingResourceProviderFactory<AutoCloseable> t : factories) {
			Mockito.verify(t).closeAll();
		}
	}
}

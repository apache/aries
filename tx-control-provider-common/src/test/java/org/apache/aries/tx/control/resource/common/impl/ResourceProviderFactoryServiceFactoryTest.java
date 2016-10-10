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

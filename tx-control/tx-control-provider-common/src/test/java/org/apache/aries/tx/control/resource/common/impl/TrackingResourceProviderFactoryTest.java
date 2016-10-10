package org.apache.aries.tx.control.resource.common.impl;

import org.junit.Test;
import org.mockito.Mockito;

public class TrackingResourceProviderFactoryTest {

	@Test
	public void testLifecycleClose() throws Exception {
		TrackingResourceProviderFactory<AutoCloseable> trpf = new TrackingResourceProviderFactory<AutoCloseable>(){};
		
		AutoCloseable result = trpf.doGetResult(() -> Mockito.mock(AutoCloseable.class));
		
		
		trpf.closeAll();
		
		Mockito.verify(result).close();
	}
}

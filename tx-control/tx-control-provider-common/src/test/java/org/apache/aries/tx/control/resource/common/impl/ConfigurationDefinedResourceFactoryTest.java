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

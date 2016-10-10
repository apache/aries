package org.apache.aries.tx.control.resource.common.impl;

import static org.hamcrest.CoreMatchers.both;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

import java.util.Dictionary;
import java.util.Hashtable;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

@RunWith(MockitoJUnitRunner.class)
public class ResourceActivatorTest {

	@Mock
	BundleContext ctx;
	
	@Mock
	ResourceProviderFactoryServiceFactory<AutoCloseable, TrackingResourceProviderFactory<AutoCloseable>> serviceFactory;

	@SuppressWarnings("rawtypes")
	@Mock
	ServiceRegistration providerReg;
	
	@Mock
	ConfigurationDefinedResourceFactory configDrivenResourceFactory;

	@Mock
	ServiceRegistration<ManagedServiceFactory> msfReg;
	
	@Test
	public void testLifecycleNoServiceOrMSF() throws Exception {
		ResourceActivator<?,?> ra = new ResourceActivator<AutoCloseable,
				TrackingResourceProviderFactory<AutoCloseable>>() {
		};
		
		ra.start(ctx);
		ra.stop(ctx);
		
		Mockito.verifyNoMoreInteractions(ctx);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLifecycleNoMSF() throws Exception {
		
		Mockito.when(ctx.registerService(eq(TrackingResourceProviderFactory.class.getName()), 
				any(), argThat(both(hasKeyValue("foo", "bar")).and(hasKeyValue("fizz", 42)))))
					.thenReturn(providerReg);
		
		ResourceActivator<?,?> ra = new ResourceActivator<AutoCloseable,
				TrackingResourceProviderFactory<AutoCloseable>>() {
					@Override
					protected ResourceProviderFactoryServiceFactory<AutoCloseable, TrackingResourceProviderFactory<AutoCloseable>> getServiceFactory(
							BundleContext context) {
						return serviceFactory;
					}

					@Override
					protected Class<? super TrackingResourceProviderFactory<AutoCloseable>> getAdvertisedInterface() {
						return TrackingResourceProviderFactory.class;
					}

					@Override
					protected Dictionary<String, Object> getServiceProperties() {
						Hashtable<String, Object> table = new Hashtable<>();
						table.put("foo", "bar");
						table.put("fizz", 42);
						return table;
					}
			};
		
		ra.start(ctx);
		
		ra.stop(ctx);
		
		Mockito.verify(providerReg).unregister();
		Mockito.verify(serviceFactory).close();
	}

	@Test
	public void testLifecycleNoService() throws Exception {
		
		Mockito.when(ctx.registerService(eq(ManagedServiceFactory.class), 
				any(), argThat(hasKeyValue(Constants.SERVICE_PID, "foo.bar.baz"))))
		.thenReturn(msfReg);
		
		ResourceActivator<?,?> ra = new ResourceActivator<AutoCloseable,
				TrackingResourceProviderFactory<AutoCloseable>>() {

					@Override
					protected ConfigurationDefinedResourceFactory getConfigurationDefinedResourceFactory(
							BundleContext context) {
						return configDrivenResourceFactory;
					}

					@Override
					protected String getMSFPid() {
						return "foo.bar.baz";
					}
		};
		
		ra.start(ctx);
		
		ra.stop(ctx);
		
		Mockito.verify(msfReg).unregister();
		Mockito.verify(configDrivenResourceFactory).stop();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLifecycleBothServiceAndMSF() throws Exception {
		Mockito.when(ctx.registerService(eq(TrackingResourceProviderFactory.class.getName()), 
				any(), argThat(both(hasKeyValue("foo", "bar")).and(hasKeyValue("fizz", 42)))))
					.thenReturn(providerReg);
		
		Mockito.when(ctx.registerService(eq(ManagedServiceFactory.class), 
				any(), argThat(hasKeyValue(Constants.SERVICE_PID, "foo.bar.baz"))))
		.thenReturn(msfReg);
		
		ResourceActivator<?,?> ra = new ResourceActivator<AutoCloseable,
				TrackingResourceProviderFactory<AutoCloseable>>() {
					@Override
					protected ResourceProviderFactoryServiceFactory<AutoCloseable, TrackingResourceProviderFactory<AutoCloseable>> getServiceFactory(
							BundleContext context) {
						return serviceFactory;
					}

					@Override
					protected Class<? super TrackingResourceProviderFactory<AutoCloseable>> getAdvertisedInterface() {
						return TrackingResourceProviderFactory.class;
					}

					@Override
					protected Dictionary<String, Object> getServiceProperties() {
						Hashtable<String, Object> table = new Hashtable<>();
						table.put("foo", "bar");
						table.put("fizz", 42);
						return table;
					}
					
					@Override
					protected ConfigurationDefinedResourceFactory getConfigurationDefinedResourceFactory(
							BundleContext context) {
						return configDrivenResourceFactory;
					}

					@Override
					protected String getMSFPid() {
						return "foo.bar.baz";
					}
			};
		
		ra.start(ctx);
		
		ra.stop(ctx);
		
		Mockito.verify(providerReg).unregister();
		Mockito.verify(serviceFactory).close();
		
		Mockito.verify(msfReg).unregister();
		Mockito.verify(configDrivenResourceFactory).stop();
	}


	static Matcher<Dictionary<String, Object>> hasKeyValue(String key, Object value) {
		return new DictionaryMatcher(key, value);
	}
	
	
	private static class DictionaryMatcher extends TypeSafeDiagnosingMatcher<Dictionary<String, Object>> {

		private final String key;
		private final Object value;
		
		public DictionaryMatcher(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		protected boolean matchesSafely(Dictionary<String, Object> map, Description mismatchDescription) {
			return value.equals(map.get(key));
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("Map entry: " + key + "=" + value);
		}
		
	}
}

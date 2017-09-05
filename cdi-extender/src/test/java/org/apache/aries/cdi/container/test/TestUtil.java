/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.container.internal.configuration.ConfigurationCallback;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.AbstractModelBuilder;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.Context;
import org.apache.aries.cdi.container.internal.model.Registrator;
import org.apache.aries.cdi.container.internal.model.Tracker;
import org.apache.aries.cdi.container.internal.reference.ReferenceCallback;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.CdiConstants;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class TestUtil {

	public static ConfigurationCallback getCallback(ConfigurationPolicy policy) {
		return new ConfigurationCallback.Builder().policy(policy).build();
	}

	public static AbstractModelBuilder getModelBuilder(final String osgiBeansFile) {
		return getModelBuilder(
			Arrays.asList(
				"OSGI-INF/cdi/beans-configuration.xml",
				"OSGI-INF/cdi/beans-only.xml",
				"OSGI-INF/cdi/beans-references.xml",
				"OSGI-INF/cdi/beans-services.xml"
			),  osgiBeansFile);
	}

	public static AbstractModelBuilder getModelBuilder(
		final List<String> defaultResources, final String osgiBeansFile) {

		return new AbstractModelBuilder() {

			@Override
			public List<String> getDefaultResources() {
				return defaultResources;
			}

			@Override
			public URL getResource(String resource) {
				return getClassLoader().getResource(resource);
			}

			@Override
			public ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}

			@Override
			public Map<String, Object> getAttributes() {
				if (osgiBeansFile == null) {
					return Collections.emptyMap();
				}

				return Collections.singletonMap(
					CdiConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE, Arrays.asList(osgiBeansFile));
			}
		};
	}

	public static <T> Collection<T> sort(Collection<T> set) {
		return sort(set, (c1, c2) -> c1.getClass().getName().compareTo(c2.getClass().getName()));
	}

	public static <T> Collection<T> sort(Collection<T> set, Comparator<T> comparator) {
		List<T> list = new ArrayList<>(set);

		Collections.sort(list, comparator);

		return list;
	}

	public static ContainerState getContainerState(BeansModel beansModel) {
		final TContext context = new TContext();
		final TBMRegistrator bmRegistrator = new TBMRegistrator();
		final TMSRegistrator msRegistrator = new TMSRegistrator();
		final TRegistrator serviceRegistrator = new TRegistrator();
		final TTracker tracker = new TTracker();

		return new ContainerState(null, null) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

			@Override
			public Context context() {
				return context;
			}

			@Override
			public Registrator<BeanManager> beanManagerRegistrator() {
				return bmRegistrator;
			}

			@Override
			public Registrator<ManagedService> managedServiceRegistrator() {
				return msRegistrator;
			}

			@Override
			public Registrator<Object> serviceRegistrator() {
				return serviceRegistrator;
			}

			@Override
			public Tracker tracker() {
				return tracker;
			}

		};
	}

	public static class TContext extends Context {

		@Override
		public <T> T getService(ServiceReference<T> reference) {
			if (reference instanceof MockServiceReference) {
				return ((MockServiceReference<T>)reference).getService();
			}
			return null;
		}

		@Override
		public <T> ServiceObjects<T> getServiceObjects(ServiceReference<T> reference) {
			return null;
		}

		@Override
		public <T> boolean ungetService(ServiceReference<T> reference) {
			return false;
		}

	}

	public static class TBMRegistrator extends Registrator<BeanManager> {

		@Override
		public void close() {
			registrations.clear();
		}

		@Override
		public void registerService(String[] classNames, BeanManager service, Dictionary<String, ?> properties) {
			registrations.put(properties, service);
		}

		@Override
		public int size() {
			return registrations.size();
		}

		public final Map<Dictionary<String, ?>, BeanManager> registrations = new ConcurrentHashMap<>();

	}

	public static class TMSRegistrator extends Registrator<ManagedService> {

		@Override
		public void close() {
			registrations.clear();
		}

		@Override
		public void registerService(String[] classNames, ManagedService service, Dictionary<String, ?> properties) {
			registrations.put(properties, service);
		}

		@Override
		public int size() {
			return registrations.size();
		}

		public final Map<Dictionary<String, ?>, ManagedService> registrations = new ConcurrentHashMap<>();

	}

	public static class TRegistrator extends Registrator<Object> {

		@Override
		public void close() {
			registrations.clear();
		}

		@Override
		public void registerService(String[] classNames, Object service, Dictionary<String, ?> properties) {
			registrations.put(properties, service);
		}

		@Override
		public int size() {
			return registrations.size();
		}

		public final Map<Dictionary<String, ?>, Object> registrations = new ConcurrentHashMap<>();

	}

	public static class TTracker extends Tracker {

		@Override
		public <T> void track(String targetFilter, ReferenceCallback callback) {
			trackers.put(targetFilter, callback);
		}

		public final Map<String, ServiceTrackerCustomizer<Object, ?>> trackers = new ConcurrentHashMap<>();

	}

}
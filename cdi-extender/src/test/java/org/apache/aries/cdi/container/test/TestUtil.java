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

import static org.apache.aries.cdi.container.internal.util.Reflection.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.aries.cdi.container.internal.ChangeCount;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.util.Filters;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Sfl4jLogger;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;

public class TestUtil {

	public static <T extends Comparable<T>> List<T> sort(Collection<T> set) {
		return sort(set, (c1, c2) -> c1.getClass().getName().compareTo(c2.getClass().getName()));
	}

	public static <T> List<T> sort(Collection<T> set, Comparator<T> comparator) {
		List<T> list = new ArrayList<>(set);

		Collections.sort(list, comparator);

		return list;
	}

	public static ContainerState getContainerState(final BeansModel beansModel) throws Exception {
		BundleDTO ccrBundleDTO = new BundleDTO();
		ccrBundleDTO.id = 1;
		ccrBundleDTO.lastModified = 2300l;
		ccrBundleDTO.state = Bundle.ACTIVE;
		ccrBundleDTO.symbolicName = "ccr";
		ccrBundleDTO.version = "1.0.0";

		Bundle ccrBundle = mockBundle(ccrBundleDTO, b -> {
			when(b.adapt(BundleWiring.class).getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)).thenReturn(new ArrayList<>());
		});

		BundleDTO bundleDTO = new BundleDTO();
		bundleDTO.id = 2;
		bundleDTO.lastModified = 24000l;
		bundleDTO.state = Bundle.ACTIVE;
		bundleDTO.symbolicName = "foo";
		bundleDTO.version = "1.0.0";

		Bundle bundle = mockBundle(bundleDTO, b -> {
			BundleWire extenderWire = mock(BundleWire.class);
			BundleCapability extenderCapability = mock(BundleCapability.class);
			BundleRequirement extenderRequirement = mock(BundleRequirement.class);

			when(b.adapt(BundleWiring.class).getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE)).thenReturn(Collections.singletonList(extenderWire));
			when(b.adapt(BundleWiring.class).listResources("OSGI-INF/cdi", "*.xml", BundleWiring.LISTRESOURCES_LOCAL)).thenReturn(Collections.singletonList("OSGI-INF/cdi/osgi-beans.xml"));

			when(extenderWire.getCapability()).thenReturn(extenderCapability);
			when(extenderCapability.getAttributes()).thenReturn(Collections.singletonMap(ExtenderNamespace.EXTENDER_NAMESPACE, CDIConstants.CDI_CAPABILITY_NAME));
			when(extenderWire.getRequirement()).thenReturn(extenderRequirement);
			when(extenderRequirement.getAttributes()).thenReturn(new HashMap<>());
		});

		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = new ServiceTracker<>(bundle.getBundleContext(), ConfigurationAdmin.class, null);
		caTracker.open();
		ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
		bundle.getBundleContext().registerService(ConfigurationAdmin.class, ca, null);

		return new ContainerState(bundle, ccrBundle, new ChangeCount(), new PromiseFactory(Executors.newFixedThreadPool(1)), caTracker, new Logs.Builder(bundle.getBundleContext()).build()) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			@Override
			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

		};
	}

	@SuppressWarnings("unchecked")
	public static Bundle mockBundle(BundleDTO bundleDTO, Consumer<Bundle> extra) throws Exception {
		Bundle bundle = mock(Bundle.class);
		BundleContext bundleContext = mock(BundleContext.class);
		BundleWiring bundleWiring = mock(BundleWiring.class);

		when(bundle.getBundleContext()).thenReturn(bundleContext);
		when(bundle.toString()).thenReturn(bundleDTO.symbolicName + "[" + bundleDTO.id + "]");
		when(bundle.getBundleId()).thenReturn(bundleDTO.id);
		when(bundle.getLastModified()).thenReturn(bundleDTO.lastModified);
		when(bundle.getSymbolicName()).thenReturn(bundleDTO.symbolicName);
		when(bundle.adapt(BundleWiring.class)).thenReturn(bundleWiring);
		when(bundle.adapt(BundleDTO.class)).thenReturn(bundleDTO);
		when(bundle.adapt(ServiceReferenceDTO[].class)).then(
			(Answer<ServiceReferenceDTO[]>) serviceDTOs -> {
				return serviceRegistrations.stream().filter(
					reg -> reg.getReference().getBundle().equals(bundle)
				).map(
					reg -> reg.getReference().toDTO()
				).collect(Collectors.toList()).toArray(new ServiceReferenceDTO[0]);
			}
		);
		when(bundle.getResource(any())).then(
			(Answer<URL>) getResource -> {
				return bundleDTO.getClass().getClassLoader().getResource((String)getResource.getArgument(0));
			}
		);
		when(bundle.loadClass(any())).then(
			(Answer<Class<?>>) loadClass -> {
				return bundleDTO.getClass().getClassLoader().loadClass((String)loadClass.getArgument(0));
			}
		);
		when(bundleWiring.getBundle()).thenReturn(bundle);
		when(bundleContext.getBundle()).thenReturn(bundle);
		when(bundleContext.getService(any())).then(
			(Answer<Object>) getService -> {
				return serviceRegistrations.stream().filter(
					reg -> reg.getReference().equals(getService.getArgument(0))
				).findFirst().get().getReference().getService();
			}
		);
		doAnswer(
			(Answer<ServiceRegistration<?>>) registerService -> {
				Class<?> clazz = registerService.getArgument(0);
				MockServiceReference<?> mockServiceReference = new MockServiceReference<>(
					bundle, registerService.getArgument(1), new String[] {clazz.getName()});

				Optional.ofNullable(
					registerService.getArgument(2)
				).map(
					arg -> (Dictionary<String, Object>)arg
				).ifPresent(
					dict -> {
						for (Enumeration<String> enu = dict.keys(); enu.hasMoreElements();) {
							String key = enu.nextElement();
							if (key.equals(Constants.OBJECTCLASS) ||
								key.equals(Constants.SERVICE_BUNDLEID) ||
								key.equals(Constants.SERVICE_ID) ||
								key.equals(Constants.SERVICE_SCOPE)) {
								continue;
							}
							mockServiceReference.setProperty(key, dict.get(key));
						}
					}
				);

				return new MockServiceRegistration<>(mockServiceReference, serviceRegistrations, serviceListeners);
			}
		).when(bundleContext).registerService(any(Class.class), any(Object.class), any());
		doAnswer(
			(Answer<ServiceRegistration<?>>) registerService -> {
				String[] clazzes = registerService.getArgument(0);
				MockServiceReference<?> mockServiceReference = new MockServiceReference<>(
					bundle, registerService.getArgument(1), clazzes);

				Optional.ofNullable(
					registerService.getArgument(2)
				).map(
					arg -> (Dictionary<String, Object>)arg
				).ifPresent(
					dict -> {
						for (Enumeration<String> enu = dict.keys(); enu.hasMoreElements();) {
							String key = enu.nextElement();
							if (key.equals(Constants.OBJECTCLASS) ||
								key.equals(Constants.SERVICE_BUNDLEID) ||
								key.equals(Constants.SERVICE_ID) ||
								key.equals(Constants.SERVICE_SCOPE)) {
								continue;
							}
							mockServiceReference.setProperty(key, dict.get(key));
						}
					}
				);

				return new MockServiceRegistration<>(mockServiceReference, serviceRegistrations, serviceListeners);
			}
		).when(bundleContext).registerService(any(String[].class), any(Object.class), any());
		doAnswer(
			(Answer<Void>) addServiceListener -> {
				ServiceListener sl = cast(addServiceListener.getArgument(0));
				Filter filter = FrameworkUtil.createFilter(addServiceListener.getArgument(1));
				if (serviceListeners.add(new SimpleEntry<>(sl, filter))) {
					serviceRegistrations.stream().filter(
						reg -> filter.match(reg.getReference())
					).forEach(
						reg -> sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reg.getReference()))
					);
				}
				return null;
			}
		).when(bundleContext).addServiceListener(any(), any());

		ServiceTracker<LoggerFactory, LoggerFactory> loggerTracker = new ServiceTracker<>(bundle.getBundleContext(), LoggerFactory.class, null);
		loggerTracker.open();
		LoggerFactory lf = mock(LoggerFactory.class);
		when(lf.getLogger(any(Class.class))).then(
			(Answer<Logger>) getLogger -> {
				Class<?> clazz = getLogger.getArgument(0);
				return new Sfl4jLogger(clazz.getName());
			}
		);
		when(lf.getLogger(anyString())).then(
			(Answer<Logger>) getLogger -> {
				String name = getLogger.getArgument(0);
				return new Sfl4jLogger(name);
			}
		);
		when(lf.getLogger(any(Class.class), any())).then(
			(Answer<Logger>) getLogger -> {
				Class<?> clazz = getLogger.getArgument(0);
				return new Sfl4jLogger(clazz.getName());
			}
		);
		when(lf.getLogger(anyString(), any())).then(
			(Answer<Logger>) getLogger -> {
				String name = getLogger.getArgument(0);
				return new Sfl4jLogger(name);
			}
		);
		when(lf.getLogger(any(), anyString(), any())).then(
			(Answer<Logger>) getLogger -> {
				String name = getLogger.getArgument(1);
				return new Sfl4jLogger(name);
			}
		);
		bundle.getBundleContext().registerService(LoggerFactory.class, lf, null);

		extra.accept(bundle);

		return bundle;
	}

	public static ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> mockCaSt(Bundle bundle) throws Exception {
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = new ServiceTracker<>(bundle.getBundleContext(), ConfigurationAdmin.class, null);
		caTracker.open();
		ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
		bundle.getBundleContext().registerService(ConfigurationAdmin.class, ca, null);

		when(ca.listConfigurations(anyString())).then(
			(Answer<Configuration[]>) listConfigurations -> {
				String query = listConfigurations.getArgument(0);
				Filter filter = Filters.asFilter(query);
				List<MockConfiguration> list = configurations.stream().filter(
					c -> filter.match(c.getProperties())
				).collect(Collectors.toList());

				if (list.isEmpty()) {
					return null;
				}
				return list.toArray(new Configuration[0]);
			}
		);

		return caTracker;
	}

	public static final List<MockConfiguration> configurations = new CopyOnWriteArrayList<>();
	public static final List<Map.Entry<ServiceListener, Filter>> serviceListeners = new CopyOnWriteArrayList<>();
	public static final List<MockServiceRegistration<?>> serviceRegistrations = new CopyOnWriteArrayList<>();

}
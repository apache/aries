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

package org.apache.aries.cdi.container.internal.phase;

import static org.apache.aries.cdi.container.test.TestUtil.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.aries.cdi.container.internal.container.ContainerDiscovery;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.AbstractModelBuilder;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cm.ManagedService;

public class ConfigurationPhaseTest {

	@Test
	public void testOnlyBeans() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-only.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = getContainerState(beansModel);

		ContainerDiscovery.discover(containerState);

		Phase_Configuration phase = new Phase_Configuration(containerState, Collections.emptyList());

		phase.open();

		Assert.assertEquals(2, containerState.managedServiceRegistrator().size());
		Assert.assertEquals(1, containerState.beanManagerRegistrator().size());
		Assert.assertEquals(0, containerState.serviceRegistrator().size());
		Assert.assertEquals(CdiEvent.Type.CREATED, containerState.lastState());
	}

	@Test
	public void testConfiguration() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-configuration.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = getContainerState(beansModel);

		ContainerDiscovery.discover(containerState);

		Phase_Configuration phase = new Phase_Configuration(containerState, Collections.emptyList());

		phase.open();

		Assert.assertEquals(4, containerState.managedServiceRegistrator().size());
		Assert.assertEquals(0, containerState.beanManagerRegistrator().size());
		Assert.assertEquals(0, containerState.serviceRegistrator().size());
		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, containerState.lastState());
	}

	@Test
	public void testReferences() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-references.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = getContainerState(beansModel);

		ContainerDiscovery.discover(containerState);

		Phase_Configuration phase = new Phase_Configuration(containerState, Collections.emptyList());

		phase.open();

		Assert.assertEquals(2, containerState.managedServiceRegistrator().size());
		Assert.assertEquals(0, containerState.beanManagerRegistrator().size());
		Assert.assertEquals(0, containerState.serviceRegistrator().size());
		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_SERVICES, containerState.lastState());
	}

	@Test
	public void testEverything() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = getContainerState(beansModel);

		ContainerDiscovery.discover(containerState);

		Phase_Configuration phase = new Phase_Configuration(containerState, Collections.emptyList());

		phase.open();

		Assert.assertEquals(10, containerState.managedServiceRegistrator().size());
		Assert.assertEquals(0, containerState.beanManagerRegistrator().size());
		Assert.assertEquals(0, containerState.serviceRegistrator().size());
		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, containerState.lastState());
	}

	@Test
	public void testEverythingCreateConfigurations() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = getContainerState(beansModel);

		ContainerDiscovery.discover(containerState);

		Phase_Configuration phase = new Phase_Configuration(containerState, Collections.emptyList());

		phase.open();

		Assert.assertEquals(10, containerState.managedServiceRegistrator().size());
		Assert.assertEquals(0, containerState.beanManagerRegistrator().size());
		Assert.assertEquals(0, containerState.serviceRegistrator().size());
		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, containerState.lastState());

		Set<Entry<Dictionary<String,?>,ManagedService>> entrySet =
			((TMSRegistrator)containerState.managedServiceRegistrator()).registrations.entrySet();

		for (Map.Entry<Dictionary<String, ?>, ManagedService> entry : entrySet) {
			Dictionary<String, Object> properties = new Hashtable<>();

			properties.put(Constants.SERVICE_PID, entry.getKey().get(Constants.SERVICE_PID));
			properties.put("time", System.currentTimeMillis());

			entry.getValue().updated(properties);
		}

		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_SERVICES, containerState.lastState());
	}

	@Test
	public void testReactiveConfigurations() throws Exception {
		AbstractModelBuilder builder = getModelBuilder(Collections.singletonList("OSGI-INF/cdi/beans-configuration.xml"), null);

		final BeansModel beansModel = builder.build();

		ContainerState containerState = getContainerState(beansModel);

		ContainerDiscovery.discover(containerState);

		Phase_Configuration phase = new Phase_Configuration(containerState, Collections.emptyList());

		phase.open();

		Assert.assertEquals(4, containerState.managedServiceRegistrator().size());
		Assert.assertEquals(0, containerState.beanManagerRegistrator().size());
		Assert.assertEquals(0, containerState.serviceRegistrator().size());
		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, containerState.lastState());

		Collection<Entry<Dictionary<String, ?>, ManagedService>> collection = sort(
			((TMSRegistrator)containerState.managedServiceRegistrator()).registrations.entrySet(), (a, b) -> ((Integer)a.getKey().get("component.id")).compareTo((Integer)b.getKey().get("component.id")));

		Iterator<Entry<Dictionary<String, ?>, ManagedService>> iterator = collection.iterator();

		Entry<Dictionary<String, ?>, ManagedService> entry = iterator.next();
		Dictionary<String, Object> properties = new Hashtable<>();
		String pid = (String)entry.getKey().get(Constants.SERVICE_PID);
		Assert.assertEquals("org.apache.aries.cdi.container.test.beans.BarWithConfig", pid);
		properties.put(Constants.SERVICE_PID, pid);
		properties.put("time", System.currentTimeMillis());
		entry.getValue().updated(properties);
		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, containerState.lastState());

		entry = iterator.next();
		properties = new Hashtable<>();
		pid = (String)entry.getKey().get(Constants.SERVICE_PID);
		Assert.assertEquals("org.apache.aries.cdi.container.test.beans.BarWithConfig", pid);
		properties.put(Constants.SERVICE_PID, pid);
		properties.put("time", System.currentTimeMillis());
		entry.getValue().updated(properties);
		Assert.assertEquals(CdiEvent.Type.CREATED, containerState.lastState());
		entry.getValue().updated(null);
		Assert.assertEquals(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, containerState.lastState());
		entry.getValue().updated(properties);
		Assert.assertEquals(CdiEvent.Type.CREATED, containerState.lastState());
	}

}

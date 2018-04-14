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

package org.apache.aries.cdi.container.internal.configuration;

public class ConfigurationManagedServiceTest_Ignore {

/*	@Test
	public void test_emptyAdd() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(new Hashtable<>());
	}

	@Test
	public void test_nullAdd() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(null);
	}

	@Test
	public void test_add() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertTrue(configurationCallback.resolved());
	}

	@Test
	public void test_addAfterRemove() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(null);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());
	}

	@Test
	public void test_addAfterUpdate() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());
	}

	@Test
	public void test_removeAfterAdd() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(null);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());
	}

	@Test
	public void test_removeAfterRemove() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(null);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(null);
	}

	@Test
	public void test_removeAfterUpdate() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(null);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());
	}

	@Test
	public void test_updateAfterAdd() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());
	}

	@Test
	public void test_updateAfterRemove() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(null);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);
	}

	@Test
	public void test_updateAfterUpdate() throws Exception {
		ConfigurationCallback configurationCallback = getCallback(POLICY);

		ManagedService managedService = new ConfigurationManagedService("foo", configurationCallback);

		Assert.assertTrue(configurationCallback.resolved());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("foo", "bar");

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());

		managedService.updated(properties);

		Assert.assertEquals(ConfigurationCallback.State.STARTED, configurationCallback.state());
		Assert.assertTrue(configurationCallback.resolved());
	}

	private ConfigurationPolicy POLICY = ConfigurationPolicy.IGNORE;
*/
}

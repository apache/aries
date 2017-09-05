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

import static org.osgi.framework.Constants.SERVICE_PID;
import static org.apache.aries.cdi.container.test.TestUtil.*;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;

public class ConfigurationCallbackTest_Require {

	@Test(expected = IllegalArgumentException.class)
	public void test_emptyAdd() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();

		callback.added(properties);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_emptyUpdate() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();

		callback.updated(properties);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_nullAdd() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		callback.added(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_nullUpdate() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		callback.updated(null);
	}

	@Test(expected = IllegalStateException.class)
	public void test_addAfterAdd() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());

		callback.added(properties);
	}

	@Test
	public void test_addAfterRemove() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());

		callback.removed();

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
	}

	@Test(expected = IllegalStateException.class)
	public void test_addAfterUpdate() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());

		properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.updated(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());

		callback.added(properties);
	}

	@Test
	public void test_removeAfterAdd() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());

		callback.removed();

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());
	}

	@Test(expected = IllegalStateException.class)
	public void test_removeAfterRemove() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());

		callback.removed();

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		callback.removed();
	}

	@Test
	public void test_removeAfterUpdate() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
		Assert.assertEquals("foo", callback.properties().get(SERVICE_PID));

		properties = new Hashtable<>();
		properties.put(SERVICE_PID, "fum");

		callback.updated(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
		Assert.assertEquals("fum", callback.properties().get(SERVICE_PID));

		callback.removed();

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());
	}

	@Test(expected = IllegalStateException.class)
	public void test_removeBeforeAdd() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		callback.removed();
	}

	@Test
	public void test_updateAfterAdd() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
		Assert.assertEquals("foo", callback.properties().get(SERVICE_PID));

		properties = new Hashtable<>();
		properties.put(SERVICE_PID, "fum");

		callback.updated(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
		Assert.assertEquals("fum", callback.properties().get(SERVICE_PID));
	}

	@Test(expected = IllegalStateException.class)
	public void test_updateAfterRemove() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());

		callback.removed();

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		callback.updated(properties);
	}

	@Test
	public void test_updateAfterUpdate() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.added(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
		Assert.assertEquals("foo", callback.properties().get(SERVICE_PID));

		properties = new Hashtable<>();
		properties.put(SERVICE_PID, "fum");

		callback.updated(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
		Assert.assertEquals("fum", callback.properties().get(SERVICE_PID));

		properties = new Hashtable<>();
		properties.put(SERVICE_PID, "fee");

		callback.updated(properties);

		Assert.assertTrue(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertFalse(callback.properties().isEmpty());
		Assert.assertEquals("fee", callback.properties().get(SERVICE_PID));
	}

	@Test(expected = IllegalStateException.class)
	public void test_updateBeforeAdd() throws Exception {
		ConfigurationCallback callback = getCallback(POLICY);

		Assert.assertFalse(callback.resolved());
		Assert.assertNotNull(callback.properties());
		Assert.assertTrue(callback.properties().isEmpty());

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_PID, "foo");

		callback.updated(properties);
	}

	private ConfigurationPolicy POLICY = ConfigurationPolicy.REQUIRE;

}

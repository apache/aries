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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;

public class MockConfiguration implements Configuration {

	public MockConfiguration(String pid, String factoryPid) {
		_pid = pid;
		_factoryPid = factoryPid;
		_properties = new Hashtable<>();
	}

	@Override
	public String getPid() {
		return _pid;
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		return _properties;
	}

	@Override
	public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> reference) {
		return _properties;
	}

	@Override
	public void update(Dictionary<String, ?> properties) throws IOException {
		Dictionary<String, Object> dict = new Hashtable<>();
		for (Enumeration<String> enu = properties.keys();enu.hasMoreElements();) {
			String key = enu.nextElement();
			dict.put(key, properties.get(key));
		}
		dict.put(Constants.SERVICE_PID, _pid);
		if (_factoryPid != null) {
			dict.put("factory.pid", _factoryPid);
		}
		_properties = dict;
		_changeCount.incrementAndGet();
	}

	@Override
	public void delete() throws IOException {
		_changeCount.get();
	}

	@Override
	public String getFactoryPid() {
		return _factoryPid;
	}

	@Override
	public void update() throws IOException {
		_changeCount.incrementAndGet();
	}

	@Override
	public boolean updateIfDifferent(Dictionary<String, ?> properties) throws IOException {
		_changeCount.incrementAndGet();
		return false;
	}

	@Override
	public void setBundleLocation(String location) {
		_location = location;
		_changeCount.incrementAndGet();
	}

	@Override
	public String getBundleLocation() {
		return _location;
	}

	@Override
	public long getChangeCount() {
		return _changeCount.get();
	}

	@Override
	public void addAttributes(ConfigurationAttribute... attrs) throws IOException {
		for (ConfigurationAttribute attr : attrs) {
			attributes.add(attr);
		}
		_changeCount.incrementAndGet();
	}

	@Override
	public Set<ConfigurationAttribute> getAttributes() {
		return attributes;
	}

	@Override
	public void removeAttributes(ConfigurationAttribute... attrs) throws IOException {
		for (ConfigurationAttribute attr : attrs) {
			attributes.remove(attr);
		}
		_changeCount.incrementAndGet();
	}

	private final Set<ConfigurationAttribute> attributes = new HashSet<>();
	private final AtomicLong _changeCount = new AtomicLong();
	private final String _factoryPid;
	private volatile String _location;
	private final String _pid;
	private volatile Dictionary<String, Object> _properties;

}

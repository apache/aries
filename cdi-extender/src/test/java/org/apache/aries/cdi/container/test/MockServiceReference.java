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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.aries.cdi.container.internal.util.Maps;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class MockServiceReference<S> implements ServiceReference<S> {

	public MockServiceReference(Bundle bundle, S service, String[] classes) {
		_bundle = bundle;
		_service = service;
		_properties.put(Constants.OBJECTCLASS, classes);
		_properties.put(Constants.SERVICE_BUNDLEID, bundle.getBundleId());
		_properties.put(Constants.SERVICE_ID, _serviceIds.incrementAndGet());
		_properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
	}

	@Override
	public int compareTo(Object other) {
		if (!(other instanceof ServiceReference)) {
			return -1;
		}

		ServiceReference<?> otherReference = (ServiceReference<?>)other;

		Long id = (Long)getProperty(Constants.SERVICE_ID);
		Long otherId = (Long)otherReference.getProperty(Constants.SERVICE_ID);

		if (id.equals(otherId)) {

			// same service

			return 0;
		}

		Object rankingObj = getProperty(Constants.SERVICE_RANKING);
		Object otherRankingObj = otherReference.getProperty(Constants.SERVICE_RANKING);

		// If no rank, then spec says it defaults to zero.

		if (rankingObj == null) {
			rankingObj = _ZERO;
		}

		if (otherRankingObj == null) {
			otherRankingObj = _ZERO;
		}

		// If rank is not Integer, then spec says it defaults to zero.

		Integer ranking = _ZERO;

		if (rankingObj instanceof Integer) {
			ranking = (Integer)rankingObj;
		}

		Integer otherRanking = _ZERO;

		if (otherRankingObj instanceof Integer) {
			otherRanking = (Integer)otherRankingObj;
		}

		// Sort by rank in ascending order.

		if (ranking.compareTo(otherRanking) < 0) {

			// lower rank

			return -1;
		}
		else if (ranking.compareTo(otherRanking) > 0) {

			// higher rank

			return 1;
		}

		// If ranks are equal, then sort by service id in descending order.

		if (id.compareTo(otherId) < 0) {
			return 1;
		}

		return -1;
	}

	@Override
	public Bundle getBundle() {
		return _bundle;
	}
	@Override
	public Dictionary<String, Object> getProperties() {
		return _properties;
	}

	@Override
	public Object getProperty(String key) {
		return _properties.get(key);
	}

	@Override
	public String[] getPropertyKeys() {
		return Collections.list(_properties.keys()).toArray(new String[0]);
	}

	public S getService() {
		return _service;
	}

	@Override
	public Bundle[] getUsingBundles() {
		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return true;
	}

	public void setProperty(String key, Object value) {
		if (Constants.SERVICE_ID.equals(key)) {
			return;
		}
		_properties.put(key, value);
	}

	public ServiceReferenceDTO toDTO() {
		ServiceReferenceDTO dto = new ServiceReferenceDTO();
		dto.bundle = _bundle.getBundleId();
		dto.id = (Long)getProperty(Constants.SERVICE_ID);
		dto.properties = Maps.of(_properties);
		dto.usingBundles = new long[0];

		return dto;
	}

	@Override
	public String toString() {
		return toDTO().toString();
	}

	public static final AtomicLong _serviceIds = new AtomicLong();

	private static final Integer _ZERO = new Integer(0);

	private final Bundle _bundle;
	private final Dictionary<String, Object> _properties = new Hashtable<>();
	private final S _service;

}

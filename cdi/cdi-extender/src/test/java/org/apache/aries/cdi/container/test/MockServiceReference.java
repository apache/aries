package org.apache.aries.cdi.container.test;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class MockServiceReference<S> implements ServiceReference<S> {

	public MockServiceReference(S service) {
		_service = service;
		_properties.put(Constants.SERVICE_ID, _serviceIds.incrementAndGet());
	}

	@Override
	public int compareTo(Object other) {
		if (!(other instanceof ServiceReference)) {
			return -1;
		}

		ServiceReference otherReference = (ServiceReference)other;

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
		return null;
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

	public static final AtomicInteger _serviceIds = new AtomicInteger();

	private static final Integer _ZERO = new Integer(0);

	private final Dictionary<String, Object> _properties = new Hashtable<>();
	private final S _service;

}

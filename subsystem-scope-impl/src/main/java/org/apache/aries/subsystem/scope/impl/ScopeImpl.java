package org.apache.aries.subsystem.scope.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ScopeImpl implements Scope {
	private static class UnmodifiableSharePolicyMap implements Map<String, List<SharePolicy>> {
		private final Map<String, List<SharePolicy>> map;
		
		public UnmodifiableSharePolicyMap(Map<String, List<SharePolicy>> map) {
			this.map = map;
		}
		
		public void clear() {
			throw new UnsupportedOperationException();
		}

		public boolean containsKey(Object key) {
			return map.containsKey(key);
		}

		public boolean containsValue(Object value) {
			return map.containsValue(value);
		}

		public Set<java.util.Map.Entry<String, List<SharePolicy>>> entrySet() {
			Set<Map.Entry<String, List<SharePolicy>>> result = new HashSet<Map.Entry<String, List<SharePolicy>>>(map.size());
			for (final Map.Entry<String, List<SharePolicy>> entry : map.entrySet()) {
				result.add(new Map.Entry<String, List<SharePolicy>>() {
					public String getKey() {
						return entry.getKey();
					}

					public List<SharePolicy> getValue() {
						return entry.getValue();
					}

					public List<SharePolicy> setValue(List<SharePolicy> object) {
						throw new UnsupportedOperationException();
					}
				});
			}
			return Collections.unmodifiableSet(result);
		}

		public List<SharePolicy> get(Object key) {
			List<SharePolicy> result = map.get(key);
			return result == null ? null : Collections.unmodifiableList(result);
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

		public Set<String> keySet() {
			return Collections.unmodifiableSet(map.keySet());
		}

		public List<SharePolicy> put(String key, List<SharePolicy> value) {
			throw new UnsupportedOperationException();
		}

		public void putAll(Map<? extends String, ? extends List<SharePolicy>> map) {
			throw new UnsupportedOperationException();
		}

		public List<SharePolicy> remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			return map.size();
		}

		public Collection<List<SharePolicy>> values() {
			return Collections.unmodifiableCollection(map.values());
		}
	}
	
	private static long lastId = -1;
	
	private static synchronized long nextId() {
		if (lastId == Long.MAX_VALUE)
			throw new IllegalStateException("The next ID would exceed Long.MAX_VALUE");
		return ++lastId;
	}
	
	volatile boolean updating;
	
	long lastUpdate;
	
	final BundleContext bundleContext;
	final Set<Bundle> bundles = Collections.synchronizedSet(new HashSet<Bundle>());
	final Set<Scope> children = Collections.synchronizedSet(new HashSet<Scope>());
	final Map<String, List<SharePolicy>> exportPolicies = Collections.synchronizedMap(new HashMap<String, List<SharePolicy>>());
	final Map<String, List<SharePolicy>> importPolicies = Collections.synchronizedMap(new HashMap<String, List<SharePolicy>>());
	
	private final long id;
	private final String location;
	private final String name;
	private final Scope parent;
	
	public ScopeImpl(
			BundleContext bundleContext,
			String name,
			String location,
			Scope parent) {
		this.bundleContext = bundleContext;
		this.name = name;
		this.location = location;
		this.parent = parent;
		id = nextId();
	}

	public Collection<Bundle> getBundles() {
		return Collections.unmodifiableCollection(bundles);
	}
	
	public Collection<Scope> getChildren() {
		return Collections.unmodifiableCollection(children);
	}
	
	public long getId() {
		return id;
	}
	
	public String getLocation() {
		return location;
	}
	
	public String getName() {
		return name;
	}
	
	public Scope getParent() {
		return parent;
	}

	public Map<String, List<SharePolicy>> getSharePolicies(String type) {
		if (SharePolicy.TYPE_EXPORT.equals(type))
			return new UnmodifiableSharePolicyMap(exportPolicies);
		else if (SharePolicy.TYPE_IMPORT.equals(type))
			return new UnmodifiableSharePolicyMap(importPolicies);
		throw new IllegalArgumentException(type);
	}
	
	public ScopeUpdate newScopeUpdate() {
		return ScopeUpdateImpl.newInstance(this);
	}
}

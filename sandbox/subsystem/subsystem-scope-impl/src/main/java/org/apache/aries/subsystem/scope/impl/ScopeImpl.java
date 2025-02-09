package org.apache.aries.subsystem.scope.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.Bundle;

public class ScopeImpl implements Scope {
	private volatile boolean updating;
	
	private long lastUpdate;
	
	private final Collection<Bundle> bundles = Collections.synchronizedSet(new HashSet<Bundle>());
	private final Set<Scope> children = Collections.synchronizedSet(new HashSet<Scope>());
	private final long id;
	private final String location;
	private final String name;
	private final long parentId;
	private final Scopes scopes;
	private final SharePolicies sharePolicies;
	
	public ScopeImpl(long id, String name, String location, long parentId, Collection<Bundle> bundles, SharePolicies sharePolicies, Scopes scopes) {
		this.id = id;
		this.name = name;
		this.location = location;
		this.parentId = parentId;
		if (bundles != null) {
			this.bundles.addAll(bundles);
		}
		this.sharePolicies = sharePolicies;
		this.scopes = scopes;
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
		return scopes.getScope(parentId);
	}

	public Map<String, List<SharePolicy>> getSharePolicies(String type) {
		return Collections.unmodifiableMap(sharePolicies.getSharePolicies(type));
	}
	
	public ScopeUpdate newScopeUpdate() {
		return ScopeUpdateImpl.newInstance(this);
	}
	
	void addBundle(Bundle bundle) {
		bundles.add(bundle);
	}
	
	void addChild(ScopeImpl child) {
		children.add(child);
	}
	
	synchronized long getLastUpdate() {
		return lastUpdate;
	}
	
	Scopes getScopes() {
		return scopes;
	}
	
	SharePolicies getSharePolicies() {
		return sharePolicies;
	}
	
	boolean isUpdating() {
		return updating;
	}
	
	void removeBundle(Bundle bundle) {
		bundles.remove(bundle);
	}
	
	void removeChild(ScopeImpl scope) {
		children.remove(scope);
	}
	
	synchronized void setLastUpdate(long value) {
		lastUpdate = value;
	}
	
	void setUpdating(boolean value) {
		updating = value;
	}
}

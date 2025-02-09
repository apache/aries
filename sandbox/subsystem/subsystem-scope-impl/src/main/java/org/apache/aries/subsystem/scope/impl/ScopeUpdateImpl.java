package org.apache.aries.subsystem.scope.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.apache.aries.subsystem.scope.internal.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class ScopeUpdateImpl implements ScopeUpdate {
	public static ScopeUpdateImpl newInstance(ScopeImpl scope) {
		ScopeUpdateImpl scopeUpdate = new ScopeUpdateImpl(scope, null);
		for (Scope child : scopeUpdate.scope.getChildren()) {
			scopeUpdate.children.add(new ScopeUpdateImpl((ScopeImpl)child, scopeUpdate));
		}
		return scopeUpdate;
	}
	
	private static final IdGenerator idGenerator = new IdGenerator(1);
	
	private final Set<Bundle> bundles = Collections.synchronizedSet(new HashSet<Bundle>());
	private final List<InstallInfo> bundlesToInstall = Collections.synchronizedList(new ArrayList<InstallInfo>());
	private final Set<ScopeUpdate> children = Collections.synchronizedSet(new HashSet<ScopeUpdate>());
	private final long id = idGenerator.nextId();
	private final ScopeUpdateImpl parent;
	private final ScopeImpl scope;
	private final SharePolicies sharePolicies = new SharePolicies();
	
	private ScopeUpdateImpl(String name, String location, ScopeUpdateImpl parent) {
		this.parent = parent;
		this.scope = new ScopeImpl(
				((ScopeImpl)parent.getScope()).getScopes().nextScopeId(),
				name,
				location,
				parent.getScope().getId(),
				null,
				new SharePolicies(),
				((ScopeImpl)parent.getScope()).getScopes());
	}
	
	private ScopeUpdateImpl(ScopeImpl scope, ScopeUpdateImpl parent) {
		this.scope = scope;
		this.parent = parent;
		bundles.addAll(scope.getBundles());
		sharePolicies.replaceAll(scope.getSharePolicies());
	}
	
	public boolean commit() throws BundleException {
		if (parent != null)
			throw new IllegalStateException("Only the root ScopeUpdate may be committed");
		return commit0();
	}
	
	public Collection<Bundle> getBundles() {
		return bundles;
	}
	
	public List<InstallInfo> getBundlesToInstall() {
		return bundlesToInstall;
	}
	
	public Collection<ScopeUpdate> getChildren() {
		return children;
	}

	public String getName() {
		return scope.getName();
	}
	
	public Scope getScope() {
		return scope;
	}

	public Map<String, List<SharePolicy>> getSharePolicies(String type) {
		return sharePolicies.getSharePolicies(type);
	}

	public ScopeUpdate newChild(String name) {
		return newChild(name, null);
	}

	public ScopeUpdate newChild(String name, String location) {
		return new ScopeUpdateImpl(name, location, this);
	}
		
	private void addBundles() {
		for (Bundle b : getBundles()) {
			if (!getScope().getBundles().contains(b)) {
				if (contains(b, this)) {
					throw new IllegalStateException("Bundle " + b.getSymbolicName() + " being added to scope " + getName() + " but already exists in another scope");
				}
				scope.getScopes().addBundle(b, scope);
			}
		}
	}
	
	private synchronized boolean commit0() throws BundleException {
		synchronized (scope) {
			if (scope.getLastUpdate() > id)
				return false;
			scope.setUpdating(true);
			synchronized (bundles) {
				removeBundles();
			}
			synchronized (children) {
				for (ScopeUpdate child : children) {
					if (!((ScopeUpdateImpl)child).commit0())
						return false;
				}
				uninstallChildren();
			}
			synchronized (bundles) {
				addBundles();
			}
			synchronized (bundlesToInstall) {
				installBundles();
			}
			updateSharePolicies();
			scope.setLastUpdate(id);
			scope.getScopes().addScope(scope);
			scope.setUpdating(false);
			return true;
		}
	}
	
	private boolean contains(Bundle bundle, ScopeUpdateImpl scopeUpdate) {
		// Recurse to the top of the tree and then perform a depth-first search.
		return parent == null ? contains0(bundle, scopeUpdate) : parent.contains(bundle, scopeUpdate);
	}
	
	private boolean contains0(Bundle bundle, ScopeUpdateImpl scopeUpdate) {
		if (!equals(scopeUpdate) && bundles.contains(bundle))
			return true;
		// Depth-first search.
		for (ScopeUpdate child : children) {
			if (((ScopeUpdateImpl)child).contains0(bundle, scopeUpdate)) return true;
		}
		return false;
	}
	
	private void installBundles() throws BundleException {
		for (InstallInfo installInfo : getBundlesToInstall()) {
			ScopeManager.installingBundleToScope.put(installInfo.getLocation(), scope);
			Activator.getBundleContext().installBundle(installInfo.getLocation(), installInfo.getContent());
		}
	}
	
	private void removeBundles() throws BundleException {
		Collection<Bundle> bundles = new HashSet<Bundle>(scope.getBundles());
		for (Bundle b : bundles) {
			if (!getBundles().contains(b)) {
				if (!contains(b, null)) {
					b.uninstall();
				}
				else {
					scope.getScopes().removeBundle(b);
				}
			}
		}
	}
	
	private void uninstall(ScopeImpl scope) throws BundleException {
		for (Scope child : scope.getChildren()) {
			uninstall((ScopeImpl)child);
		}
		Collection<Bundle> bundles = new HashSet<Bundle>(scope.getBundles());
		for (Bundle bundle : bundles) {
			if (!contains(bundle, null)) {
				bundle.uninstall();
			}
		}
		scope.getScopes().removeScope(scope);
	}
	
	private void uninstallChildren() throws BundleException {
		Collection<Scope> children = new HashSet<Scope>(scope.getChildren());
		for (Scope child : children) {
//		for (Iterator<Scope> i = scope.children.iterator(); i.hasNext();) {
//			Scope child = i.next();
			boolean found = false;
			for (ScopeUpdate su : getChildren()) {
				if (child.equals(su.getScope())) {
					found = true;
					break;
				}
			}
			if (!found) {
				uninstall((ScopeImpl)child);
			}
		}
	}
	
	private void updateSharePolicies() {
		scope.getSharePolicies().replaceAll(sharePolicies);
	}
 }

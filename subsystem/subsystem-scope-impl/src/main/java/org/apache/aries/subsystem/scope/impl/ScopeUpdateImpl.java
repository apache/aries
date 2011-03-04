package org.apache.aries.subsystem.scope.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class ScopeUpdateImpl implements ScopeUpdate {
	public static ScopeUpdateImpl newInstance(ScopeImpl scope) {
		ScopeUpdateImpl scopeUpdate = new ScopeUpdateImpl(null, null, scope, null);
		for (Scope child : scopeUpdate.scope.getChildren()) {
			scopeUpdate.children.add(new ScopeUpdateImpl(null, null, (ScopeImpl)child, scopeUpdate));
		}
		return scopeUpdate;
	}
	
	public static ScopeUpdateImpl newInstance(String name, String location, ScopeUpdateImpl parent) {
		return new ScopeUpdateImpl(name, location, null, parent);
	}
	
	private static long lastId;
	private static synchronized long nextId() {
		if (lastId == Long.MAX_VALUE)
			throw new IllegalStateException("The next ID would exceed Long.MAX_VALUE");
		return ++lastId;
	}
	
	private final Set<Bundle> bundles;
	private final List<InstallInfo> bundlesToInstall = new ArrayList<InstallInfo>();
	private final Set<ScopeUpdate> children = new HashSet<ScopeUpdate>();
	private final Map<String, List<SharePolicy>> exportPolicies;
	private final long id = nextId();
	private final Map<String, List<SharePolicy>> importPolicies;
	private final ScopeUpdateImpl parent;
	private final ScopeImpl scope;
	
	private ScopeUpdateImpl(
			String name,
			String location,
			ScopeImpl scope,
			ScopeUpdateImpl parent) {
		if (scope == null)
			scope = new ScopeImpl(parent.scope.bundleContext, name, location, parent.scope);
		this.scope = scope;
		this.parent = parent;
		bundles = new HashSet<Bundle>(scope.getBundles());
		exportPolicies = new HashMap<String, List<SharePolicy>>(scope.getSharePolicies(SharePolicy.TYPE_EXPORT));
		importPolicies = new HashMap<String, List<SharePolicy>>(scope.getSharePolicies(SharePolicy.TYPE_IMPORT));
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
		if (SharePolicy.TYPE_EXPORT.equals(type))
			return exportPolicies;
		else if (SharePolicy.TYPE_IMPORT.equals(type))
			return importPolicies;
		throw new IllegalArgumentException(type);
	}

	public ScopeUpdate newChild(String name) {
		return newChild(name, null);
	}

	public ScopeUpdate newChild(String name, String location) {
		return ScopeUpdateImpl.newInstance(name, location, this);
	}
		
	private void addBundles() {
		for (Bundle b : getBundles()) {
			if (!getScope().getBundles().contains(b)) {
				if (contains(b, this)) {
					throw new IllegalStateException("Bundle " + b.getSymbolicName() + " being added to scope " + getName() + " but already exists in another scope");
				}
				scope.bundles.add(b);
				ScopeManager.bundleToScope.put(b, scope);
			}
		}
	}
	
	private boolean commit0() throws BundleException {
		if (scope.lastUpdate > id)
			return false;
		scope.updating = true;
		for (ScopeUpdate child : children) {
			if (!((ScopeUpdateImpl)child).commit0())
				return false;
		}
		removeBundles();
		addBundles();
		installBundles();
		uninstallScopes();
		installScopes();
		updateSharePolicies();
		scope.lastUpdate = id;
		scope.updating = false;
		return true;
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
			scope.bundleContext.installBundle(installInfo.getLocation(), installInfo.getContent());
		}
	}
	
	private void installScopes() {
		for (ScopeUpdate child : getChildren()) {
			if (!getScope().getChildren().contains(child.getScope())) {
				scope.children.add(child.getScope());
			}
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
					scope.bundles.remove(b);
				}
			}
		}
	}
	
	private void uninstallScopes() throws BundleException {
		for (Iterator<Scope> i = scope.children.iterator(); i.hasNext();) {
			Scope child = i.next();
			boolean found = false;
			for (ScopeUpdate su : getChildren()) {
				if (child.equals(su.getScope())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Collection<Bundle> bundles = new HashSet<Bundle>(child.getBundles());
				for (Bundle b : bundles) {
					b.uninstall();
				}
				i.remove();
			}
		}
	}
	
	private void updateSharePolicies() {
		updateExportSharePolicies(getSharePolicies(SharePolicy.TYPE_EXPORT));
		updateImportSharePolicies(getSharePolicies(SharePolicy.TYPE_IMPORT));
	}
	
	private void updateExportSharePolicies(Map<String, List<SharePolicy>> exportPolicies) {
		scope.exportPolicies.clear();
		scope.exportPolicies.putAll(exportPolicies);
	}
	
	private void updateImportSharePolicies(Map<String, List<SharePolicy>> importPolicies) {
		scope.importPolicies.clear();
		scope.importPolicies.putAll(importPolicies);
	}
 }

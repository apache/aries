package org.apache.aries.subsystem.scope.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class Scopes {
	private final BundleContext bundleContext;
	private final Map<Bundle, ScopeImpl> bundleToScope = Collections.synchronizedMap(new HashMap<Bundle, ScopeImpl>());
	private final IdGenerator idGenerator;
	private final SortedMap<Long, ScopeImpl> idToScope = Collections.synchronizedSortedMap(new TreeMap<Long, ScopeImpl>());
	
	public Scopes(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		selectAll();
		for (Bundle bundle : bundleContext.getBundles()) {
			if (contains(bundle)) {
				continue;
			}
			addBundle(bundle);
		}
		if (idToScope.isEmpty()) {
			idGenerator = new IdGenerator(0);
		}
		else {
			idGenerator = new IdGenerator(idToScope.lastKey() + 1);
		}
	}
	
	public synchronized void addBundle(Bundle bundle) {
		addBundle(bundle, getRootScope());
	}
	
	public synchronized void addBundle(Bundle bundle, ScopeImpl scope) {
		bundleToScope.put(bundle, scope);
		scope.addBundle(bundle);
		insert(scope);
	}
	
	public synchronized void addScope(ScopeImpl scope) {
		idToScope.put(scope.getId(), scope);
		for (Bundle bundle : scope.getBundles()) {
			bundleToScope.put(bundle, scope);
		}
		ScopeImpl parent = (ScopeImpl)scope.getParent();
		if (parent != null) {
			parent.addChild(scope);
		}
		insert(scope);
		for (Scope child : scope.getChildren()) {
			addScope((ScopeImpl)child);
		}
	}
	
	public synchronized void clear() {
		idToScope.clear();
		bundleToScope.clear();
	}
	
	public synchronized boolean contains(Bundle bundle) {
		return bundleToScope.containsKey(bundle);
	}
	
	public synchronized ScopeImpl getRootScope() {
		ScopeImpl scope = idToScope.get(0L);
		if (scope == null) {
			scope = new ScopeImpl(0, "root", null, -1, Arrays.asList(bundleContext.getBundles()), new SharePolicies(), this);
			addScope(scope);
		}
		return scope;
	}
	
	public synchronized ScopeImpl getScope(Long id) {
		return idToScope.get(id);
	}
	
	public synchronized ScopeImpl getScope(Bundle bundle) {
		ScopeImpl scope = bundleToScope.get(bundle);
		if (scope == null) {
			addBundle(bundle);
			scope = getRootScope();
		}
		return scope;
	}
	
	public long nextScopeId() {
		return idGenerator.nextId();
	}
	
	public synchronized void removeBundle(Bundle bundle) {
		ScopeImpl scope = bundleToScope.remove(bundle);
		if (scope != null) {
			synchronized (scope) {
				scope.removeBundle(bundle);
			}
			insert(scope);
		}
	}
	
	public synchronized void removeScope(ScopeImpl scope) {
		for (Scope child : scope.getChildren()) {
			removeScope((ScopeImpl)child);
		}
		idToScope.remove(scope.getId());
		for (Bundle bundle : scope.getBundles()) {
			bundleToScope.remove(bundle);
		}
		ScopeImpl parent = (ScopeImpl)scope.getParent();
		if (parent != null) {
			parent.removeChild(scope);
		}
		delete(scope);
	}
	
	private void delete(ScopeImpl scope) {
		File file = bundleContext.getDataFile("scope" + scope.getId());
		if (file == null) return;
		file.delete();
	}
	
	private void insert(ScopeImpl scope) {
		File file = bundleContext.getDataFile("scope" + scope.getId());
		if (file == null) return;
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(file));
			insertScope(scope, dos);
		}
		catch (IOException e) {
			// TODO Log this. Remove print stack trace.
			e.printStackTrace();
		}
		finally {
			if (dos != null) {
				try {
					dos.close();
				}
				catch (IOException e) {}
			}
		}
	}
	
	private void insertBundles(Collection<Bundle> bundles, DataOutputStream dos) throws IOException {
		for (Bundle bundle : bundles) {
			dos.writeLong(bundle.getBundleId());
		}
		dos.writeLong(-1);
	}
	
	private void insertScope(ScopeImpl scope, DataOutputStream dos) throws IOException {
		dos.writeLong(scope.getId());
		dos.writeUTF(scope.getName());
		dos.writeUTF(scope.getLocation() == null ? "" : scope.getLocation());
		dos.writeLong(scope.getParent() == null ? -1 : scope.getParent().getId());
		insertBundles(scope.getBundles(), dos);
		insertSharePolicies(scope.getSharePolicies(SharePolicy.TYPE_EXPORT), dos);
		insertSharePolicies(scope.getSharePolicies(SharePolicy.TYPE_IMPORT), dos);
	}
	
	private void insertSharePolicies(Map<String, List<SharePolicy>> sharePolicies, DataOutputStream dos) throws IOException {
		for (String namespace : sharePolicies.keySet()) {
			insertSharePolicies(sharePolicies.get(namespace), dos);
		}
	}
	
	private void insertSharePolicies(List<SharePolicy> sharePolicies, DataOutputStream dos) throws IOException {
		for (SharePolicy sharePolicy : sharePolicies) {
			dos.writeUTF(sharePolicy.getType());
			dos.writeUTF(sharePolicy.getNamespace());
			dos.writeUTF(sharePolicy.getFilter().toString());
		}
	}
	
	private void selectAll() {
		File file = bundleContext.getDataFile("");
		if (file == null) {
			return;
		}
		File[] files = file.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("scope");
			}
		});
		if (files == null || files.length == 0) {
			return;
		}
		for (File f : files) {
			DataInputStream dis = null; 
			try {
				dis = new DataInputStream(new FileInputStream(f));
				addScope(selectScope(dis, bundleContext));
			}
			catch (Exception e) {
				// TODO Log this. Remove print stack trace.
				e.printStackTrace();
			}
			finally {
				if (dis != null) {
					try {
						dis.close();
					}
					catch (IOException e) {}
				}
			}
		}
	}
	
	private Collection<Bundle> selectBundles(DataInputStream dis, BundleContext bundleContext) throws IOException {
		Collection<Bundle> bundles = new ArrayList<Bundle>();
		long bundleId;
		while ((bundleId = dis.readLong()) != -1) {
			Bundle bundle = bundleContext.getBundle(bundleId);
			if (bundle != null) {
				bundles.add(bundle);
			}
		}
		return bundles;
	}
	
	private ScopeImpl selectScope(DataInputStream dis, BundleContext bundleContext) throws InvalidSyntaxException, IOException {
		long id = dis.readLong();
		String name = dis.readUTF();
		String location = dis.readUTF();
		long parentId = dis.readLong();
		Collection<Bundle> bundles = selectBundles(dis, bundleContext);
		SharePolicies sharePolicies = selectSharePolicies(dis);
		return new ScopeImpl(id, name, location, parentId, bundles, sharePolicies, this);
	}
	
	private SharePolicies selectSharePolicies(DataInputStream dis) throws InvalidSyntaxException, IOException {
		SharePolicies sharePolicies = new SharePolicies();
		while (true) {
			try {
				String type = dis.readUTF();
				String namespace = dis.readUTF();
				String filter = dis.readUTF();
				sharePolicies.addSharePolicy(new SharePolicy(type, namespace, FrameworkUtil.createFilter(filter)));
			}
			catch (EOFException e) {
				break;
			}
		}
		return sharePolicies;
	}
}

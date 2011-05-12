package org.apache.aries.subsystem.scope.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.scope.SharePolicy;

public class SharePolicies {
	private final Map<String, Map<String, List<SharePolicy>>> typeToSharePolicies = Collections.synchronizedMap(new HashMap<String, Map<String, List<SharePolicy>>>());
	
	public SharePolicies() {
		init();
	}
	
	public SharePolicies(SharePolicies sharePolicies) {
		replaceAll(sharePolicies);
	}
	
	public synchronized void addSharePolicy(SharePolicy sharePolicy) {
		String type = sharePolicy.getType();
		Map<String, List<SharePolicy>> namespaceToSharePolicies = typeToSharePolicies.get(type);
		if (namespaceToSharePolicies == null) {
			namespaceToSharePolicies = Collections.synchronizedMap(new HashMap<String, List<SharePolicy>>());
			typeToSharePolicies.put(type, namespaceToSharePolicies);
		}
		String namespace = sharePolicy.getNamespace();
		List<SharePolicy> sharePolicies = namespaceToSharePolicies.get(namespace);
		if (sharePolicies == null) {
			sharePolicies = Collections.synchronizedList(new ArrayList<SharePolicy>());
			namespaceToSharePolicies.put(namespace, sharePolicies);
		}
		sharePolicies.add(sharePolicy);
	}
	
	public synchronized Map<String, List<SharePolicy>> getSharePolicies(String type) {
		if (!(SharePolicy.TYPE_EXPORT.equals(type) || SharePolicy.TYPE_IMPORT.equals(type))) {
			throw new IllegalArgumentException(type);
		}
		return typeToSharePolicies.get(type);
	}
	
	public synchronized void removeSharePolicy(SharePolicy sharePolicy) {
		String type = sharePolicy.getType();
		Map<String, List<SharePolicy>> namespaceToSharePolicies = typeToSharePolicies.get(type);
		if (namespaceToSharePolicies == null) {
			return;
		}
		String namespace = sharePolicy.getNamespace();
		List<SharePolicy> sharePolicies = namespaceToSharePolicies.get(namespace);
		if (sharePolicies == null) {
			return;
		}
		sharePolicies.remove(sharePolicy);
		if (sharePolicies.isEmpty()) {
			namespaceToSharePolicies.remove(namespace);
		}
		if (namespaceToSharePolicies.isEmpty()) {
			typeToSharePolicies.remove(type);
		}
	}
	
	public synchronized void replaceAll(SharePolicies sharePolicies) {
		init();
		synchronized (sharePolicies) {
			synchronized (sharePolicies.typeToSharePolicies) {
				for (String type : sharePolicies.typeToSharePolicies.keySet()) {
					Map<String, List<SharePolicy>> namespaceToSharePolicies = sharePolicies.typeToSharePolicies.get(type);
					synchronized (namespaceToSharePolicies) {
						for (String namespace : namespaceToSharePolicies.keySet()) {
							List<SharePolicy> policies = namespaceToSharePolicies.get(namespace);
							synchronized (policies) {
								for (SharePolicy policy : policies) {
									addSharePolicy(policy);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void init() {
		typeToSharePolicies.put(SharePolicy.TYPE_EXPORT, Collections.synchronizedMap(new HashMap<String, List<SharePolicy>>()));
		typeToSharePolicies.put(SharePolicy.TYPE_IMPORT, Collections.synchronizedMap(new HashMap<String, List<SharePolicy>>()));
	}
 }

package org.apache.aries.subsystem.scope.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class ScopeManager {
	static final Map<Bundle, ScopeImpl> bundleToScope = Collections.synchronizedMap(new HashMap<Bundle, ScopeImpl>());
	static final Map<String, ScopeImpl> installingBundleToScope = Collections.synchronizedMap(new HashMap<String, ScopeImpl>());
	
	final ScopeImpl rootScope;
	
	private final BundleContext bundleContext;
	
	public ScopeManager(BundleContext bundleContext) throws BundleException {
		this.bundleContext = bundleContext;
		rootScope = new ScopeImpl(bundleContext, "root", null, null);
	}
	
	public Scope getRootScope() {
		return rootScope;
	}
	
	public EventHook newEventHook() {
		return new EventHook();
	}
	
	public EventListenerHook newEventListenerHook() {
		return new EventListenerHook();
	}
	
	public org.osgi.framework.hooks.bundle.FindHook newBundleFindHook() {
		return new BundleFindHook();
	}
	
	public ResolverHook newResolverHook() {
		return new ResolverHook();
	}
	
	public ResolverHookFactory newResolverHookFactory() {
		return new ResolverHookFactory();
	}
	
	public ServiceFactory newServiceFactory() {
		return new ServiceFactory();
	}
	
	public org.osgi.framework.hooks.service.FindHook newServiceFindHook() {
		return new ServiceFindHook();
	}
	
	private class EventHook implements org.osgi.framework.hooks.bundle.EventHook {
		public void event(BundleEvent event, Collection<BundleContext> contexts) {
			int type = event.getType();
			if (type == BundleEvent.INSTALLED)
				handleInstalledEvent(event);
			handleAnyEvent(event, contexts);
			if (type == BundleEvent.UNINSTALLED)
				handleUninstalledEvent(event);
		}
		
		private void handleAnyEvent(BundleEvent event, Collection<BundleContext> contexts) {
			// All bundles may see system bundle lifecycle changes.
			if (event.getBundle().getBundleId() != 0) {
				// Otherwise, only bundles in the same scope as the bundle undergoing
				// the state change may see the event.
				ScopeImpl scope = bundleToScope.get(event.getBundle());
				Collection<Bundle> bundles = scope.getBundles();
				for (Iterator<BundleContext> i = contexts.iterator(); i.hasNext();) {
					BundleContext bc = i.next();
					if (!bundles.contains(bc.getBundle()))
						i.remove();
				}
			}
		}
		
		private void handleInstalledEvent(BundleEvent event) {
			processOriginBundleOnInstall(event);
			processSourceBundleOnInstall(event);
		}
		
		private void handleUninstalledEvent(BundleEvent event) {
			processSourceBundleOnUninstall(event);
		}
		
		private void processOriginBundleOnInstall(BundleEvent event) {
			Bundle b = event.getOrigin();
			// There's a brief window when Scope Admin is initializing where it's
			// possible for the origin bundle to not be in a scope.
			if (bundleToScope.get(b) == null) {
				bundleToScope.put(b, rootScope);
				rootScope.bundles.add(b);
			}
		}
		
		private void processSourceBundleOnInstall(BundleEvent event) {
			Bundle b = event.getBundle();
			// If the installer is not Scope Admin, add the installed bundle
			// to the installer's scope.
			if (event.getOrigin().getBundleId() != bundleContext.getBundle().getBundleId()) {
				ScopeImpl s = bundleToScope.get(event.getOrigin());
				bundleToScope.put(b, s);
				s.bundles.add(b);
			}
			else {
				ScopeImpl s = installingBundleToScope.remove(b.getLocation());
				bundleToScope.put(b, s);
				s.bundles.add(b);
			}
		}
		
		private void processSourceBundleOnUninstall(BundleEvent event) {
			Bundle b = event.getBundle();
			ScopeImpl s = bundleToScope.remove(b);
			// There's a brief window when Scope Admin is initializing where it's
			// possible for the scope to be null.
			if (s != null) {
				s.bundles.remove(b);
			}
		}
	}
	
	private class BundleFindHook implements org.osgi.framework.hooks.bundle.FindHook {
		public void find(BundleContext context, Collection<Bundle> bundles) {
			Scope scope = bundleToScope.get(context.getBundle());
			// A bundle may only see other bundles within its scope.
			bundles.retainAll(scope.getBundles());
		}
	}
	
	private class EventListenerHook implements org.osgi.framework.hooks.service.EventListenerHook {
		public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerInfo>> listeners) {
			Bundle bundle = event.getServiceReference().getBundle();
			ScopeImpl scope = bundleToScope.get(bundle);
			for (Iterator<BundleContext> i = listeners.keySet().iterator(); i.hasNext();) {
				if (!scope.getBundles().contains(i.next().getBundle()))
					i.remove();
			}
		}
	}
	
	private class ResolverHook implements org.osgi.framework.hooks.resolver.ResolverHook {
		public void end() {
		}
		
		public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
			for (Iterator<BundleCapability> i = candidates.iterator(); i.hasNext();) {
				if (filterMatch(requirement, i.next()))
					i.remove();
			}
		}
		
		public void filterResolvable(Collection<BundleRevision> candidates) {
			for (Iterator<BundleRevision> i = candidates.iterator(); i.hasNext();) {
				BundleRevision candidate = i.next();
				ScopeImpl scope = bundleToScope.get(candidate.getBundle());
				if (scope.updating)
					i.remove();
			}
		}

		public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
			ScopeImpl scope = bundleToScope.get(singleton.getRevision().getBundle());
			for (Iterator<BundleCapability> i = collisionCandidates.iterator(); i.hasNext();) {
				BundleCapability collisionCandidate = i.next();
				if (!scope.getBundles().contains(collisionCandidate.getRevision().getBundle())) {
					i.remove();
				}
			}
		}
		
		private boolean filterMatch(BundleRequirement requirement, BundleCapability capability) {
			Scope scope = bundleToScope.get(requirement.getRevision().getBundle());
			if (scope.getBundles().contains(capability.getRevision().getBundle()))
				return false;
			if (scope.getId() < bundleToScope.get(capability.getRevision().getBundle()).getId()) {
				if (matchesDescendants(scope.getChildren(), capability, null))
					return false;
			}
			return !matchesAncestry(scope, capability);
		}
		
		private boolean matchesPolicyAndContainsBundle(Scope scope, BundleCapability capability, String sharePolicyType) {
			if (matchesPolicy(scope, capability, sharePolicyType)) {
				if (scope.getBundles().contains(capability.getRevision().getBundle())) {
					return true;
				}
			}
			return false;
		}
		
		private boolean matchesPolicy(Scope scope, BundleCapability capability, String sharePolicyType) {
			List<SharePolicy> policies = scope.getSharePolicies(sharePolicyType).get(capability.getNamespace());
			if (policies == null) return false;
			for (SharePolicy policy : policies) {
				if (policy.getFilter().matches(capability.getAttributes())) {
					return true;
				}
			}
			return false;
		}
		
		private boolean matchesAncestry(Scope scope, BundleCapability capability) {
			if (matchesPolicy(scope, capability, SharePolicy.TYPE_IMPORT)) {
				Scope parent = scope.getParent();
				if (parent != null) {
					if (parent.getBundles().contains(capability.getRevision().getBundle())) 
						return true;
					if (matchesDescendants(parent.getChildren(), capability, scope))
						return true;
					return matchesAncestry(parent, capability);
				}
			}
			return false;
		}
		
		private boolean matchesDescendant(Scope child, BundleCapability capability) {
			if (matchesPolicyAndContainsBundle(child, capability, SharePolicy.TYPE_EXPORT))
				return true;
			return matchesDescendants(child.getChildren(), capability, null);
		}
		
		private boolean matchesDescendants(Collection<Scope> children, BundleCapability capability, Scope skip) {
			for (Scope child : children) {
				if (child.equals(skip))
					continue;
				if (matchesDescendant(child, capability)) {
					return true;
				}
			}
			return false;
		}
	}
	
	private class ResolverHookFactory implements org.osgi.framework.hooks.resolver.ResolverHookFactory {
		public ResolverHook begin(java.util.Collection<BundleRevision> triggers) {
			return new ResolverHook();
		}
	}
	
	private class ServiceFactory implements org.osgi.framework.ServiceFactory<Scope> {
		public Scope getService(Bundle b, ServiceRegistration<Scope> sr) {
			ScopeImpl scope = bundleToScope.get(b);
			if (scope == null) {
				scope = rootScope;
				bundleToScope.put(b, scope);
			}
			return scope;
		}

		public void ungetService(Bundle b, ServiceRegistration<Scope> sr, Scope s) {
		}
	}
	
	private class ServiceFindHook implements org.osgi.framework.hooks.service.FindHook {
		public void find(BundleContext context, String name, String filter, boolean allServices, Collection<ServiceReference<?>> references) {
			// System bundle can see all services.
			if (context.getBundle().getBundleId() == 0) return;
			for (Iterator<ServiceReference<?>> i = references.iterator(); i.hasNext();) {
				if (filterMatch(context, i.next()))
					i.remove();
			}
		}
		
		private boolean filterMatch(BundleContext context, ServiceReference<?> reference) {
			Scope scope = bundleToScope.get(context.getBundle());
			if (scope.getBundles().contains(reference.getBundle()))
				return false;
			if (scope.getId() < bundleToScope.get(reference.getBundle()).getId()) {
				if (matchesDescendants(scope.getChildren(), reference))
					return false;
			}
			return !matchesAncestry(scope, reference);
		}
		
		private boolean matchesPolicyAndContainsBundle(Scope scope, ServiceReference<?> reference, String sharePolicyType) {
			if (matchesPolicy(scope, reference, sharePolicyType)) {
				if (scope.getBundles().contains(reference.getBundle())) {
					return true;
				}
			}
			return false;
		}
		
		private boolean matchesPolicy(Scope scope, ServiceReference<?> reference, String sharePolicyType) {
			List<SharePolicy> policies = scope.getSharePolicies(sharePolicyType).get("scope.share.service");
			if (policies == null) return false;
			for (SharePolicy policy : policies) {
				if (policy.getFilter().match(reference)) {
					return true;
				}
			}
			return false;
		}
		
		private boolean matchesAncestry(Scope scope, ServiceReference<?> reference) {
			if (matchesPolicy(scope, reference, SharePolicy.TYPE_IMPORT)) {
				Scope parent = scope.getParent();
				if (parent != null) {
					if (parent.getBundles().contains(reference.getBundle())) 
						return true;
					return matchesAncestry(parent, reference);
				}
			}
			return false;
		}
		
		private boolean matchesDescendant(Scope child, ServiceReference<?> reference) {
			if (matchesPolicyAndContainsBundle(child, reference, SharePolicy.TYPE_EXPORT))
				return true;
			return matchesDescendants(child.getChildren(), reference);
		}
		
		private boolean matchesDescendants(Collection<Scope> children, ServiceReference<?> reference) {
			for (Scope child : children) {
				if (matchesDescendant(child, reference)) {
					return true;
				}
			}
			return false;
		}
	}
}

package org.apache.aries.subsystem.scope.impl;

import java.io.IOException;
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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class ScopeManager 
		implements 
				EventHook, 
				EventListenerHook, 
				org.osgi.framework.hooks.bundle.FindHook,
				org.osgi.framework.hooks.service.FindHook, 
				ResolverHook, 
				ResolverHookFactory {
	
	static final Map<String, ScopeImpl> installingBundleToScope = Collections.synchronizedMap(new HashMap<String, ScopeImpl>());
	
	private final BundleContext bundleContext;
	private final Scopes scopes;
	
	public ScopeManager(BundleContext bundleContext) throws InvalidSyntaxException, IOException {
		this.bundleContext = bundleContext;
		scopes = new Scopes(bundleContext);
	}
	
	public ResolverHook begin(java.util.Collection<BundleRevision> triggers) {
		return this;
	}
	
	public void end() {
	}
	
	public void event(BundleEvent event, Collection<BundleContext> contexts) {	
		int type = event.getType();
		Bundle source = event.getBundle();
		if (type == BundleEvent.INSTALLED) {
			// For bundle installed events, the origin is the bundle whose context
			// was used to install the source bundle. In this case, we need to be
			// sure the origin bundle is assigned to a scope. This is necessary to
			// ensure the next step will succeed. This condition may occur, for
			// example, during Scope Admin initialization.
			Bundle origin = event.getOrigin();
			synchronized (scopes) {
				if (!scopes.contains(origin)) {
					scopes.addBundle(origin);
				}
				// If Scope Admin is not the installer, add the installed bundle to the
				// origin bundle's scope. This will occur whenever bundles are not
				// installed via Scope Admin.
				if (origin.getBundleId() != bundleContext.getBundle().getBundleId()) {
					scopes.addBundle(source, scopes.getScope(origin));
				}
				// Otherwise, assign the installed bundle to the scope designated by the scope update.
				else {
					ScopeImpl scope = installingBundleToScope.remove(source.getLocation());
					scopes.addBundle(source, scope);
				}
			}
		}
		// Now filter the event listeners, if necessary. Only bundles in the same scope as the 
		// bundle undergoing the state change may see the event. The one exception is the
		// system bundle, which receives all events and sends events to all listeners.
		if (source.getBundleId() == 0) return;
		ScopeImpl scope = scopes.getScope(source);
		Collection<Bundle> bundles = scope.getBundles();
		for (Iterator<BundleContext> i = contexts.iterator(); i.hasNext();) {
			BundleContext bc = i.next();
			Bundle b = bc.getBundle();
			if (b.getBundleId() != 0 && !bundles.contains(b)) {
				i.remove();
			}
		}
		if (type == BundleEvent.UNINSTALLED) {
			// For bundle uninstalled events, remove the bundle from Scope Admin.
			// Note this must be done after filtering the event listeners or the
			// bundle will get added back.
			scopes.removeBundle(source);
		}
	}
	
	public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerInfo>> listeners) {
		Bundle registrar = event.getServiceReference().getBundle();
		ScopeImpl scope = scopes.getScope(registrar);
		for (Iterator<BundleContext> i = listeners.keySet().iterator(); i.hasNext();) {
			Bundle listener = i.next().getBundle();
			if (!scope.getBundles().contains(listener))
				i.remove();
		}
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
			ScopeImpl scope = scopes.getScope(candidate.getBundle());
			if (scope.isUpdating())
				i.remove();
		}
	}

	public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
		ScopeImpl scope = scopes.getScope(singleton.getRevision().getBundle());
		for (Iterator<BundleCapability> i = collisionCandidates.iterator(); i.hasNext();) {
			BundleCapability collisionCandidate = i.next();
			if (!scope.getBundles().contains(collisionCandidate.getRevision().getBundle())) {
				i.remove();
			}
		}
	}
	
	public void find(BundleContext context, Collection<Bundle> bundles) {
		// The system bundle may see all bundles.
		if (context.getBundle().getBundleId() == 0) return;
		Scope scope = scopes.getScope(context.getBundle());
		for (Iterator<Bundle> i = bundles.iterator(); i.hasNext();) {
			Bundle bundle = i.next();
			// All bundles may see the system bundle.
			if (bundle.getBundleId() == 0) continue;
			// Otherwise, a bundle may only see other bundles within its scope.
			if (!scope.getBundles().contains(bundle))
				i.remove();
		}
	}
	
	public void find(BundleContext context, String name, String filter, boolean allServices, Collection<ServiceReference<?>> references) {
		// System bundle can see all services.
		if (context.getBundle().getBundleId() == 0) return;
		for (Iterator<ServiceReference<?>> i = references.iterator(); i.hasNext();) {
			if (filterMatch(context, i.next()))
				i.remove();
		}
	}
	
	public Scope getRootScope() {
		return scopes.getRootScope();
	}
	
	public Scope getScope(Bundle bundle) {
		return scopes.getScope(bundle);
	}
	
	public void shutdown() {
		scopes.clear();
	}
	
	private boolean filterMatch(BundleRequirement requirement, BundleCapability capability) {
		Scope scope = scopes.getScope(requirement.getRevision().getBundle());
		if (scope.getBundles().contains(capability.getRevision().getBundle()))
			return false;
		if (scope.getId() < scopes.getScope(capability.getRevision().getBundle()).getId()) {
			if (matchesDescendants(scope.getChildren(), capability, null))
				return false;
		}
		return !matchesAncestry(scope, capability);
	}
	
	private boolean filterMatch(BundleContext context, ServiceReference<?> reference) {
		Scope scope = scopes.getScope(context.getBundle());
		if (scope.getBundles().contains(reference.getBundle()))
			return false;
		if (scope.getId() < scopes.getScope(reference.getBundle()).getId()) {
			if (matchesDescendants(scope.getChildren(), reference))
				return false;
		}
		return !matchesAncestry(scope, reference);
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
	
	private boolean matchesDescendant(Scope child, BundleCapability capability) {
		if (matchesPolicyAndContainsBundle(child, capability, SharePolicy.TYPE_EXPORT))
			return true;
		return matchesDescendants(child.getChildren(), capability, null);
	}
	
	private boolean matchesDescendant(Scope child, ServiceReference<?> reference) {
		if (matchesPolicyAndContainsBundle(child, reference, SharePolicy.TYPE_EXPORT))
			return true;
		return matchesDescendants(child.getChildren(), reference);
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
	
	private boolean matchesDescendants(Collection<Scope> children, ServiceReference<?> reference) {
		for (Scope child : children) {
			if (matchesDescendant(child, reference)) {
				return true;
			}
		}
		return false;
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
}

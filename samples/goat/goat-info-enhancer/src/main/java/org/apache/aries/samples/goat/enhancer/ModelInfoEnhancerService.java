/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.goat.enhancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.samples.goat.api.ComponentInfo;
import org.apache.aries.samples.goat.api.ComponentInfoProvider;
import org.apache.aries.samples.goat.api.ModelInfoService;
import org.apache.aries.samples.goat.api.RelationshipInfo;
import org.apache.aries.samples.goat.api.RelationshipInfoProvider;
import org.apache.aries.samples.goat.info.ComponentInfoImpl;
import org.apache.aries.samples.goat.info.RelationshipInfoImpl;

public class ModelInfoEnhancerService implements ModelInfoService,
		ComponentInfoProvider, RelationshipInfoProvider,
		ComponentInfoProvider.ComponentInfoListener,
		RelationshipInfoProvider.RelationshipInfoListener {

	private static final String SERVICE_REGISTRATION = "Service registration";

	private static final String SERVICE_USAGE = "Service usage";

	// TODO where should we expose these shared strings?
	private static final String SERVICE = "Service";

	private ModelInfoService originalService;

	private final Map<String, ComponentInfo> components = new HashMap<String, ComponentInfo>();
	private final Map<String, RelationshipInfo> relationships = new HashMap<String, RelationshipInfo>();

	private final List<ComponentInfoListener> clisteners;
	private final List<RelationshipInfoListener> rlisteners;

	public ModelInfoEnhancerService(ModelInfoService infoService) {

		clisteners = Collections
				.synchronizedList(new ArrayList<ComponentInfoListener>());
		rlisteners = Collections
				.synchronizedList(new ArrayList<RelationshipInfoListener>());

		this.originalService = infoService;
		Collection<ComponentInfo> originalComponents = originalService
				.getComponentInfoProvider().getComponents();
		// We keep all the original components
		for (ComponentInfo info : originalComponents) {
			components.put(info.getId(), info);
		}
		// We add a new component for each service
		Collection<RelationshipInfo> originalRelationships = originalService
				.getRelationshipInfoProvider().getRelationships();
		// We keep all the original components
		for (RelationshipInfo rel : originalRelationships) {

			if (SERVICE.equals(rel.getType())) {
				ComponentInfoImpl serviceComponent = new ComponentInfoImpl();
				String id = constructServiceComponentId(rel);
				serviceComponent.setId(id);
				Map<String, String> componentProperties = new HashMap<String, String>();
				componentProperties.put("Name", rel.getName());
				serviceComponent.setComponentProperties(componentProperties);

				components.put(id, serviceComponent);

				// Make new relationships;

				RelationshipInfoImpl registration = new RelationshipInfoImpl();
				registration.setType(SERVICE_REGISTRATION);
				registration.setName(rel.getName());
				registration.setProvidedBy(rel.getProvidedBy());
				registration.setRelationshipAspects(rel
						.getRelationshipAspects());

				ArrayList<ComponentInfo> arrayList = new ArrayList<ComponentInfo>();
				arrayList.add(serviceComponent);
				registration.setConsumedBy(arrayList);

				relationships.put(constructId(registration), registration);

				RelationshipInfoImpl consumption = new RelationshipInfoImpl();
				consumption.setType(SERVICE_USAGE);
				consumption.setName(rel.getName());
				consumption.setProvidedBy(serviceComponent);
				consumption.setConsumedBy(rel.getConsumedBy());
				consumption
						.setRelationshipAspects(rel.getRelationshipAspects());

				relationships.put(constructId(consumption), consumption);

			} else {
				// Pass non-service relationships through
				relationships.put(constructId(rel), rel);

			}

			originalService.getComponentInfoProvider()
					.registerComponentInfoListener(this);
			originalService.getRelationshipInfoProvider()
					.registerRelationshipInfoListener(this);
		}

	}

	@Override
	public String getName() {
		return "Model Enhancer Service";
	}

	@Override
	public ComponentInfoProvider getComponentInfoProvider() {
		return this;
	}

	@Override
	public RelationshipInfoProvider getRelationshipInfoProvider() {
		return this;
	}

	@Override
	public Collection<RelationshipInfo> getRelationships() {
		return relationships.values();
	}

	@Override
	public Collection<ComponentInfo> getComponents() {
		return components.values();
	}

	@Override
	public ComponentInfo getComponentForId(String id) {
		return components.get(id);
	}

	@Override
	public void registerRelationshipInfoListener(
			RelationshipInfoListener listener) {
		rlisteners.add(listener);
	}

	@Override
	public void registerComponentInfoListener(ComponentInfoListener listener) {
		clisteners.add(listener);
	}

	@Override
	public void updateRelationship(RelationshipInfo r) {
		if (SERVICE.equals(r.getType())) {
			updateSyntheticServiceArtefactsAndNotifyListeners(r);
		} else {
			// Update our copy
			relationships.put(constructId(r), r);
			// This shouldn't affect us, but pass it on to our listeners
			for (RelationshipInfoListener listener : rlisteners) {
				listener.updateRelationship(r);
			}
		}

	}

	@Override
	public void removeRelationship(RelationshipInfo r) {

		if (SERVICE.equals(r.getType())) {
			removeSyntheticServiceArtefactsAndNotifyListeners(r);
		} else {
			// We don't want to track this relationship anymore
			String id = constructId(r);
			RelationshipInfo relationship = relationships.get(id);
			relationships.remove(id);
			if (relationship != null) {
				// This shouldn't affect us, but pass it on to our listeners
				for (RelationshipInfoListener listener : rlisteners) {
					listener.removeRelationship(relationship);
				}
			}
		}

	}

	@Override
	public void updateComponent(ComponentInfo b) {
		// Update our copy
		components.put(b.getId(), b);
		// This shouldn't affect us, but pass it on to our listeners
		for (ComponentInfoListener listener : clisteners) {
			listener.updateComponent(b);
		}

	}

	@Override
	public void removeComponent(ComponentInfo b) {
		// This shouldn't affect us unless it has relationships pointing to it
		// Cheerfully assume that gets handled upstream

		// We don't want to know about this component anymore
		ComponentInfo component = components.remove(b);
		if (component != null) {// This shouldn't affect us, but pass it on to
								// our listeners
			for (ComponentInfoListener listener : clisteners) {
				listener.removeComponent(component);
			}
		}

	}

	private String constructServiceComponentId(RelationshipInfo rel) {
		return "/syntheticenhancedservices/" + rel.getName() + "/"
				+ rel.getProvidedBy().getId();
	}

	private String constructId(RelationshipInfo b) {
		return b.getType() + "/" + b.getName() + "/"
				+ b.getProvidedBy().getId();
	}

	private void removeSyntheticServiceArtefactsAndNotifyListeners(
			RelationshipInfo r) {
		// We need to remove our two relationships and the synthetic
		// component

		String componentId = constructServiceComponentId(r);

		// Do the relationships first
		// The registration has type "service registration", and the
		// original provider and name
		String registrationRelationshipId = SERVICE_REGISTRATION + "/"
				+ r.getName() + "/" + r.getProvidedBy().getId();
		RelationshipInfo registrationRelationship = relationships
				.get(registrationRelationshipId);

		// The consumers have type "service usage", and the
		// original name, and the new provided by

		String usageRelationshipId = SERVICE_USAGE + "/" + r.getName() + "/"
				+ componentId;
		RelationshipInfo usageRelationship = relationships
				.get(usageRelationshipId);

		relationships.remove(usageRelationshipId);
		relationships.remove(registrationRelationshipId);

		// Tell our listeners about the relationships first

		for (RelationshipInfoListener listener : rlisteners) {
			if (usageRelationship != null) {
				listener.removeRelationship(usageRelationship);
			}
			if (registrationRelationship != null) {
				listener.removeRelationship(registrationRelationship);
			}

		}

		ComponentInfo component = components.remove(componentId);
		if (component != null) {
			// Tell our listeners their service component went away
			for (ComponentInfoListener listener : clisteners) {
				listener.removeComponent(component);
			}
		}
	}

	private void updateSyntheticServiceArtefactsAndNotifyListeners(
			RelationshipInfo r) {
		// We need to update our two relationships and the synthetic
		// component
		// Hopefully the thing which changed won't prevent us
		// from finding our relationship

		String componentId = constructServiceComponentId(r);

		// Do the relationships first
		// The registration has type "service registration", and the
		// original provider and name
		String registrationRelationshipId = SERVICE_REGISTRATION + "/"
				+ r.getName() + "/" + r.getProvidedBy().getId();
		RelationshipInfoImpl registrationRelationship = (RelationshipInfoImpl) relationships
				.get(registrationRelationshipId);
		registrationRelationship.setName(r.getName());
		registrationRelationship.setRelationshipAspects(r
				.getRelationshipAspects());

		// The consumers have type "service usage", and the
		// original name, and the new provided by

		String usageRelationshipId = SERVICE_USAGE + "/" + r.getName() + "/"
				+ componentId;
		RelationshipInfoImpl usageRelationship = (RelationshipInfoImpl) relationships
				.get(usageRelationshipId);

		// The consumers may have changed, so we update the usage relationship
		usageRelationship.setConsumedBy(r.getConsumedBy());
		usageRelationship.setName(r.getName());
		usageRelationship.setRelationshipAspects(r.getRelationshipAspects());

		// Tell our listeners about the relationships first

		for (RelationshipInfoListener listener : rlisteners) {
			if (usageRelationship != null) {
				listener.updateRelationship(usageRelationship);
			}
			if (registrationRelationship != null) {
				listener.updateRelationship(registrationRelationship);
			}

		}

		ComponentInfo component = components.get(componentId);
		if (component != null) {
			// Tell our listeners their service component was updated
			for (ComponentInfoListener listener : clisteners) {
				listener.updateComponent(component);
			}
		}
	}
}

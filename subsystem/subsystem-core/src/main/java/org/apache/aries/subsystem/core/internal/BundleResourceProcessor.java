/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.net.URL;
import java.util.Set;

import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceOperation;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

public class BundleResourceProcessor implements ResourceProcessor {
	private final BundleContext bundleContext;
	
	public BundleResourceProcessor(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}
	
	public void process(final ResourceOperation operation) throws SubsystemException {
		switch (operation.getType()) {
			case INSTALL:
				install(operation);
				break;
			case START:
				start(operation);
				break;
			case STOP:
				stop(operation);
				break;
			case UNINSTALL:
				uninstall(operation);
				break;
			case UPDATE:
				update(operation);
				break;
			default:
				throw new SubsystemException("Unsupported resource opertaion type: " + operation.getType());
		}
	}
	
	private Bundle findBundle(Resource resource, Region region) {        
        Set<Long> bundleIds = region.getBundleIds();
    	for (Long bundleId : bundleIds) {
    		Bundle b = bundleContext.getBundle(bundleId);
    		String location = String.valueOf(resource.getAttributes().get(Resource.LOCATION_ATTRIBUTE));
            if (location.equals(b.getLocation())) {
                return b;
            }
    	}
        return null;
    }
	
	private void install(final ResourceOperation operation) {
		Coordination coordination = operation.getCoordination();
		final Resource resource = operation.getResource();
		final Region region = (Region)operation.getContext().get("region");
		String location = String.valueOf(resource.getAttributes().get(Resource.LOCATION_ATTRIBUTE));
        try {
        	region.installBundle(location, new URL(location).openStream());
            coordination.addParticipant(new Participant() {
				public void ended(Coordination c) throws Exception {
					operation.completed();
				}

				public void failed(Coordination c) throws Exception {
					Bundle bundle = findBundle(resource, region);
					region.removeBundle(bundle);
				}
            });
        }
        catch (Exception e) {
        	coordination.fail(e);
        }
	}
	
	private void start(final ResourceOperation operation) {
		Resource resource = operation.getResource();
		String startAttribute = String.valueOf(resource.getAttributes().get(SubsystemConstants.RESOURCE_START_ATTRIBUTE));
        if (!"true".equals(startAttribute)) return;
		Coordination coordination = operation.getCoordination();
		Region region = (Region)operation.getContext().get("region");
		final Bundle bundle = findBundle(resource, region);
		try {
			bundle.start();
			coordination.addParticipant(new Participant() {
				public void ended(Coordination c) throws Exception {
					operation.completed();
				}

				public void failed(Coordination c) throws Exception {
					bundle.stop();
				}
			});
		}
		catch (Exception e) {
			coordination.fail(e);
		}
	}
	
	private void stop(final ResourceOperation operation) {
		Coordination coordination = operation.getCoordination();
		Resource resource = operation.getResource();
		Region region = (Region)operation.getContext().get("region");
		final Bundle bundle = findBundle(resource, region);
		try {
			bundle.stop();
			coordination.addParticipant(new Participant() {
				public void ended(Coordination c) throws Exception {
					operation.completed();
				}

				public void failed(Coordination c) throws Exception {
					bundle.start();
				}
			});
		}
		catch (Exception e) {
			coordination.fail(e);
		}
	}
	
	private void uninstall(final ResourceOperation operation) {
		Coordination coordination = operation.getCoordination();
		final Resource resource = operation.getResource();
		final Region region = (Region)operation.getContext().get("region");
		Bundle bundle = findBundle(resource, region);
		try {
			region.removeBundle(bundle);
			coordination.addParticipant(new Participant() {
				public void ended(Coordination c) throws Exception {
					operation.completed();
				}

				public void failed(Coordination c) throws Exception {
					String location = String.valueOf(resource.getAttributes().get(Resource.LOCATION_ATTRIBUTE));
					region.installBundle(location);
				}
			});
		}
		catch (Exception e) {
			coordination.fail(e);
		}
	}
	
	private void update(final ResourceOperation operation) {
		Resource resource = operation.getResource();
		String updateAttribute = String.valueOf(resource.getAttributes().get(SubsystemConstants.RESOURCE_UPDATE_ATTRIBUTE));
		if (!"true".equals(updateAttribute)) return;
		final String location = String.valueOf(resource.getAttributes().get(Resource.LOCATION_ATTRIBUTE));
		Region region = (Region)operation.getContext().get("region");
		final Bundle bundle = findBundle(resource, region);
		Coordination coordination = operation.getCoordination();
		coordination.addParticipant(new Participant() {
			public void ended(Coordination c) throws Exception {
				bundle.update(new URL(location).openStream());
				operation.completed();
			}
			
			public void failed(Coordination c) throws Exception {
				// Do nothing.
			}
		});
	}
}

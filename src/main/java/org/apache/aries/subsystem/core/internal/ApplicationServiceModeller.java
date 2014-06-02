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

import java.util.ArrayList;
import java.util.List;

import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.util.filesystem.IDirectory;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemException;

public class ApplicationServiceModeller implements ServiceModeller {

    private final ModelledResourceManager manager;

    public ApplicationServiceModeller(Object manager) {
        this.manager = (ModelledResourceManager) manager;
    }

    @Override
    public ServiceModel computeRequirementsAndCapabilities(Resource resource, IDirectory directory) throws SubsystemException {
        try {
            ServiceModelImpl model = new ServiceModelImpl();
            ParsedServiceElements elements = manager.getServiceElements(directory);
            for (ExportedService service : elements.getServices()) {
                model.capabilities.add(new BasicCapability.Builder()
                        .namespace(ServiceNamespace.SERVICE_NAMESPACE)
                        .attribute(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, new ArrayList<String>(service.getInterfaces()))
                        .attributes(service.getServiceProperties())
                        .resource(resource)
                        .build());
            }
            for (ImportedService service : elements.getReferences()) {
                StringBuilder builder = new StringBuilder();
                String serviceInterface = service.getInterface();
                String filter = service.getFilter();

                if (serviceInterface != null && filter != null) {
                	builder.append("(&");
                }
                if (serviceInterface != null) {
                	builder.append('(')
                        .append(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE)
                        .append('=')
                        .append(serviceInterface)
                        .append(')');
                }

                if (filter != null)
                    builder.append(filter);
                if (serviceInterface != null && filter != null) {
                	builder.append(')');
                }
                if (builder.length() > 0) {
	                model.requirements.add(new BasicRequirement.Builder()
	                        .namespace(ServiceNamespace.SERVICE_NAMESPACE)
	                        .directive(Namespace.REQUIREMENT_FILTER_DIRECTIVE, builder.toString())
	                        .directive(
	                                Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE,
	                                service.isOptional() ? Namespace.RESOLUTION_OPTIONAL : Namespace.RESOLUTION_MANDATORY)
	                        .directive(
	                                Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
	                                service.isMultiple() ? Namespace.CARDINALITY_MULTIPLE : Namespace.CARDINALITY_SINGLE)
	                        .resource(resource)
	                        .build());
                }
            }
            return model;
        } catch (ModellerException e) {
            throw new SubsystemException(e);
        }
    }

    static class ServiceModelImpl implements ServiceModel {
        final List<Requirement> requirements = new ArrayList<Requirement>();
        final List<Capability> capabilities = new ArrayList<Capability>();
        @Override
        public List<Requirement> getServiceRequirements() {
            return requirements;
        }

        @Override
        public List<Capability> getServiceCapabilities() {
            return capabilities;
        }
    }

}

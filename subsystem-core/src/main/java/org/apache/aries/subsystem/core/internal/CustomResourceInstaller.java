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

import java.io.InputStream;

import org.apache.aries.subsystem.ContentHandler;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.repository.RepositoryContent;

public class CustomResourceInstaller extends ResourceInstaller {
    private final ServiceReference<ContentHandler> handlerRef;
    private final String type;

    public CustomResourceInstaller(Coordination coordination, Resource resource, String type,
            BasicSubsystem subsystem, ServiceReference<ContentHandler> handlerRef) {
        super(coordination, resource, subsystem);
        this.handlerRef = handlerRef;
        this.type = type;
    }

    @Override
    public Resource install() throws Exception {
        try {
            ContentHandler handler = subsystem.getBundleContext().getService(handlerRef);
            if (handler != null) {
                InputStream is = ((RepositoryContent) resource).getContent();
                handler.install(is, ResourceHelper.getSymbolicNameAttribute(resource), type, subsystem, coordination);
                addReference(resource);
                return resource;
            } else {
                throw new Exception("Custom content handler not found: " + handlerRef);
            }
        } finally {
            subsystem.getBundleContext().ungetService(handlerRef);
        }
    }
}

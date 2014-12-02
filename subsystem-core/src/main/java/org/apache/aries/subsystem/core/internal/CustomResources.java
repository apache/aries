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

import org.apache.aries.subsystem.ContentHandler;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

class CustomResources {
    private CustomResources() {
        // Only static methods
    }

    /**
     * Find a custom content handler in the context of a given subsystem. Custom content handlers are
     * services of type {@link ContentHandler} with the service registration property
     * {@link ContentHandler#CONTENT_TYPE_PROPERTY} set to the type being handled.
     * @param subsystem The subsystem that provides the context to look up the service.
     * @param type The content type to find the handler for.
     * @return The Service Reference for the Content Handler for the type or {@code null} if not found.
     */
    static ServiceReference<ContentHandler> getCustomContentHandler(BasicSubsystem subsystem, String type) {
        try {
            for(ServiceReference<ContentHandler> ref :
                    subsystem.getBundleContext().getServiceReferences(ContentHandler.class,
                    "(" + ContentHandler.CONTENT_TYPE_PROPERTY + "=" + type + ")")) {
                return ref;
            }
            return null;
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

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
package org.apache.aries.subsystem;

import java.io.InputStream;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.subsystem.Subsystem;

/**
 * A handler for custom content in Subsystems. This handler must be registered as Whiteboard
 * services with the {@link #CONTENT_TYPE_PROPERTY} property indicating the content type this
 * handler must be invoked for. <p>
 *
 * Custom content embedded inside an subsystem archive (e.g. {@code .esa} file) must be declared
 * in the {@code Subsystem-Content} header where the {@link #EMBEDDED_RESOURCE_ATTRIBUTE} can
 * be used to associate it with the name of a file inside the archive.
 */
public interface ContentHandler {
    static final String CONTENT_TYPE_PROPERTY = "org.aries.subsystem.contenthandler.type";
    static final String EMBEDDED_RESOURCE_ATTRIBUTE = "embedded-resource";

    /**
     * Install this custom content.
     * @param is An input stream to the content.
     * @param symbolicName The name of the content.
     * @param contentType The type of the content.
     * @param subsystem The target subsystem.
     * @param coordination The current coordination. Can be used to register a compensation in case of
     * failure or to fail the installation action.
     */
    void install(InputStream is, String symbolicName, String contentType, Subsystem subsystem, Coordination coordination);

    /**
     * Start this custom content.
     * @param symbolicName The name of the content.
     * @param contentType The type of the content.
     * @param subsystem The target subsystem.
     * @param coordination The current coordination. Can be used to register a compensation in case of
     * failure or to fail the start action.
     */
    void start(String symbolicName, String contentType, Subsystem subsystem, Coordination coordination);

    /**
     * Stop this custom content.
     * @param symbolicName The name of the content.
     * @param contentType The type of the content.
     * @param subsystem The target subsystem.
     */
    void stop(String symbolicName, String contentType, Subsystem subsystem);

    /**
     * Uninstall this custom content.
     * @param symbolicName The name of the content.
     * @param contentType The type of the content.
     * @param subsystem The target subsystem.
     */
    void uninstall(String symbolicName, String contentType, Subsystem subsystem);
}

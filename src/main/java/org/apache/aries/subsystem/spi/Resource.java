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
package org.apache.aries.subsystem.spi;

import java.io.IOException;
import java.io.InputStream;

import org.osgi.framework.Version;

/**
 * A resource is the representation of a uniquely identified and typed data.
 * A bundle is represented as a resource with a type
 * {@link org.apache.aries.subsystem.SubsystemConstants#RESOURCE_TYPE_BUNDLE}.
 */
public interface Resource {

    /**
     * Symbolic name of the resource
     *
     * @return
     */
    String getSymbolicName();

    /**
     * Version of the resource
     *
     * @return
     */
    Version getVersion();

    /**
     * Type of the resource
     *
     * @return
     */
    String getType();

    /**
     * Location of the resource.
     * Can be the real url of the resource, but not necesseraly.
     *
     * @return
     */
    String getLocation();

    /**
     * Open the resource for reading.
     *
     * @return
     * @throws IOException
     */
    InputStream open() throws IOException;

}

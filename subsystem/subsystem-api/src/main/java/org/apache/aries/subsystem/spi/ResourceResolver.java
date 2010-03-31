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

import java.util.List;

import org.apache.aries.subsystem.SubsystemException;

/**
 * The ResourceResolver object is used by the SubsystemAdmin to locate
 * resources and resolve a subsystem (i.e. compute its transitive
 * closure).
 *
 * The resolver is an important part, but I'm not sure it actually
 * belong to the SPI, it may be an interface at the implementation level.
 * Putting in the SPI allow to explain its role and put a name on this
 * important actor, but this also mean that people will be allowed
 * to deploy resolver themselves and expect it to be used.
 *
 */
public interface ResourceResolver {

    /**
     * Find the given resource.
     * Usually called with the parsed content of one of the subsystem headers.
     * For example:
     *    my-bsn;version=1.0;type=subsystem
     *
     * @param resource the resource path with its associated metadata
     * @return 
     */
    Resource find(String resource) throws SubsystemException;

    /**
     * Resolve the subsystem by computing the list of resources
     * that need to be installed in addition to the content resources.
     *
     * @param subsystemContent the resources that define the content of the subsystem
     * @param subsystemResources the resources available from the subsystem archive
     * @return
     */
    List<Resource> resolve(List<Resource> subsystemContent, List<Resource> subsystemResources) throws SubsystemException;

}

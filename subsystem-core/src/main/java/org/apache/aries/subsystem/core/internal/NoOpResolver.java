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

import java.util.Collections;
import java.util.List;

import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class NoOpResolver implements ResourceResolver {

    public Resource find(String resource) throws SubsystemException {
        Clause[] clauses = Parser.parseHeader(resource);
        if (clauses.length != 1) {
            throw new SubsystemException("Unsupported resource: " + resource);
        }

        String bsn = clauses[0].getName();
        String ver = clauses[0].getAttribute(Constants.VERSION_ATTRIBUTE);
        String typ = clauses[0].getAttribute(SubsystemConstants.RESOURCE_TYPE_ATTRIBUTE);
        String loc = clauses[0].getAttribute(SubsystemConstants.RESOURCE_LOCATION_ATTRIBUTE);
        if (loc == null) {
            throw new SubsystemException("Mandatory location missing on resource: " + resource);
        }
        return new ResourceImpl(
                bsn,
                ver != null ? new Version(ver) : Version.emptyVersion,
                typ != null ? typ : SubsystemConstants.RESOURCE_TYPE_BUNDLE,
                loc
        );
    }

    public List<Resource> resolve(List<Resource> subsystemContent, List<Resource> subsystemResources) throws SubsystemException {
        return Collections.emptyList();
    }

}

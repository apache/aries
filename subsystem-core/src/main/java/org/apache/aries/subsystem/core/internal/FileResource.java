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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.util.filesystem.IFile;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;

public class FileResource implements Resource, RepositoryContent {
    private final IFile file;
    private volatile Map<String, List<Capability>> capabilities       ;

    public FileResource(IFile file) {
        this.file = file;
    }

    @Override
    public List<Capability> getCapabilities(String namespace) {
        Map<String, List<Capability>> namespace2capabilities = capabilities;
        if (namespace2capabilities == null) {
            return Collections.emptyList();
        }
        List<Capability> caps;
        if (namespace == null) {
            caps = new ArrayList<Capability>();
            for (List<Capability> l : capabilities.values()) {
                caps.addAll(l);
            }
            return Collections.unmodifiableList(caps);
        }
        caps = namespace2capabilities.get(namespace);
        if (caps != null)
            return Collections.unmodifiableList(caps);
        else
            return Collections.emptyList();
    }

    @Override
    public List<Requirement> getRequirements(String namespace) {
        return Collections.emptyList();
    }

    public void setCapabilities(List<Capability> capabilities) {
        Map<String, List<Capability>> m = new HashMap<String, List<Capability>>();
        for (Capability c : capabilities) {
            List<Capability> l = m.get(c.getNamespace());
            if (l == null) {
                l = new ArrayList<Capability>();
                m.put(c.getNamespace(), l);
            }
            l.add(c);
        }
        this.capabilities = m;
    }

    @Override
    public InputStream getContent() {
        try {
            return file.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

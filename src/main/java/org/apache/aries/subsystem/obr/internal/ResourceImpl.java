/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.obr.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.aries.subsystem.spi.Resource;
import org.osgi.framework.Version;

public class ResourceImpl implements Resource {

    private final String symbolicName;
    private final Version version;
    private final String type;
    private final String location;
    private final Map<String,String> attributes;

    public ResourceImpl(String symbolicName, Version version, String type, String location, Map<String,String> attributes) {
        this.symbolicName = symbolicName;
        this.version = version;
        this.type = type;
        this.location = location;
        this.attributes = attributes;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public Version getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public InputStream open() throws IOException {
        return new URL(getLocation()).openStream();
    }

}

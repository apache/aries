/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin;

import org.apache.aries.blueprint.plugin.spi.Activation;
import org.apache.aries.blueprint.plugin.spi.Availability;
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlueprintConfigurationImpl implements BlueprintConfiguration {
    private static final String NS_TX2 = "http://aries.apache.org/xmlns/transactions/v2.0.0";
    private static final String NS_JPA2 = "http://aries.apache.org/xmlns/jpa/v2.0.0";

    private final Set<String> namespaces;
    private final Activation defaultActivation;
    private final Map<String, String> customParameters;
    private final Availability defaultAvailability;
    private final Long defaultTimeout;

    public BlueprintConfigurationImpl(Set<String> namespaces, Activation defaultActivation, Map<String, String> customParameters, Availability defaultAvailability, Long defaultTimeout) {
        this.namespaces = namespaces != null ? namespaces : new HashSet<>(Arrays.asList(NS_TX2, NS_JPA2));
        this.defaultActivation = defaultActivation;
        this.customParameters = customParameters == null ? new HashMap<String, String>() : customParameters;
        this.defaultAvailability = defaultAvailability;
        this.defaultTimeout = defaultTimeout;
        validateTimeout();
    }

    private void validateTimeout() {
        if (defaultTimeout != null && defaultTimeout < 0L) {
            throw new NegativeTimeout(defaultTimeout);
        }
    }

    @Override
    public Set<String> getNamespaces() {
        return namespaces;
    }

    @Override
    public Activation getDefaultActivation() {
        return defaultActivation;
    }

    @Override
    public Availability getDefaultAvailability() {
        return defaultAvailability;
    }

    @Override
    public Long getDefaultTimeout() {
        return defaultTimeout;
    }

    @Override
    public Map<String, String> getCustomParameters() {
        return customParameters;
    }
}

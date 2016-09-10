package org.apache.aries.blueprint.plugin;

import org.apache.aries.blueprint.plugin.spi.Activation;
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlueprintConfigurationImpl implements BlueprintConfiguration {
    public static final String NS_TX2 = "http://aries.apache.org/xmlns/transactions/v2.0.0";
    public static final String NS_JPA2 = "http://aries.apache.org/xmlns/jpa/v2.0.0";

    private final Set<String> namespaces;
    private final Activation defaultActivation;
    private final Map<String, String> customParameters;

    public BlueprintConfigurationImpl(Set<String> namespaces, Activation defaultActivation, Map<String, String> customParameters) {
        this.namespaces = namespaces != null ? namespaces : new HashSet<>(Arrays.asList(NS_TX2, NS_JPA2));
        this.defaultActivation = defaultActivation;
        this.customParameters =  customParameters == null ? new HashMap<String, String>() : customParameters;
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
    public Map<String, String> getCustomParameters() {
        return customParameters;
    }
}

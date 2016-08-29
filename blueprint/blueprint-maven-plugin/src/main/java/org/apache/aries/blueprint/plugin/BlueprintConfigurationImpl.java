package org.apache.aries.blueprint.plugin;

import org.apache.aries.blueprint.plugin.spi.Activation;
import org.apache.aries.blueprint.plugin.spi.BlueprintConfiguration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.aries.blueprint.plugin.Generator.NS_JPA2;

public class BlueprintConfigurationImpl implements BlueprintConfiguration {
    public static final String NS_TX2 = "http://aries.apache.org/xmlns/transactions/v2.0.0";

    private final Set<String> namespaces;
    private final Activation defaultActivation;

    public BlueprintConfigurationImpl(Set<String> namespaces, Activation defaultActivation) {
        this.namespaces = namespaces != null ? namespaces : new HashSet<>(Arrays.asList(NS_TX2, NS_JPA2));
        this.defaultActivation = defaultActivation;
    }

    @Override
    public Set<String> getNamespaces() {
        return namespaces;
    }

    @Override
    public Activation getDefaultActivation() {
        return defaultActivation;
    }
}

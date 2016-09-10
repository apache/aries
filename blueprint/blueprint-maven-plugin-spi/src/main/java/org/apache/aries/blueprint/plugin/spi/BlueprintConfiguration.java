package org.apache.aries.blueprint.plugin.spi;

import java.util.Map;
import java.util.Set;

public interface BlueprintConfiguration {
    Set<String> getNamespaces();
    Activation getDefaultActivation();
    Map<String, String> getCustomParameters();
}

package org.apache.aries.blueprint.plugin.model;

import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextInitializationHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

public class BlueprintInitialization implements ContextInitializationHandler {
    @Override
    public void initContext(ContextEnricher contextEnricher) {
        contextEnricher.addBean("blueprintBundleContext", BundleContext.class);
        contextEnricher.addBean("blueprintBundle", Bundle.class);
        contextEnricher.addBean("blueprintContainer", BlueprintContainer.class);
        contextEnricher.addBean("blueprintConverter", Converter.class);
    }
}

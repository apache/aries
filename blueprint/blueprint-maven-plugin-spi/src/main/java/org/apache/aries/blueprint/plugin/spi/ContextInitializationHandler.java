package org.apache.aries.blueprint.plugin.spi;

public interface ContextInitializationHandler {
    void initContext(ContextEnricher contextEnricher);
}

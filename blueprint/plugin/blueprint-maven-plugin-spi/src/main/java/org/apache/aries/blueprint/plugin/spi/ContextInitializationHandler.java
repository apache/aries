package org.apache.aries.blueprint.plugin.spi;

/**
 * Handler called at the beginning of blueprint XML creation
 */
public interface ContextInitializationHandler {
    /**
     * Add custom XML or add bean to context
     * @param contextEnricher context enricher
     */
    void initContext(ContextEnricher contextEnricher);
}

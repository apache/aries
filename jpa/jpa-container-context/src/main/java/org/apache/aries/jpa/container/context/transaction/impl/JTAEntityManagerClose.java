package org.apache.aries.jpa.container.context.transaction.impl;

/**
 * Close all JTAEntityManager instances on blueprint shutdown
 */
public interface JTAEntityManagerClose {
    void internalClose();
}

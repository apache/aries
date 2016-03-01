package org.apache.aries.jpa.container.quiesce.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A callback for a named persistence units
 */
public class NamedCallback {
    private final Set<String> names;
    private final DestroyCallback callback;

    public NamedCallback(Collection<String> names, DestroyCallback countdown) {
        this.names = new HashSet<String>(names);
        callback = countdown;
    }

    public void callback(String name) {
        boolean winner;
        synchronized (this) {
            winner = !!!names.isEmpty() && names.remove(name) && names.isEmpty();
        }
        if (winner)
            callback.callback();
    }
}
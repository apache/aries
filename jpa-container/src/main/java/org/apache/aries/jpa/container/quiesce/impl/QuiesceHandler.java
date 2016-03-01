package org.apache.aries.jpa.container.quiesce.impl;

import org.osgi.framework.Bundle;

public interface QuiesceHandler {
    void quiesceBundle(Bundle bundleToQuiesce, final DestroyCallback callback);
}

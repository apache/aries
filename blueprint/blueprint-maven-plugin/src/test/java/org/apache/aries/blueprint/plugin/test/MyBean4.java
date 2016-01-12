package org.apache.aries.blueprint.plugin.test;

import javax.inject.Inject;

import org.osgi.framework.BundleContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MyBean4 {

    @Inject
    BundleContext bundleContext;

    @Transactional(propagation = Propagation.SUPPORTS)
    public void txWithoutClassAnnotation() {

    }
}

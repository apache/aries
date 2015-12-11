package org.apache.aries.blueprint.plugin.test;

import javax.inject.Inject;

import org.osgi.framework.BundleContext;
import org.springframework.stereotype.Component;

@Component
public class MyBean4 {

    @Inject
    BundleContext bundleContext;
}

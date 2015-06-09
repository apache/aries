package org.apache.aries.blueprint.plugin.test;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.osgi.framework.BundleContext;

@Singleton
public class MyBean4 {

    @Inject
    BundleContext bundleContext;
}

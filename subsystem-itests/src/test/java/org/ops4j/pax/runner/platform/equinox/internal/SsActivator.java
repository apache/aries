package org.ops4j.pax.runner.platform.equinox.internal;

import org.ops4j.pax.runner.platform.PlatformBuilder;
import org.ops4j.pax.runner.platform.builder.AbstractPlatformBuilderActivator;
import org.osgi.framework.BundleContext;

public class SsActivator extends AbstractPlatformBuilderActivator
{

    /**
     * {@inheritDoc}
     */
    @Override
    protected PlatformBuilder[] createPlatformBuilders( final BundleContext bundleContext )
    {
        return new PlatformBuilder[]{
            new EquinoxPlatformBuilder( bundleContext, "3.2.1" ),
            new EquinoxPlatformBuilder( bundleContext, "3.3.0" ),
            new EquinoxPlatformBuilder( bundleContext, "3.3.1" ),
            new EquinoxPlatformBuilder( bundleContext, "3.3.2" ),
            new EquinoxPlatformBuilder( bundleContext, "3.4.0" ),
            new EquinoxPlatformBuilder( bundleContext, "3.4.1" ),
            new EquinoxPlatformBuilder( bundleContext, "3.4.2" ),
            new EquinoxPlatformBuilder( bundleContext, "3.5.0" ),
            new EquinoxPlatformBuilder( bundleContext, "3.5.1" ),
            new EquinoxPlatformBuilder( bundleContext, "3.6.0" ),
            new EquinoxPlatformBuilder( bundleContext, "V43PROTOTYPE-3.6.0.201003231329" ),
            new EquinoxPlatformBuilderSnapshot( bundleContext )
        };
    }

}
package org.apache.aries.blueprint.plugin.test;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import javax.inject.Singleton;

@Singleton
@OsgiServiceProvider
@Properties({
    @Property(name = "service.ranking", value = "100")
})
public class ServiceWithRanking implements ServiceA {
}
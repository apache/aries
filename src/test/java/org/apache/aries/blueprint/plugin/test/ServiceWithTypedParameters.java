package org.apache.aries.blueprint.plugin.test;

import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;

import javax.inject.Singleton;

@OsgiServiceProvider
@Properties({
        @Property(name = "test1", value = "test"),
        @Property(name = "test2:Integer", value = "15"),
        @Property(name = "test3:java.lang.Boolean", value = "true"),
        @Property(name = "test4:[]", value = "val1|val2"),
        @Property(name = "test5:Short[]", value = "1|2|3"),
        @Property(name = "test6:java.lang.Double[]", value = "1.5|0.8|-7.1")
})
@Singleton
public class ServiceWithTypedParameters {
}

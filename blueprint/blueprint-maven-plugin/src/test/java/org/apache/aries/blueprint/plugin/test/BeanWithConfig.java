package org.apache.aries.blueprint.plugin.test;

import javax.inject.Singleton;

import org.apache.aries.blueprint.api.config.Config;
import org.apache.aries.blueprint.api.config.ConfigProperty;
import org.apache.aries.blueprint.api.config.Property;

@Config//
( //
    pid = "org.apache.aries.my", //
    placeholderPrefix = "$[", //
    placeholderSuffix = "]", //
    defaults = { 
                @Property(key="title", value="My Title")
    }
)
@Singleton
public class BeanWithConfig {
    @ConfigProperty("$[title]")
    String title;
}

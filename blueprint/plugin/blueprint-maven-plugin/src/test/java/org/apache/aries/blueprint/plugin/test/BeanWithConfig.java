package org.apache.aries.blueprint.plugin.test;

import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.config.Config;
import org.apache.aries.blueprint.annotation.config.ConfigProperty;
import org.apache.aries.blueprint.annotation.config.DefaultProperty;

@Config//
( //
    pid = "org.apache.aries.my", //
    placeholderPrefix = "$[", //
    placeholderSuffix = "]", //
    defaults = { 
                @DefaultProperty(key="title", value="My Title")
    }
)
@Singleton
public class BeanWithConfig {
    @ConfigProperty("$[title]")
    String title;
}

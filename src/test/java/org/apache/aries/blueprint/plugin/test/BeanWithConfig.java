package org.apache.aries.blueprint.plugin.test;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.config.Config;
import org.apache.aries.blueprint.annotation.config.ConfigProperties;
import org.apache.aries.blueprint.annotation.config.ConfigProperty;
import org.apache.aries.blueprint.annotation.config.DefaultProperty;

import java.util.Properties;

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

    @Produces
    @Named("producedWithConfigProperty")
    public MyProducedWithConstructor createBean(@ConfigProperty("1000") long test) {
        return null;
    }
}

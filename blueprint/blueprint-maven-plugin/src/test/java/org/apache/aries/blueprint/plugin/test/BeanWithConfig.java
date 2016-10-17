package org.apache.aries.blueprint.plugin.test;

import javax.inject.Singleton;

import org.apache.aries.blueprint.api.config.Config;
import org.apache.aries.blueprint.api.config.ConfigProperty;

@Config(pid="org.apache.aries.my")
@Singleton
public class BeanWithConfig {
    @ConfigProperty("${title}")
    String title;
}

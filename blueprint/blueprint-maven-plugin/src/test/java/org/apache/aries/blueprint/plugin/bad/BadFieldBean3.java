package org.apache.aries.blueprint.plugin.bad;

import javax.inject.Singleton;

import org.apache.aries.blueprint.plugin.test.MyBean1;

@Singleton
public class BadFieldBean3 extends ParentWithInjectedField
{
    private MyBean1 field;
}

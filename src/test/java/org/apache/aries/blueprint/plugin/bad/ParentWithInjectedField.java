package org.apache.aries.blueprint.plugin.bad;

import javax.inject.Inject;

import org.apache.aries.blueprint.plugin.test.MyBean1;

public class ParentWithInjectedField
{
    @Inject
    private MyBean1 field;
}

package org.apache.aries.blueprint.plugin.model;

import com.google.inject.name.Named;

public class BuiltInBeanRef extends BeanRef {

    public BuiltInBeanRef(Class<?> clazz, String id) {
        super(clazz);
        this.id = id;
    }

    @Override
    public boolean matches(BeanRef template) {
        if (template.clazz != this.clazz || template.qualifiers.size() > 1) {
            return false;
        }
        if (template.qualifiers.size() == 0) {
            return true;
        }
        Named name = (Named)template.qualifiers.get(Named.class);
        return name != null && id.equals(name.value());
    }

    
}

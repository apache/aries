package org.apache.aries.blueprint.plugin.model;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

class Registry {
    private SortedSet<BeanRef> reg = new TreeSet<BeanRef>();

    void addBean(BeanRef beanRef) {
        reg.add(beanRef);
    }

    Collection<BeanRef> getBeans(){
        return reg;
    }
}

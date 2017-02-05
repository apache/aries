package org.apache.aries.blueprint.plugin.model;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

class BeanRefStore {
    private SortedSet<BeanRef> reg = new TreeSet<BeanRef>();

    void addBean(BeanRef beanRef) {
        reg.add(beanRef);
    }

    Collection<BeanRef> getBeans() {
        return reg;
    }

    BeanRef getMatching(BeanTemplate template) {
        for (BeanRef bean : reg) {
            if (bean.matches(template)) {
                return bean;
            }
        }
        return null;
    }
}

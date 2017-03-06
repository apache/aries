package org.apache.aries.jpa.blueprint.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

public class BlueprintContainerStub implements BlueprintContainer {

    private Map<String, Object> instances;

    public BlueprintContainerStub() {
        instances = new HashMap<String, Object>();
        instances.put("coordinator", new CoordinatorStub());
        instances.put("em", new EntityManagerStub());
    }

    @Override
    public Set<String> getComponentIds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getComponentInstance(String id) {
        if ("em".equals(id)) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return instances.get(id);
    }

    @Override
    public ComponentMetadata getComponentMetadata(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends ComponentMetadata> Collection<T> getMetadata(Class<T> type) {
        // TODO Auto-generated method stub
        return null;
    }

}

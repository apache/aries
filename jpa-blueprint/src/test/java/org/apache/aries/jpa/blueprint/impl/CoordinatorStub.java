package org.apache.aries.jpa.blueprint.impl;

import java.util.Collection;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

public class CoordinatorStub implements Coordinator {

    @Override
    public Coordination create(String name, long timeMillis) {
        return null;
    }

    @Override
    public Coordination begin(String name, long timeMillis) {
        return null;
    }

    @Override
    public Coordination peek() {
        return null;
    }

    @Override
    public Coordination pop() {
        return null;
    }

    @Override
    public boolean fail(Throwable cause) {
        return false;
    }

    @Override
    public boolean addParticipant(Participant participant) {
        return false;
    }

    @Override
    public Collection<Coordination> getCoordinations() {
        return null;
    }

    @Override
    public Coordination getCoordination(long id) {
        return null;
    }
}

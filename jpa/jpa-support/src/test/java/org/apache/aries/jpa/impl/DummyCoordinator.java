/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.impl;

import java.util.ArrayDeque;
import java.util.Collection;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

public class DummyCoordinator implements Coordinator {

    private java.util.Deque<Coordination> coordinations = new ArrayDeque<Coordination>();

    @Override
    public Coordination create(String name, long timeMillis) {
        throw new IllegalStateException();
    }
    

    @Override
    public Coordination begin(String name, long timeMillis) {
        Coordination oldCoordination = coordinations.peekLast();
        Coordination coordination = new DummyCoordination(oldCoordination);
        this.coordinations.push(coordination);
        return coordination;
    }

    @Override
    public Coordination peek() {
        return coordinations.peek();
    }

    @Override
    public Coordination pop() {
        return coordinations.pop();
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

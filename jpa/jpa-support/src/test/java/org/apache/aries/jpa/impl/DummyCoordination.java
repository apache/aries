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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

public class DummyCoordination implements Coordination {
    private Set<Participant> participants = new HashSet<>();
    private Map<Class<?>, Object> vars = new HashMap<Class<?>, Object>();
    private Coordination enclosing;

    public DummyCoordination(Coordination enclosing) {
        this.enclosing = enclosing;
    }
    
    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void end() {
        Iterator<Participant> it = participants.iterator();
        while (it.hasNext()) {
            try {
                it.next().ended(this);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public boolean fail(Throwable cause) {
        return false;
    }

    @Override
    public Throwable getFailure() {
        return null;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void addParticipant(Participant participant) {
        this.participants.add(participant);
    }

    @Override
    public List<Participant> getParticipants() {
        return null;
    }

    @Override
    public Map<Class<?>, Object> getVariables() {
        return vars ;
    }

    @Override
    public long extendTimeout(long timeMillis) {
        return 0;
    }

    @Override
    public void join(long timeMillis) throws InterruptedException {
    }

    @Override
    public Coordination push() {
        return null;
    }

    @Override
    public Thread getThread() {
        return null;
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    @Override
    public Coordination getEnclosingCoordination() {
        return enclosing;
    }

}

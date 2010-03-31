/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem;

import java.util.EventObject;

/**
 * Event sent to listeners when an operation has been performed on a subsystem.
 */
public class SubsystemEvent {

    public enum Type {
        INSTALLED,
        STARTED,
        STOPPED,
        UPDATED,
        UNINSTALLED,
        RESOLVED,
        STARTING,
        STOPPING
    }
    private final Type type;

    private final long timestamp;

    private final Subsystem subsystem;

    public SubsystemEvent(Type type, long timestamp, Subsystem subsystem) {
        this.type = type;
        this.timestamp = timestamp;
        this.subsystem = subsystem;
    }

    public Type getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Subsystem getSubsystem() {
        return subsystem;
    }

}

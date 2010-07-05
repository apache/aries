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

import java.io.InputStream;

/**
 * Event sent to listeners when an operation has been performed on a subsystem.
 */
public class SubsystemEvent {

    /**
     * The subsystem lifecycle event types that can be produced by a subsystem.
     * See {@link Subsystem} and {@link SubsystemAdmin} for details on the
     * circumstances under which these events are fired.
     */
    public enum Type {
        
        /**
         * Event type used to indicate a subsystem has been installed.
         */
        INSTALLED,

        /**
         * Event type used to indicate a subsystem has been started.
         */
        STARTED,

        /**
         * Event type used to indicate a subsystem has been stopped.
         */
        STOPPED,
        
        /**
         * Event type used to indicate a subsystem has been updated.
         */
        UPDATED,
        
        /**
         * Event type used to indicate a subsystem has been uninstalled.
         */
        UNINSTALLED,
        
        /**
         * Event type used to indicate a subsystem has been resolved.
         */
        RESOLVED,
        
        /**
         * Event type used to indicate a subsystem is starting.
         */
        STARTING,
        
        /**
         * Event type used to indicate a subsystem is stopping.
         */
        STOPPING
    }
    private final Type type;

    private final long timestamp;

    private final Subsystem subsystem;

    /**
     * Constructs a new subsystem event.
     * 
     * @param type The type of the event.  For example, INSTALLED.  See {@link SubsystemEvent.Type} for the list of event types.
     * @param timestamp The timestamp for the event.
     * @param subsystem The subsystem for which the event is being created.
     */
    public SubsystemEvent(Type type, long timestamp, Subsystem subsystem) {
        this.type = type;
        this.timestamp = timestamp;
        this.subsystem = subsystem;
    }

    /**
     * Gets the type of the subsystem event.  See {@link SubsystemEvent.Type} for the list of event types.
     * @return the type of the subsystem event.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the timestamp for the subsystem event.
     * 
     * @return the timestamp for the subsystem event.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the subsystem for which the event was created.
     * 
     * @return the subsystem for which the event was created.
     */
    public Subsystem getSubsystem() {
        return subsystem;
    }

}

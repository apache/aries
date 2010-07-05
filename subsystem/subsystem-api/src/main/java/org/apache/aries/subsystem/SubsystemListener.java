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

/**
 * A listener for subsystem events (see {@link SubsystemEvent}).
 * 
 * Registered OSGi services implementing this interface will be notified of
 * subsystem events when they occur. Notifications will only be delivered for
 * subsystems that are at the same level. For example, events are not seen for
 * parent or child subsystems.
 */
public interface SubsystemListener {

    /**
     * Called to deliver a subsystem event to this subsystem listener.
     * 
     * @param event The subsystem event being delivered.
     */
    void subsystemEvent(SubsystemEvent event);

}

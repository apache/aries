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
import java.util.Collection;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public interface Subsystem {

    public enum State {

        Installed,
        Resolved,
        Starting,
        Stopping,
        Active,
        Uninstalled

    }

    State getState();

    void resolve();

    void start();

    void stop();

    void update();

    void update(InputStream is);

    void uninstall();

    long getSubsystemId();

    String getLocation();

    String getSymbolicName();

    Version getVersion();

    Map<String, String> getHeaders();

    Map<String, String> getHeaders(String locale);

    Collection<Bundle> getConstituents();

}

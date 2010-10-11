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
package org.apache.aries.subsystem.executor;

import java.util.concurrent.Executor;

/**
 * A simple <code>Executor</code> that just runs each job submitted on a
 * new thread. This executor is intended to be registered as a services for
 * use by the SubsystemAdmin. It can be replaced by a different executor with
 * different policies (e.g. maybe using a host runtime's thread pool).
 * 
 */
public class SimpleExecutor implements Executor {

    /**
     * Runs any submitted job on a new thread.
     * 
     * @param command
     *            The <code>Runnable</code> to be executed on a new thread.
     */
    public void execute(Runnable command) {
        new Thread(command).start();
    }

}

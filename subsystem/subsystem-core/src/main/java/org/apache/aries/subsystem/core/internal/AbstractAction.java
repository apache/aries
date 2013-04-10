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
package org.apache.aries.subsystem.core.internal;

import java.security.PrivilegedAction;

import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemException;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

public abstract class AbstractAction implements PrivilegedAction<Object> {
	protected final boolean disableRootCheck;
	protected final BasicSubsystem requestor;
	protected final BasicSubsystem target;
	
	public AbstractAction(BasicSubsystem requestor, BasicSubsystem target, boolean disableRootCheck) {
		this.requestor = requestor;
		this.target = target;
		this.disableRootCheck = disableRootCheck;
	}
	
	protected void checkRoot() {
		if (!disableRootCheck && target.isRoot())
			throw new SubsystemException("This operation may not be performed on the root subsystem");
	}
	
	protected void checkValid() {
		BasicSubsystem s = (BasicSubsystem)Activator.getInstance().getSubsystemServiceRegistrar().getSubsystemService(target);
		if (s != target)
			throw new IllegalStateException("Detected stale subsystem instance: " + s);
	}
	
	protected void waitForStateChange(State fromState) {
		long then = System.currentTimeMillis() + 60000;
		synchronized (target) {
			while (target.getState().equals(fromState)) {
				// State change has not occurred.
				long now = System.currentTimeMillis();
				if (then <= now)
					// Wait time has expired.
					throw new SubsystemException("Operation timed out while waiting for the subsystem to change state from " + fromState);
				try {
					// Wait will never be called with zero or a negative
					// argument due to previous check.
					target.wait(then - now);
				}
				catch (InterruptedException e) {
					throw new SubsystemException(e);
				}
			}
		}
	}
}

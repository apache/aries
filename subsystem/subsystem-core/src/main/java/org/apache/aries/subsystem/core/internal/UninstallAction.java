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

import java.util.EnumSet;

import org.osgi.service.subsystem.Subsystem.State;

public class UninstallAction extends AbstractAction {
	public UninstallAction(BasicSubsystem requestor, BasicSubsystem target, boolean disableRootCheck) {
		super(requestor, target, disableRootCheck);
	}
	
	@Override
	public Object run() {
		// Protect against re-entry now that cycles are supported.
		if (!LockingStrategy.set(State.UNINSTALLING, target)) {
			return null;
		}
		try {
			// Acquire the global write lock to prevent all other operations until
			// the installation is complete. There is no need to hold any other locks.
			LockingStrategy.writeLock();
			try {
				checkRoot();
				checkValid();
				State state = target.getState();
				if (EnumSet.of(State.UNINSTALLED).contains(state)) {
					return null;
				}
				if (state.equals(State.ACTIVE)) {
					new StopAction(requestor, target, disableRootCheck).run();
				}
				ResourceUninstaller.newInstance(requestor, target).uninstall();
			}
			finally {
				// Release the global write lock.
				LockingStrategy.writeUnlock();
			}
		}
		finally {
			// Protection against re-entry no longer required.
			LockingStrategy.unset(State.UNINSTALLING, target);
		}
		return null;
	}
}

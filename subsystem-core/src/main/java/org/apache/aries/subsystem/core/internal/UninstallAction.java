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
	public UninstallAction(AriesSubsystem subsystem, boolean disableRootCheck, boolean explicit) {
		super(subsystem, disableRootCheck, explicit);
	}
	
	@Override
	public Object run() {
		checkValid();
		checkRoot();
		State state = subsystem.getState();
		if (EnumSet.of(State.UNINSTALLED).contains(state))
			return null;
		else if (EnumSet.of(State.INSTALL_FAILED, State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING, State.UNINSTALLING).contains(state)) {
			waitForStateChange();
			subsystem.uninstall();
		}
		else if (state.equals(State.ACTIVE)) {
			new StopAction(subsystem, disableRootCheck, explicit).run();
			subsystem.uninstall();
		}
		else
			ResourceUninstaller.newInstance(subsystem, subsystem).uninstall();
		return null;
	}
}

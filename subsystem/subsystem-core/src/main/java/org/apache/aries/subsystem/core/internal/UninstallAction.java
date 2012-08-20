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
	public UninstallAction(AriesSubsystem requestor, AriesSubsystem target, boolean disableRootCheck) {
		super(requestor, target, disableRootCheck);
	}
	
	@Override
	public Object run() {
		checkValid();
		checkRoot();
		State state = target.getState();
		if (EnumSet.of(State.UNINSTALLED).contains(state))
			return null;
		else if (EnumSet.of(State.INSTALL_FAILED, State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING, State.UNINSTALLING).contains(state)) {
			waitForStateChange();
			target.uninstall();
		}
		else if (state.equals(State.ACTIVE)) {
			new StopAction(requestor, target, disableRootCheck).run();
			target.uninstall();
		}
		else
			ResourceUninstaller.newInstance(requestor, target).uninstall();
		return null;
	}
}

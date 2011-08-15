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
package org.apache.aries.subsystem.core.resource;

import java.util.List;

import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.coordinator.Coordination;

public abstract class AbstractRuntimeResource implements RuntimeResource {
	private long useCount;
	
	protected final ResourceListener listener;
	protected final Resource resource;
	protected final AriesSubsystem subsystem;
	
	public AbstractRuntimeResource(Resource resource, ResourceListener listener, AriesSubsystem subsystem) {
		this.resource = resource;
		this.listener = listener;
		this.subsystem = subsystem;
	}
	
	@Override
	public List<Capability> getCapabilities(String namespace) {
		return resource.getCapabilities(namespace);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return resource.getRequirements(namespace);
	}
	
	@Override
	public void install(Coordination coordination) throws Exception {
		synchronized (this) {
			if (useCount > 0)
				return;
		}
		if (listener != null)
			listener.installing(resource);
		doInstall(coordination);
		if (listener != null)
			listener.installed(resource);
		synchronized (this) {
			useCount++;
		}
	}
	
	@Override
	public void start(Coordination coordination) throws Exception {
		synchronized (this) {
			if (useCount > 0)
				return;
		}
		if (listener != null)
			listener.starting(resource);
		doStart(coordination);
		if (listener != null)
			listener.started(resource);
		synchronized (this) {
			useCount++;
		}
	}
	
	@Override
	public void stop(Coordination coordination) throws Exception {
		synchronized (this) {
			if (useCount > 0)
				return;
		}
		if (listener != null)
			listener.stopping(resource);
		doStop(coordination);
		if (listener != null)
			listener.stopped(resource);
		synchronized (this) {
			useCount++;
		}
	}

	@Override
	public void uninstall(Coordination coordination) throws Exception {
		boolean uninstall;
		synchronized (this) {
			if (useCount == 0)
				return;
			uninstall = --useCount == 0;
		}
		if (uninstall) {
			if (listener != null)
				listener.uninstalling(resource);
			doUninstall(coordination);
			if (listener != null)
				listener.uninstalled(resource);
		}
	}
	
	protected abstract void doInstall(Coordination coordination) throws Exception;
	
	protected abstract void doStart(Coordination coordination) throws Exception;
	
	protected abstract void doStop(Coordination coordination) throws Exception;
	
	protected abstract void doUninstall(Coordination coordination) throws Exception;
}

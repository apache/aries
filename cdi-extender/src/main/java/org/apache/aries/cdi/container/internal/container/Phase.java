/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.container;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.framework.Bundle;
import org.osgi.util.promise.Promise;

public abstract class Phase {

	protected final Syncro syncro = new Syncro(true);

	public Phase(ContainerState containerState, Phase next) {
		this.containerState = containerState;
		this.next = Optional.ofNullable(next);
	}

	public final Bundle bundle() {
		return containerState.bundle();
	}

	public abstract Op closeOp();

	public abstract boolean close();

	public final void error(Throwable t) {
		containerState.error(t);
	}

	public abstract boolean open();

	public abstract Op openOp();

	public final <T> Promise<T> submit(Op op, Callable<T> callable) {
		return containerState.submit(op, callable);
	}

	protected final ContainerState containerState;
	protected final Optional<Phase> next;

}

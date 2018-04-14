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

import org.apache.aries.cdi.container.internal.CCR;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.felix.utils.extender.Extension;
import org.osgi.service.log.Logger;

public class CDIBundle extends Phase implements Extension {

	public CDIBundle(CCR ccr, ContainerState containerState, Phase next) {
		super(containerState, next);
		_ccr = ccr;
		_log = containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public boolean close() {
		containerState.closing();

		return next.map(
			next -> {
				submit(next.closeOp(), next::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error in cdibundle CLOSE on {}", bundle(), f));

						error(f);
					}
				);

				_ccr.remove(bundle());

				return true;
			}
		).orElse(true);
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Op.Type.INIT, bundle().toString());
	}

	@Override
	public void destroy() throws Exception {
		submit(closeOp(), this::close).onFailure(
			f -> {
				_log.error(l -> l.error("CCR Error in closing cdi bundle {}", containerState.bundle(), f));
			}
		);
	}

	@Override
	public boolean open() {
		return next.map(
			next -> {
				_ccr.add(containerState.bundle(), containerState);

				submit(next.openOp(), next::open).then(
					null,
					f -> {
						_log.error(l -> l.error("CCR Error in cdibundle OPEN on {}", bundle(), f.getFailure()));

						error(f.getFailure());
					}

				);

				return true;
			}
		).orElse(true);
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Op.Type.INIT, bundle().toString());
	}

	@Override
	public void start() throws Exception {
		submit(openOp(), this::open).onFailure(
			f -> {
				_log.error(l -> l.error("CCR Error in starting cdi bundle {}", containerState.bundle(), f));
			}
		);
	}


	private final CCR _ccr;
	private final Logger _log;

}

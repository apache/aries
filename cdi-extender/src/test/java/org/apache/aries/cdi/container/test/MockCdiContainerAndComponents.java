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

package org.apache.aries.cdi.container.test;

import static org.apache.aries.cdi.container.test.TestUtil.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.ExtensionPhase;
import org.apache.aries.cdi.container.internal.container.Phase;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.util.Logs;

public class MockCdiContainerAndComponents implements AutoCloseable {

	public MockCdiContainerAndComponents(String name, String... beanClasses) throws Exception {
		Map<String, OSGiBean> beans = new HashMap<>();

		for (String className : beanClasses) {
			Class<?> clazz = Class.forName(className);

			beans.put(className, new OSGiBean.Builder(new Logs.Builder(null).build(), clazz).build());
		}

		_beansModel = new BeansModel(beans, Collections.emptyList());

		_containerState = getContainerState(_beansModel);

		_nextPhase = new ExtensionPhase(_containerState, null);

		_nextPhase.open();
	}

	@Override
	public void close() throws Exception {
		_nextPhase.close();
	}

	public ContainerState containerState() {
		return _containerState;
	}

	private final BeansModel _beansModel;
	private ContainerState _containerState;
	private final Phase _nextPhase;

}
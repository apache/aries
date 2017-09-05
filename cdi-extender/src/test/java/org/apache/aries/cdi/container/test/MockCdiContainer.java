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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.ContainerDeployment;
import org.apache.aries.cdi.container.internal.container.ContainerEnvironment;
import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.manager.BeanManagerImpl;
import org.junit.Assert;

public class MockCdiContainer implements AutoCloseable {

	public MockCdiContainer(String name, String... beanClasses) {
		this(name, Collections.emptyList(), beanClasses);
	}

	public MockCdiContainer(String name, List<Metadata<Extension>> extensions, String... beanClasses) {
		_bda = new MockBeanDeploymentArchive(name, beanClasses);

		Deployment deployment = new ContainerDeployment(extensions, _bda);

		WeldBootstrap bootstrap = new WeldBootstrap();

		bootstrap.startExtensions(extensions);
		bootstrap.startContainer(new ContainerEnvironment(), deployment);
		bootstrap.startInitialization();
		bootstrap.deployBeans();
		bootstrap.validateBeans();
		bootstrap.endInitialization();

		_bootstrap = bootstrap;
	}

	@Override
	public void close() {
		_bootstrap.shutdown();
	}

	public Bean<?> getBean(Class<?> clazz) {
		final BeanManagerImpl managerImpl = getBeanManager();

		Set<javax.enterprise.inject.spi.Bean<?>> beans =
			managerImpl.getBeans(clazz, AnyLiteral.INSTANCE);

		Assert.assertFalse(beans.isEmpty());

		return managerImpl.resolve(beans);
	}

	public BeanManagerImpl getBeanManager() {
		if (_beanManagerImpl != null) {
			return _beanManagerImpl;
		}

		return _beanManagerImpl = _bootstrap.getManager(_bda);
	}

	public WeldBootstrap getBootstrap() {
		return _bootstrap;
	}

	private final BeanDeploymentArchive _bda;
	private BeanManagerImpl _beanManagerImpl;
	private final WeldBootstrap _bootstrap;

}
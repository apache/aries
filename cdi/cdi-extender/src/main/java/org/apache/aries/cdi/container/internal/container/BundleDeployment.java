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

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

public class BundleDeployment implements CDI11Deployment {

	public BundleDeployment(Iterable<Metadata<Extension>> extensions, BeanDeploymentArchive beanDeploymentArchive) {
		_extensions = extensions;
		_beanDeploymentArchive = beanDeploymentArchive;

		_beanDeploymentArchives = new ArrayList<BeanDeploymentArchive>();
		_beanDeploymentArchives.add(beanDeploymentArchive);
	}

	@Override
	public BeanDeploymentArchive getBeanDeploymentArchive(Class<?> beanClass) {
		return _beanDeploymentArchive;
	}

	@Override
	public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
		return _beanDeploymentArchives;
	}

	@Override
	public Iterable<Metadata<Extension>> getExtensions() {
		return _extensions;
	}

	@Override
	public ServiceRegistry getServices() {
		return _beanDeploymentArchive.getServices();
	}

	@Override
	public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> aClass) {
		return _beanDeploymentArchive;
	}

	private final BeanDeploymentArchive _beanDeploymentArchive;
	private final Collection<BeanDeploymentArchive> _beanDeploymentArchives;
	private final Iterable<Metadata<Extension>> _extensions;

}
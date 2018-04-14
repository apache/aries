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

import java.util.Collection;
import java.util.Collections;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;

public class ContainerDeploymentArchive
	implements BeanDeploymentArchive {

	public <T extends ResourceLoader & ProxyServices> ContainerDeploymentArchive(
		T loader, String id, Collection<String> beanClassNames, BeansXml beansXml) {

		_id = id;
		_beanClassNames = beanClassNames;
		_beansXml = beansXml;
		_services = new SimpleServiceRegistry();

		if (loader != null) {
			_services.add(ResourceLoader.class, loader);
			_services.add(ProxyServices.class, loader);
		}
	}

	@Override
	public Collection<String> getBeanClasses() {
		return _beanClassNames;
	}

	@Override
	public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
		return Collections.emptyList();
	}

	@Override
	public BeansXml getBeansXml() {
		return _beansXml;
	}

	@Override
	public Collection<EjbDescriptor<?>> getEjbs() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return _id;
	}

	@Override
	public ServiceRegistry getServices() {
		return _services;
	}

	private final Collection<String> _beanClassNames;
	private final BeansXml _beansXml;
	private final String _id;
	private final ServiceRegistry _services;

}
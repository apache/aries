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

import org.apache.aries.cdi.container.internal.loader.BundleResourcesLoader;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class BundleDeploymentArchive implements BeanDeploymentArchive {

	public BundleDeploymentArchive(
		BundleWiring bundleWiring, String id, Collection<String> beanClassNames, BeansXml beansXml,
		Bundle extenderBundle) {

		_id = id;
		_beanClassNames = beanClassNames;
		_beanDeploymentArchives = Collections.emptyList();
		_beansXml = beansXml;
		_ejbs = Collections.emptyList();
		_services = new SimpleServiceRegistry();

		BundleResourcesLoader loader = new BundleResourcesLoader(bundleWiring, extenderBundle);

		_services.add(ResourceLoader.class, loader);
		_services.add(ProxyServices.class, loader);
	}

	@Override
	public Collection<String> getBeanClasses() {
		return _beanClassNames;
	}

	@Override
	public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
		return _beanDeploymentArchives;
	}

	@Override
	public BeansXml getBeansXml() {
		return _beansXml;
	}

	@Override
	public Collection<EjbDescriptor<?>> getEjbs() {
		return _ejbs;
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
	private final Collection<BeanDeploymentArchive> _beanDeploymentArchives;
	private final BeansXml _beansXml;
	private final Collection<EjbDescriptor<?>> _ejbs;
	private final String _id;
	private final ServiceRegistry _services;

}
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

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import org.osgi.service.cdi.annotations.Service;

@ApplicationScoped
public class ServiceExtension implements Extension {

	public ServiceExtension(List<ServiceDeclaration> services) {
		_services = services;
	}

	void processBean(@Observes ProcessBean<?> processBean, BeanManager beanManager) {
		Service service = processBean.getAnnotated().getAnnotation(Service.class);

		if (service == null) {
			return;
		}

		Bean<?> bean = processBean.getBean();
		CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);

		_services.add(new ServiceDeclaration(service, bean, creationalContext));
	}

	private final List<ServiceDeclaration> _services;

}

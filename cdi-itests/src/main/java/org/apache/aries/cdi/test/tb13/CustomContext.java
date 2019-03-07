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

package org.apache.aries.cdi.test.tb13;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContext;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardListener;
import org.osgi.framework.BundleContext;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.http.context.ServletContextHelper;

@ApplicationScoped
@HttpWhiteboardContext(name = "customContext", path = "/")
@HttpWhiteboardListener
@Service({ServletContextHelper.class, ServletContextListener.class})
public class CustomContext extends ServletContextHelper implements ServletContextListener {

	@Inject
	public CustomContext(BundleContext bundleContext) {
		super(bundleContext.getBundle());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
	}

}

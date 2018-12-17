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

package org.apache.aries.cdi.executable;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * The sole purpose of this class is to setup JUL to pipe logs through
 * slf4j and on to logback.
 * <p>
 * This is only usable in the bnd embedded launcher used in bnd generated
 * executable jars, and must be on the {@code -runpath}.
 */
@Header(name = "Embedded-Activator", value = "${@class}")
public class EmbeddedActivator implements BundleActivator {

	/**
	 * If {@code true}, execute the activator {@link
	 * #start(BundleContext)} before framework start.
	 */
	public static final boolean IMMEDIATE= true;

	/**
	 * @param context the framework bundle context
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	/**
	 * This is never actually called by the bnd launcher.
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
	}

}

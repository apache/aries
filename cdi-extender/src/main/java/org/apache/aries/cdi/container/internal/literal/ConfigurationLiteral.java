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

package org.apache.aries.cdi.container.internal.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;

public class ConfigurationLiteral extends AnnotationLiteral<Configuration> implements Configuration {

	public static final Configuration INSTANCE = new ConfigurationLiteral(
		new String[] {Configuration.NAME}, ConfigurationPolicy.OPTIONAL);

	public ConfigurationLiteral(String[] pids, ConfigurationPolicy configurationPolicy) {
		_pids = pids;
		_configurationPolicy = configurationPolicy;
	}

	@Override
	public ConfigurationPolicy configurationPolicy() {
		return _configurationPolicy;
	}

	@Override
	public String[] value() {
		return _pids;
	}

	private static final long serialVersionUID = 1L;

	private final ConfigurationPolicy _configurationPolicy;
	private final String[] _pids;

}

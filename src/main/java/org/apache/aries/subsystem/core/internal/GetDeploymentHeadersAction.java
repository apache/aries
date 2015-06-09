/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.aries.subsystem.core.archive.Header;

public class GetDeploymentHeadersAction implements PrivilegedAction<Map<String, String>> {
	private final BasicSubsystem subsystem;
	
	public GetDeploymentHeadersAction(BasicSubsystem subsystem) {
		this.subsystem = subsystem;
	}
	
	@Override
	public Map<String, String> run() {
		Map<String, Header<?>> headers = subsystem.getDeploymentManifest().getHeaders();
		Map<String, String> result = new HashMap<String, String>(headers.size());
		for (Entry<String, Header<?>> entry: headers.entrySet()) {
			Header<?> value = entry.getValue();
			result.put(entry.getKey(), value.getValue());
		}
		return result;
	}
}

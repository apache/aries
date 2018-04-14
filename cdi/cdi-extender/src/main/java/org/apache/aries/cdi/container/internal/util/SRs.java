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

package org.apache.aries.cdi.container.internal.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class SRs {

	private SRs() {
		// no instances
	}

	public static <T> ServiceReferenceDTO from(ServiceReference<T> reference) {
		for (ServiceReferenceDTO dto : reference.getBundle().adapt(ServiceReferenceDTO[].class)) {
			if (dto.id == id(reference)) {
				return dto;
			}
		}
		return null;
	}

	@SafeVarargs
	public static <T> List<ServiceReferenceDTO> from(ServiceReference<T>[] references, ServiceReference<T>... more) {
		if (references == null) return Arrays.stream(more).sorted().map(
			r -> from(r)
		).collect(Collectors.toList());

		return Stream.concat(Arrays.stream(references), Arrays.stream(more)).sorted().map(
			r -> from(r)
		).collect(Collectors.toList());
	}

	@SafeVarargs
	public static <T> List<ServiceReferenceDTO> from(Collection<ServiceReference<T>> references, ServiceReference<T>... more) {
		return Stream.concat(references.stream(), Arrays.stream(more)).sorted().map(
			r -> from(r)
		).collect(Collectors.toList());
	}

	public static <T> long id(ServiceReference<T> reference) {
		return (Long)reference.getProperty(Constants.SERVICE_ID);
	}

}

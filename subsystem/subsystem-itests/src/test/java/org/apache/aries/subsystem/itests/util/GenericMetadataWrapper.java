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
package org.apache.aries.subsystem.itests.util;

import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;

public class GenericMetadataWrapper {
	private final GenericMetadata metadata;
	
	public GenericMetadataWrapper(GenericMetadata metadata) {
		if (metadata == null)
			throw new NullPointerException();
		this.metadata = metadata;
	}
	
	public GenericMetadata getGenericMetadata() {
		return metadata;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof GenericMetadataWrapper))
			return false;
		GenericMetadataWrapper that = (GenericMetadataWrapper)o;
		return metadata.getNamespace().equals(that.metadata.getNamespace())
				&& metadata.getAttributes().equals(that.metadata.getAttributes())
				&& metadata.getDirectives().equals(that.metadata.getDirectives());
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + metadata.getNamespace().hashCode();
		result = 31 * result + metadata.getAttributes().hashCode();
		result = 31 * result + metadata.getDirectives().hashCode();
		return result;
	}

    @Override
    public String toString() {
        return "GenericMetadata[" +
                "namespace=" + metadata.getNamespace() + ", " +
                "directives=" + metadata.getDirectives() + "," +
                "attributes=" + metadata.getAttributes() + "," +
                "]";
    }
}

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
package org.apache.aries.subsystem.spi;

import org.apache.aries.subsystem.SubsystemException;

/**
 * ResourceConverters are used to tranform resources into another kind of
 * resources.  For example, convert a blueprint xml configuration file
 * into a deployable OSGi bundle
 *
 * Note that converters do not really *enable* anything. The ease the use
 * of the subsystem archives, so we could remove those from the SPI.
 */
public interface ResourceConverter {

    /**
     * Check if the converter can convert a resource into the given type
     * TODO: do we need that or should it be inferred from service properties?
     *
     * @param resource
     * @param targetType
     * @return
     */
    boolean canConvert(Resource resource, String targetType);

    /**
     * Convert a resource into the given type.
     *
     * @param resource
     * @return <code>null</code> if the conversion is not supported
     */
    Resource convert(Resource resource, String targetType) throws SubsystemException;

}

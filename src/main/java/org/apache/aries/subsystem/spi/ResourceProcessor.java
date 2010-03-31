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

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemException;

/**
 * A ResourceProcessor is an object that can manage a given resource type.
 * Processors must be registered in the OSGi registry.
 *
 * The purpose is to actually install a resource.  There would be a processor
 * for OSGi bundles (which may be internal), maybe we can also have a standard
 * one for configurations for the ConfigAdmin.
 */
public interface ResourceProcessor {

    void begin(Subsystem subsystem);

    void process(Resource resource) throws SubsystemException;

    void dropped(Resource resource) throws SubsystemException;

    void prepare() throws SubsystemException;

    void commit();

    void rollback();

}

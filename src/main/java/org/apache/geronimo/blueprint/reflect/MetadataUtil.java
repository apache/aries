/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.blueprint.reflect;

import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;


/**
 * A utility class that handles cloning various polymorphic
 * bits of metadata into concrete class implementations.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class MetadataUtil {

    static public Metadata cloneMetadata(Metadata source) {
        if (source == null) {
            return null;
        } 
        else if (source instanceof MapMetadata) {
            return new MapMetadataImpl((MapMetadata)source);
        }
        else if (source instanceof NullMetadata) {
            return NullMetadata.NULL;
        }
        else if (source instanceof PropsMetadata) {
            return new PropsMetadataImpl((PropsMetadata)source);
        }
        else if (source instanceof RefMetadata) {
            return new RefMetadataImpl((RefMetadata)source);
        }
        else if (source instanceof IdRefMetadata) {
            return new IdRefMetadataImpl((IdRefMetadata)source);
        }
        else if (source instanceof ValueMetadata) {
            return new ValueMetadataImpl((ValueMetadata)source);
        }
        else if (source instanceof BeanMetadata) {
            return new BeanMetadataImpl((BeanMetadata)source);
        }
        else if (source instanceof RefCollectionMetadata) {
            return new RefCollectionMetadataImpl((RefCollectionMetadata)source);
        }
        else if (source instanceof ServiceMetadata) {
            return new ServiceMetadataImpl((ServiceMetadata)source);
        }
        else if (source instanceof ReferenceMetadata) {
            return new ReferenceMetadataImpl((ReferenceMetadata)source);
        }

        throw new RuntimeException("Unknown Metadata type received: " + source.getClass().getName());
    }


    /**
     * Clone a component metadata item, returning a mutable
     * instance.
     *
     * @param source The source metadata item.
     *
     * @return A mutable instance of this metadata item.
     */
    static public ComponentMetadata cloneComponentMetadata(ComponentMetadata source) {
        return (ComponentMetadata) cloneMetadata(source);
    }

    /**
     * Clone a target item, returning a mutable
     * instance.
     *
     * @param source The source target item.
     *
     * @return A mutable instance of this target item.
     */
    static public Target cloneTarget(Target source) {
        return (Target) cloneMetadata(source);
    }

    /**
     * Create a new metadata instance of the given type
     *
     * @param type the class of the Metadata object to create
     * @param <T>
     * @return a new instance
     */
    public static <T extends Metadata> T createMetadata(Class<T> type) {
        if (MapMetadata.class.isAssignableFrom(type)) {
            return type.cast(new MapMetadataImpl());
        } else if (NullMetadata.class.isAssignableFrom(type)) {
            return type.cast(NullMetadata.NULL);
        } else if (PropsMetadata.class.isAssignableFrom(type)) {
            return type.cast(new PropsMetadataImpl());
        } else if (RefMetadata.class.isAssignableFrom(type)) {
            return type.cast(new RefMetadataImpl());
        } else if (IdRefMetadata.class.isAssignableFrom(type)) {
            return type.cast(new IdRefMetadataImpl());
        } else if (ValueMetadata.class.isAssignableFrom(type)) {
            return type.cast(new ValueMetadataImpl());
        } else if (BeanMetadata.class.isAssignableFrom(type)) {
            return type.cast(new BeanMetadataImpl());
        } else if (RefCollectionMetadata.class.isAssignableFrom(type)) {
            return type.cast(new RefCollectionMetadataImpl());
        } else if (ServiceMetadata.class.isAssignableFrom(type)) {
            return type.cast(new ServiceMetadataImpl());
        } else if (ReferenceMetadata.class.isAssignableFrom(type)) {
            return type.cast(new ReferenceMetadataImpl());
        } else {
            throw new IllegalArgumentException("Unsupport metadata type: " + (type != null ? type.getName() : null));
        }
    }
}


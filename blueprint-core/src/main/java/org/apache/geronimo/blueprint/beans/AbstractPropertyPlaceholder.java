/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.blueprint.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.geronimo.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.geronimo.blueprint.mutable.MutableBeanArgument;
import org.apache.geronimo.blueprint.mutable.MutableBeanMetadata;
import org.apache.geronimo.blueprint.mutable.MutableBeanProperty;
import org.apache.geronimo.blueprint.mutable.MutableCollectionMetadata;
import org.apache.geronimo.blueprint.mutable.MutableListener;
import org.apache.geronimo.blueprint.mutable.MutableMapEntry;
import org.apache.geronimo.blueprint.mutable.MutableMapMetadata;
import org.apache.geronimo.blueprint.mutable.MutablePropsMetadata;
import org.apache.geronimo.blueprint.mutable.MutableRefCollectionMetadata;
import org.apache.geronimo.blueprint.mutable.MutableRegistrationListener;
import org.apache.geronimo.blueprint.mutable.MutableServiceMetadata;
import org.apache.geronimo.blueprint.mutable.MutableValueMetadata;
import org.osgi.service.blueprint.context.ComponentDefinitionException;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.Listener;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * Abstract class for property placeholders.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766508 $, $Date: 2009-04-19 22:09:27 +0200 (Sun, 19 Apr 2009) $
 */
public abstract class AbstractPropertyPlaceholder implements ComponentDefinitionRegistryProcessor {

    private String placeholderPrefix = "${";
    private String placeholderSuffix = "}";
    private Pattern pattern;

    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    public void process(ComponentDefinitionRegistry registry) throws ComponentDefinitionException {
        for (String name : registry.getComponentDefinitionNames()) {
            processMetadata(registry.getComponentDefinition(name));
        }
    }

    protected Metadata processMetadata(Metadata metadata) {
        if (metadata instanceof BeanMetadata) {
            return processBeanMetadata((BeanMetadata) metadata);
        } else if (metadata instanceof RefCollectionMetadata) {
            return processRefCollectionMetadata((RefCollectionMetadata) metadata);
        } else if (metadata instanceof ReferenceMetadata) {
            return processReferenceMetadata((ReferenceMetadata) metadata);
        } else if (metadata instanceof ServiceMetadata) {
            return processServiceMetadata((ServiceMetadata) metadata);
        } else if (metadata instanceof CollectionMetadata) {
            return processCollectionMetadata((CollectionMetadata) metadata);
        } else if (metadata instanceof MapMetadata) {
            return processMapMetadata((MapMetadata) metadata);
        } else if (metadata instanceof PropsMetadata) {
            return processPropsMetadata((PropsMetadata) metadata);
        } else if (metadata instanceof ValueMetadata) {
            return processValueMetadata((ValueMetadata) metadata);
        } else {
            return metadata;
        }
    }

    protected Metadata processBeanMetadata(BeanMetadata component) {
        for (BeanArgument arg :  component.getArguments()) {
            ((MutableBeanArgument) arg).setValue(processMetadata(arg.getValue()));
        }
        for (BeanProperty prop : component.getProperties()) {
            ((MutableBeanProperty) prop).setValue(processMetadata(prop.getValue()));
        }
        ((MutableBeanMetadata) component).setFactoryComponent((Target) processMetadata(component.getFactoryComponent()));
        return component;
    }

    protected Metadata processServiceMetadata(ServiceMetadata component) {
        ((MutableServiceMetadata) component).setServiceComponent((Target) processMetadata(component.getServiceComponent()));
        List<MapEntry> entries = new ArrayList<MapEntry>(component.getServiceProperties());
        for (MapEntry entry : entries) {
            ((MutableServiceMetadata) component).removeServiceProperty(entry);
        }
        for (MapEntry entry : processMapEntries(entries)) {
            ((MutableServiceMetadata) component).addServiceProperty(entry);
        }
        for (RegistrationListener listener : component.getRegistrationListeners()) {
            ((MutableRegistrationListener) listener).setListenerComponent((Target) processMetadata(listener.getListenerComponent()));
        }
        return component;
    }

    protected Metadata processReferenceMetadata(ReferenceMetadata component) {
        for (Listener listener : component.getServiceListeners()) {
            ((MutableListener) listener).setListenerComponent((Target) processMetadata(listener.getListenerComponent()));
        }
        return component;
    }

    protected Metadata processRefCollectionMetadata(RefCollectionMetadata component) {
        for (Listener listener : component.getServiceListeners()) {
            ((MutableListener) listener).setListenerComponent((Target) processMetadata(listener.getListenerComponent()));
        }
        ((MutableRefCollectionMetadata) component).setComparator((Target) processMetadata(component.getComparator()));
        return component;
    }

    protected Metadata processPropsMetadata(PropsMetadata metadata) {
        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        for (MapEntry entry : entries) {
            ((MutablePropsMetadata) metadata).removeEntry(entry);
        }
        for (MapEntry entry : processMapEntries(entries)) {
            ((MutablePropsMetadata) metadata).addEntry(entry);
        }
        return metadata;
    }

    protected Metadata processMapMetadata(MapMetadata metadata) {
        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        for (MapEntry entry : entries) {
            ((MutableMapMetadata) metadata).removeEntry(entry);
        }
        for (MapEntry entry : processMapEntries(entries)) {
            ((MutableMapMetadata) metadata).addEntry(entry);
        }
        return metadata;
    }

    protected List<MapEntry> processMapEntries(List<MapEntry> entries) {
        for (MapEntry entry : entries) {
            ((MutableMapEntry) entry).setKey((NonNullMetadata) processMetadata(entry.getKey()));
            ((MutableMapEntry) entry).setValue(processMetadata(entry.getValue()));
        }
        return entries;
    }

    protected Metadata processCollectionMetadata(CollectionMetadata metadata) {
        List<Metadata> values = new ArrayList<Metadata>(metadata.getValues());
        for (Metadata value : values) {
            ((MutableCollectionMetadata) metadata).removeValue(value);
        }
        for (Metadata value : values) {
            ((MutableCollectionMetadata) metadata).addValue(processMetadata(value));
        }
        return metadata;
    }

    protected Metadata processValueMetadata(ValueMetadata metadata) {
        ((MutableValueMetadata) metadata).setStringValue(processString(metadata.getStringValue()));
        return metadata;
    }

    protected String processString(String str) {
        Matcher matcher = getPattern().matcher(str);
        while (matcher.find()) {
            String rep = getProperty(matcher.group(1));
            if (rep != null) {
                str = str.replace(matcher.group(0), rep);
                matcher.reset(str);
            }
        }
        return str;
    }

    protected String getProperty(String val) {
        return null;
    }

    protected Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile("\\Q" + placeholderPrefix + "\\E(.+)\\Q" + placeholderSuffix + "\\E");
        }
        return pattern;
    }

}

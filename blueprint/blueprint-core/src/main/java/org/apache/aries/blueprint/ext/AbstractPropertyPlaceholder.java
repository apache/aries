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
package org.apache.aries.blueprint.ext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanArgument;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanProperty;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableMapEntry;
import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.apache.aries.blueprint.mutable.MutablePropsMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceListener;
import org.apache.aries.blueprint.mutable.MutableRegistrationListener;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceReferenceMetadata;
import org.apache.aries.blueprint.utils.StrLookup;
import org.apache.aries.blueprint.utils.StrSubstitutor;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for property placeholders.
 *
 * @version $Rev$, $Date$
 */
public abstract class AbstractPropertyPlaceholder implements ComponentDefinitionRegistryProcessor {
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPropertyPlaceholder.class);

    private final StrSubstitutor substitutor;

    private String placeholderPrefix;
    private String placeholderSuffix;
    private LinkedList<String> processingStack = new LinkedList<String>();
    private Bundle blueprintBundle;

    public AbstractPropertyPlaceholder() {
        this.placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;
        this.placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

        this.substitutor = new StrSubstitutor();
        this.substitutor.setEnableSubstitutionInVariables(true);
        this.substitutor.setVariablePrefix(this.placeholderPrefix);
        this.substitutor.setVariableSuffix(this.placeholderSuffix);
        this.substitutor.setVariableResolver(new StrLookup<String>() {
            @Override
            public String lookup(String value) {
                String result = retrieveValue(value);
                return result != null ? result : value;
            }
        });
    }

    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
        this.substitutor.setVariablePrefix(this.placeholderPrefix);
    }

    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
        this.substitutor.setVariableSuffix(this.placeholderSuffix);
    }

    public void process(ComponentDefinitionRegistry registry) throws ComponentDefinitionException {
        try {
             blueprintBundle = (Bundle) ((PassThroughMetadata)registry.getComponentDefinition("blueprintBundle")).getObject();
            
            for (String name : registry.getComponentDefinitionNames()) {
                processMetadata(registry.getComponentDefinition(name));
            }
        } finally {
          processingStack.clear();
          blueprintBundle = null;
        }
    }

    protected Metadata processMetadata(Metadata metadata) {
        try {
            if (metadata instanceof BeanMetadata) {
                BeanMetadata bmd = (BeanMetadata) metadata;
                processingStack.add("Bean named " + bmd.getId() + "->");
                return processBeanMetadata(bmd);
            } else if (metadata instanceof ReferenceListMetadata) {
                ReferenceListMetadata rlmd = (ReferenceListMetadata) metadata;
                processingStack.add("Reference List named " + rlmd.getId() + "->");
                return processRefCollectionMetadata(rlmd);
            } else if (metadata instanceof ReferenceMetadata) {
                ReferenceMetadata rmd = (ReferenceMetadata) metadata;
                processingStack.add("Reference named " + rmd.getId() + "->");
                return processReferenceMetadata(rmd);
            } else if (metadata instanceof ServiceMetadata) {
                ServiceMetadata smd = (ServiceMetadata) metadata;
                processingStack.add("Service named " + smd.getId() + "->");
                return processServiceMetadata(smd);
            } else if (metadata instanceof CollectionMetadata) {
                CollectionMetadata cmd = (CollectionMetadata) metadata;
                processingStack.add("Collection of type " + cmd.getCollectionClass() + "->");
                return processCollectionMetadata(cmd);
            } else if (metadata instanceof MapMetadata) {
                processingStack.add("Map->");
                return processMapMetadata((MapMetadata) metadata);
            } else if (metadata instanceof PropsMetadata) {
                processingStack.add("Properties->");
                return processPropsMetadata((PropsMetadata) metadata);
            } else if (metadata instanceof ValueMetadata) {
                processingStack.add("Value->");
                return processValueMetadata((ValueMetadata) metadata);
            } else {
                processingStack.add("Unknown Metadata " + metadata + "->");
                return metadata;
            }
        } finally {
            processingStack.removeLast();
        }
    }

    protected Metadata processBeanMetadata(BeanMetadata component) {
        for (BeanArgument arg :  component.getArguments()) {
            
            try {
                processingStack.add(
                    "Argument index " + arg.getIndex() + " and value type " + arg.getValueType() + "->");
                if(arg instanceof MutableBeanArgument) {
                    ((MutableBeanArgument) arg).setValue(processMetadata(arg.getValue()));
                } else {
                    //Say that we can't change this argument, but continue processing
                    //If the value is mutable then we may be ok!
                    printWarning(arg, "Constructor Argument");
                    processMetadata(arg.getValue());
                }
            } finally {
                processingStack.removeLast();
            }
        }
        for (BeanProperty prop : component.getProperties()) {
          
            try {
                processingStack.add("Property named " + prop.getName() + "->");
                if(prop instanceof MutableBeanProperty) {
                    ((MutableBeanProperty) prop).setValue(processMetadata(prop.getValue()));
                } else {
                    //Say that we can't change this property, but continue processing
                    //If the value is mutable then we may be ok!
                    printWarning(prop, "Injection Property");
                    processMetadata(prop.getValue());
                }
            } finally {
                processingStack.removeLast();
            }
        }
        
        Target factoryComponent = component.getFactoryComponent();
        if(factoryComponent != null) {
            
            try {
                
                if(component instanceof MutableBeanMetadata) {
                    processingStack.add("Factory Component->");
                    ((MutableBeanMetadata) component).setFactoryComponent(
                        (Target) processMetadata(factoryComponent));
                } else {
                    printWarning(component, "Factory Component");
                    processingStack.add("Factory Component->");
                    processMetadata(factoryComponent);
                }
            } finally {
                processingStack.removeLast();
            }
        }
        
        return component;
    }

    protected Metadata processServiceMetadata(ServiceMetadata component) {
      
        try {
            if(component instanceof MutableServiceMetadata) {
                processingStack.add("Service Component->");
                ((MutableServiceMetadata) component).setServiceComponent(
                    (Target) processMetadata(component.getServiceComponent()));
            } else {
                printWarning(component, "Service Component");
                processingStack.add("Service Component->");
                processMetadata(component.getServiceComponent());
            }
        } finally {
            processingStack.removeLast();
        }
        
        List<MapEntry> entries = new ArrayList<MapEntry>(component.getServiceProperties());
        if(!!! entries.isEmpty()) {
          
            try {
                if(component instanceof MutableServiceMetadata) {
                    processingStack.add("Service Properties->");
                    MutableServiceMetadata msm = (MutableServiceMetadata) component;
                
                    for (MapEntry entry : entries) {
                        msm.removeServiceProperty(entry);
                    }
                    for (MapEntry entry : processMapEntries(entries)) {
                        msm.addServiceProperty(entry);
                    }
                } else {
                    printWarning(component, "Service Properties");
                    processingStack.add("Service Properties->");
                    processMapEntries(entries);
                }
            } finally {
              processingStack.removeLast();
            }
        }
        
        for (RegistrationListener listener : component.getRegistrationListeners()) {
            Target listenerComponent = listener.getListenerComponent();
            try {
                processingStack.add("Registration Listener " + listenerComponent + "->");
                if(listener instanceof MutableRegistrationListener) {
                    ((MutableRegistrationListener) listener).setListenerComponent((Target) processMetadata(listenerComponent));
                } else {
                    //Say that we can't change this listener, but continue processing
                    //If the value is mutable then we may be ok!
                    printWarning(listener, "Service Registration Listener");
                    processMetadata(listenerComponent);
                }
            } finally {
            processingStack.removeLast();
            }
        }
        return component;
    }

    protected Metadata processReferenceMetadata(ReferenceMetadata component) {
        return processServiceReferenceMetadata(component);
    }

    protected Metadata processRefCollectionMetadata(ReferenceListMetadata component) {
        return processServiceReferenceMetadata(component);
    }

    private Metadata processServiceReferenceMetadata(ServiceReferenceMetadata component) {
        if (component instanceof MutableServiceReferenceMetadata) {
            ValueMetadata valueMetadata = ((MutableServiceReferenceMetadata) component).getExtendedFilter();
            if (valueMetadata != null) {
                ((MutableServiceReferenceMetadata) component).setExtendedFilter(
                        doProcessValueMetadata(valueMetadata));
            }
        }
        for (ReferenceListener listener : component.getReferenceListeners()) {
            Target listenerComponent = listener.getListenerComponent();
            try {
                processingStack.add("Reference Listener " + listenerComponent + "->");
                if(listener instanceof MutableReferenceListener) {
                    ((MutableReferenceListener) listener).setListenerComponent((Target) processMetadata(listenerComponent));
                } else {
                    //Say that we can't change this listener, but continue processing
                    //If the value is mutable then we may be ok!
                    printWarning(listener, "Reference Binding Listener");
                    processMetadata(listenerComponent);
                }
            } finally {
            processingStack.removeLast();
            }
        }
        return component;
    }
    
    protected Metadata processPropsMetadata(PropsMetadata metadata) {
      
        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        if(!!! entries.isEmpty()) {
          
            try {
                if(metadata instanceof MutablePropsMetadata) {
                    processingStack.add("Properties->");
                    MutablePropsMetadata mpm = (MutablePropsMetadata) metadata;
                
                    for (MapEntry entry : entries) {
                        mpm.removeEntry(entry);
                    }
                    for (MapEntry entry : processMapEntries(entries)) {
                        mpm.addEntry(entry);
                    }
                } else {
                    printWarning(metadata, "Properties");
                    processingStack.add("Properties->");
                    processMapEntries(entries);
                }
            } finally {
              processingStack.removeLast();
            }
        }
        return metadata;
    }

    protected Metadata processMapMetadata(MapMetadata metadata) {
        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        if(!!! entries.isEmpty()) {
          
            try {
                if(metadata instanceof MutableMapMetadata) {
                    processingStack.add("Map->");
                    MutableMapMetadata mmm = (MutableMapMetadata) metadata;
                
                    for (MapEntry entry : entries) {
                        mmm.removeEntry(entry);
                    }
                    for (MapEntry entry : processMapEntries(entries)) {
                        mmm.addEntry(entry);
                    }
                } else {
                    printWarning(metadata, "Map");
                    processingStack.add("Map->");
                    processMapEntries(entries);
                }
            } finally {
              processingStack.removeLast();
            }
        }
        return metadata;
    }

    protected List<MapEntry> processMapEntries(List<MapEntry> entries) {
        for (MapEntry entry : entries) {
            try {
                processingStack.add("Map Entry Key: " + entry.getKey() + " Value: " + entry.getValue() + "->" );
            
                if(entry instanceof MutableMapEntry) {
                    ((MutableMapEntry) entry).setKey((NonNullMetadata) processMetadata(entry.getKey()));
                    ((MutableMapEntry) entry).setValue(processMetadata(entry.getValue()));
                } else {
                  printWarning(entry, "Map Entry");
                  processMetadata(entry.getKey());
                  processMetadata(entry.getValue());
                }
            } finally {
                processingStack.removeLast();
            }
        }
        return entries;
    }

    protected Metadata processCollectionMetadata(CollectionMetadata metadata) {
      
        List<Metadata> values = new ArrayList<Metadata>(metadata.getValues());
        if(!!! values.isEmpty()) {
        
            try {
                if(metadata instanceof MutableCollectionMetadata) {
                    processingStack.add("Collection type: " + metadata.getValueType() + "->");
                    MutableCollectionMetadata mcm = (MutableCollectionMetadata) metadata;
                
                    for (Metadata value : values) {
                        mcm.removeValue(value);
                    }
                    for (Metadata value : values) {
                        mcm.addValue(processMetadata(value));
                    }
                } else {
                    printWarning(metadata, "Collection type: " + metadata.getValueType());
                    processingStack.add("Collection type: " + metadata.getValueType() + "->");
                    for (Metadata value : values) {
                       processMetadata(value);
                    }
                }
            } finally {
              processingStack.removeLast();
            }
        }
        return metadata;
    }

    protected Metadata processValueMetadata(ValueMetadata metadata) {
        return doProcessValueMetadata(metadata);
    }

    protected ValueMetadata doProcessValueMetadata(ValueMetadata metadata) {
        return new LateBindingValueMetadata(substitutor, metadata);
    }

    private void printWarning(Object immutable, String processingType) {
        StringBuilder sb = new StringBuilder("The property placeholder processor for ");
        sb.append(placeholderPrefix).append(',').append(" ").append(placeholderSuffix)
          .append(" in bundle ").append(blueprintBundle.getSymbolicName()).append("/")
          .append(blueprintBundle.getVersion()).append(" found an immutable ").append(processingType)
          .append(" at location ");
        
        for(String s : processingStack) {
            sb.append(s);
        }
        
        sb.append(". This may prevent properties, beans, or other items referenced by this component from being properly processed.");
        
        LOGGER.info(sb.toString());
    }
    
    protected String retrieveValue(String expression) {
        return getProperty(expression);
    }

    protected String getProperty(String val) {
        return null;
    }

    public class LateBindingValueMetadata implements ValueMetadata {

        private final StrSubstitutor substitutor;
        private final ValueMetadata metadata;
        private boolean retrieved;
        private String retrievedValue;

        public LateBindingValueMetadata(StrSubstitutor substitutor, ValueMetadata metadata) {
            this.substitutor = substitutor;
            this.metadata = metadata;
        }

        public String getStringValue() {
            if (!retrieved) {
                String value = metadata.getStringValue();
                StringBuilder result = new StringBuilder(value);
                LOGGER.debug("Before process: {}", result);
                retrieved = this.substitutor.replaceIn(result);
                LOGGER.debug("After process: {}", retrievedValue);
                
                if (retrieved) {
                    retrievedValue = result.toString();
                } else {
                    retrievedValue = value;
                }
            }
            return retrievedValue;
        }

        public String getType() {
            return metadata.getType();
        }
    }
}

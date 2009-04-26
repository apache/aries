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
package org.apache.geronimo.blueprint.compendium;

import java.util.Map;
import java.util.List;
import java.util.Dictionary;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.ComponentDefinitionException;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.Listener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;
import org.apache.geronimo.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.geronimo.blueprint.beans.AbstractPropertyPlaceholder;
import org.apache.geronimo.blueprint.mutable.MutableValueMetadata;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766508 $, $Date: 2009-04-19 22:09:27 +0200 (Sun, 19 Apr 2009) $
 */
public class CompendiumPropertyPlaceholder extends AbstractPropertyPlaceholder {

    private BlueprintContext blueprintContext;
    private ConfigurationAdmin configAdmin; 
    private String persistentId;
    private Map defaultProperties;

    public BlueprintContext getBlueprintContext() {
        return blueprintContext;
    }

    public void setBlueprintContext(BlueprintContext blueprintContext) {
        this.blueprintContext = blueprintContext;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public Map getDefaultProperties() {
        return defaultProperties;
    }

    public void setDefaultProperties(Map defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    @Override
    protected Metadata processValueMetadata(ValueMetadata metadata) {
        return new LateBindingValueMetadata(metadata.getStringValue(), metadata.getTypeName());
    }

    protected String getProperty(String val) {
        Object v = null;
        try {
            Configuration config = configAdmin.getConfiguration(persistentId);
            Dictionary props = config.getProperties();
            if (props != null) {
                v = props.get(val);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            // TODO: log ?
        }
        if (v == null && defaultProperties.containsKey(val)) {
            v = defaultProperties.get(val);
        }
        return v instanceof String ? (String) v : null;
    }

    public class LateBindingValueMetadata implements MutableValueMetadata {

        private String stringValue;
        private String typeName;

        public LateBindingValueMetadata(String stringValue, String typeName) {
            this.stringValue = stringValue;
            this.typeName = typeName;
        }

        public String getStringValue() {
            return processString(stringValue);
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
    }

}

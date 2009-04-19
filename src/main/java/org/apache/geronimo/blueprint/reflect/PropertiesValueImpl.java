/*
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
package org.apache.geronimo.blueprint.reflect;

import java.util.Properties;

import org.osgi.service.blueprint.reflect.PropertiesValue;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class PropertiesValueImpl implements PropertiesValue {

    private Properties propertiesValue;

    public PropertiesValueImpl() {
    }

    public PropertiesValueImpl(Properties propertiesValue) {
        this.propertiesValue = propertiesValue;
    }

    public PropertiesValueImpl(PropertiesValue source) {
        if (source.getPropertiesValue() != null) {
            propertiesValue = new Properties(source.getPropertiesValue());
        }
    }
    
    public Properties getPropertiesValue() {
        return propertiesValue;
    }

    public void setPropertiesValue(Properties propertiesValue) {
        this.propertiesValue = propertiesValue;
    }
}

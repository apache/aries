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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class MethodInjectionMetadataImpl implements MethodInjectionMetadata {

    private String name;
    private List<ParameterSpecification> parameterSpecifications;

    public MethodInjectionMetadataImpl() {
    }

    public MethodInjectionMetadataImpl(String name, List<ParameterSpecification> parameterSpecifications) {
        this.name = name;
        this.parameterSpecifications = parameterSpecifications;
    }

    public MethodInjectionMetadataImpl(MethodInjectionMetadata source) {
        name = source.getName();
        if (source.getParameterSpecifications() != null) {
            parameterSpecifications = new ArrayList<ParameterSpecification>();
            Iterator i = source.getParameterSpecifications().iterator();
            while (i.hasNext()) {
                parameterSpecifications.add(new ParameterSpecificationImpl((ParameterSpecification)i.next()));
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ParameterSpecification> getParameterSpecifications() {
        return Collections.unmodifiableList(parameterSpecifications);
    }

    public void setParameterSpecifications(List<ParameterSpecification> parameterSpecifications) {
        this.parameterSpecifications = parameterSpecifications;
    }
}

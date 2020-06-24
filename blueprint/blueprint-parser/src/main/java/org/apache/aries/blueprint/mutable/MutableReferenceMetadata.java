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
package org.apache.aries.blueprint.mutable;

import java.util.Collection;

import org.apache.aries.blueprint.ExtendedReferenceMetadata;

/**
 * A mutable version of the <code>ReferenceMetadata</code> that allows modifications.
 *
 * @version $Rev$, $Date$
 */
public interface MutableReferenceMetadata extends ExtendedReferenceMetadata, MutableServiceReferenceMetadata {

    void setTimeout(long timeout);

//IC see: https://issues.apache.org/jira/browse/ARIES-577
    void setDefaultBean(String value);
    
    void setProxyChildBeanClasses(Collection<Class<?>> classes);
//IC see: https://issues.apache.org/jira/browse/ARIES-765

    void setExtraInterfaces(Collection<String> interfaces);
//IC see: https://issues.apache.org/jira/browse/ARIES-1141

    void setDamping(int damping);
//IC see: https://issues.apache.org/jira/browse/ARIES-1535
//IC see: https://issues.apache.org/jira/browse/ARIES-1536

    void setLifecycle(int lifecycle);
}

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
package org.apache.aries.jmx.codec;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.osgi.jmx.framework.FrameworkMBean;

public class BatchResolveResult {
    private final boolean success;
    private final Long[] successfulBundles;

    public BatchResolveResult(boolean success, Long[] successfulBundles) {
        this.success = success;
        this.successfulBundles = successfulBundles;
    }

    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(FrameworkMBean.COMPLETED, successfulBundles);
            items.put(FrameworkMBean.SUCCESS, success);
            return new CompositeDataSupport(FrameworkMBean.BATCH_RESOLVE_RESULT_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData", e);
        }
    }
}

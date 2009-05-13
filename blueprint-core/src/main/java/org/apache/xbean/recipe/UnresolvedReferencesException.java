/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.recipe;

import java.util.List;
import java.util.Map;

public class UnresolvedReferencesException extends ConstructionException {
    private final Map<String,List<Reference>> unresolvedRefs;

    public UnresolvedReferencesException(Map<String, List<Reference>> unresolvedRefs) {
        super("Unresolved references to " + unresolvedRefs.keySet());
        this.unresolvedRefs = unresolvedRefs;
    }

    public UnresolvedReferencesException(String message, Map<String, List<Reference>> unresolvedRefs) {
        super(message);
        this.unresolvedRefs = unresolvedRefs;
    }

    public UnresolvedReferencesException(String message, Throwable cause, Map<String, List<Reference>> unresolvedRefs) {
        super(message, cause);
        this.unresolvedRefs = unresolvedRefs;
    }

    public UnresolvedReferencesException(Throwable cause, Map<String, List<Reference>> unresolvedRefs) {
        super("Unresolved references to " + unresolvedRefs.keySet(), cause);
        this.unresolvedRefs = unresolvedRefs;
    }

    public Map<String, List<Reference>> getUnresolvedRefs() {
        return unresolvedRefs;
    }
}

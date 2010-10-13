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
package org.apache.aries.subsystem.scope.impl;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.internal.Activator;

public class ScopeAdminImpl implements ScopeAdmin {

    private ScopeImpl parentScope;
    private ScopeImpl scope;
    
    public ScopeAdminImpl(ScopeImpl parentScope, ScopeImpl scope) {
        this.parentScope = parentScope;
        this.scope = scope;
    }
    
    public Scope getParentScope() {
        return this.parentScope;
    }

    public Scope getScope() {
        return this.scope;
    }
 
    public ScopeUpdate newScopeUpdate() {
        return new ScopeUpdateImpl(this.scope, Activator.getBundleContext());
    }

}

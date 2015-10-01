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
package org.apache.aries.subsystem.core.archive;

import java.util.Collection;
import java.util.Collections;

public class AriesSubsystemLocationHeader implements Header<Clause> {
	public static final String NAME = "AriesSubsystem-Location";
	
	private final String value;

	public AriesSubsystemLocationHeader(String value) {
		if (value == null) {
			throw new NullPointerException();
		}
		this.value = value;
	}

	@Override
	public Collection<Clause> getClauses() {
		return Collections.<Clause>singleton(
				new Clause() {
					@Override
					public Attribute getAttribute(String name) {
						return null;
					}

					@Override
					public Collection<Attribute> getAttributes() {
						return Collections.emptyList();
					}

					@Override
					public Directive getDirective(String name) {
						return null;
					}

					@Override
					public Collection<Directive> getDirectives() {
						return Collections.emptyList();
					}

					@Override
					public Parameter getParameter(String name) {
						return null;
					}

					@Override
					public Collection<Parameter> getParameters() {
						return Collections.emptyList();
					}

					@Override
					public String getPath() {
						return value;
					}
				});
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}

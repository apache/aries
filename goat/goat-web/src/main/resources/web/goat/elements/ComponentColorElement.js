/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
// dojo.provide allows pages use all types declared in this resource
dojo.provide("goat.elements.ComponentColorElement");

dojo.require("goat.configuration.Theme");
dojo.require("goat.configuration.ComponentAppearance");

dojo
		.declare("goat.elements.ComponentColorElement",
				[ goat.elements.ElementBase ], {

					value : null,
					componentAppearance: null,

					constructor : function(component, componentAppearance, /* String */type, /* String */
							value) {
						this.component = component;
						this.componentAppearance = componentAppearance;
						this.value = value;
						this.type = type;
						this.hint = "none";
						console.log("on construction is " + this.value)
			},
					getWidth : function() {
						return 0;
					},
					getHeight : function() {
						return 0;
					},
					render : function() {
						// No special rendering action needed
					},
					apply : function() {
						if (this.type == "component.property.State") {
							if (this.componentAppearance.greyOutInactiveBundles()) {
								if (this.value != "ACTIVE") {
									this.componentAppearance
											.setBackgroundColor("#808080");
								}
							}
						}

					},
					update : function(value) {
						this.value = value;
					},
					remove : function() {
						// no op, we only exist due to the color of the owning
						// component..
				}

				});

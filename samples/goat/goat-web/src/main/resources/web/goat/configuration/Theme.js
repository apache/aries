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
dojo.provide("goat.configuration.Theme");

/*
 * Still TODO: Read in the configuration from a flat file on the server side
 * Read in the configuration from a cookie
 * Provide a GUI for setting the theme aspects
 * Provide pre-canned themes for users to choose from
 */
dojo.declare("goat.configuration.Theme", [], {

	// object properties
	greyOutInactiveBundles : null,
	useLinearShading : null,
	showState : null,
	showVersion : null,
	bundleBackgroundColor: "#ffff80",
	bundleInactiveBackgroundColor : "#808080",
	bundleBackgroundContrastColor: "#ffffff",
	bundleOutlineColor0: "#808080",
	bundleOutlineColor1: "#BA98E2",
	bundleOutlineColor2: "#682DAE",
	serviceBackgroundColor: "#FFFF33",

	constructor : function() {
		this.greyOutInactiveBundles = true;
		this.useLinearShading = true;
		this.showState = true;
		this.showVersion = true;
	},
	shouldGreyOutInactiveBundles: function() {
		return this.greyOutInactiveBundles;
	},
	shouldUseLinearShading: function() {
		return this.useLinearShading;
	},
	getBundleBackgroundColor: function() {
		return this.bundleBackgroundColor;
	},
	getBundleInactiveBackgroundColor: function() {
		return this.bundleInactiveBackgroundColor;
	},
	getBundleBackgroundContrastColor: function() {
		return this.bundleBackgroundContrastColor;
	},
	getServiceBackgroundColor: function() {
		return this.serviceBackgroundColor;
	},
	getBundleOutlineColor0: function() {
		return this.bundleOutlineColor0;
	},
	getBundleOutlineColor1: function() {
		return this.bundleOutlineColor1;
	},
	getBundleOutlineColor2: function() {
		return this.bundleOutlineColor2;
	},
	getTriangleSize: function() {
		return 20;
	}
	

});

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
dojo.provide("goat.configuration.ComponentAppearance");

dojo.require("goat.configuration.Theme");

dojo.declare("goat.configuration.ComponentAppearance", [], {

	parentAppearance : null,

	theme : null,

	fontFamily : null,
	fontStretch : null,
	textFill : null,
	lineStyle : null,
	lineColour : null,
	lineWidth : null,
	backgroundColour : null,

	constructor : function(theme, parentAppearance) {
		this.theme = theme;
		this.parentAppearance = parentAppearance;
	},
	getBackgroundColor : function() {
		if (this.backgroundColor != null) {
			return this.backgroundColor;
		} else if (this.parentAppearance != null) {
			return parentAppearance.getBackgroundColor();
		} else {
			return this.theme.getBundleBackgroundColor();
		}
	},
	setBackgroundColor : function(backgroundColor) {
		this.backgroundColor = backgroundColor;
	},

	useLinearShading : function() {
		return this.theme.shouldUseLinearShading();

	},
	greyOutInactiveBundles : function() {
		return this.theme.shouldGreyOutInactiveBundles();
	},
});

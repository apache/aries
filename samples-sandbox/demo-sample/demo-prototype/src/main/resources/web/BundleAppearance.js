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
dojo.provide("org.apache.aries.samples.demo.prototype.BundleAppearance");
dojo.require("org.apache.aries.samples.demo.prototype.Preferences");

/* A little utility class for storing variables relating to the appearance
 * of a bundle.
 */
dojo.declare("org.apache.aries.samples.demo.prototype.BundleAppearance", null, {	
	preferences: null,
	
	fontFamily: null,
	fontStretch: null,
	textFill: null,
	lineStyle: null,
	lineColour: null,
	lineWidth: null, 
	backgroundColour: null,

	constructor: function(state, preferences) {
		this.preferences=preferences;
		this.update(state);
	},
	update: function(state) {
		this.fontFamily="Times";
		this.fontStretch="condensed";
		
		this.lineWidth=2;
		
		this.lineColour='#808080';
		if (this.preferences.useColorsForState){
			// Grey out bundles which aren't active
			if (state=='ACTIVE')
			{
				this.textFill='black';
				this.backgroundColour="#ffff80";
			} else 
			{
				this.textFill='#808080';
				// Use a greyer version of the bundle yellow
				this.backgroundColour="#f0f090";
			}
			// Use dotted lines for bundles which are only installed
			if (state=='INSTALLED')
			{
				this.lineStyle="Dash";
			} else 
			{
				this.lineStyle='none';
			}


		} else 
		{
			this.lineColour='black';	
			this.textFill='black';
			this.lineStyle='none';
			this.backgroundColour="#ffff80";
		}
	}
});
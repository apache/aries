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
dojo.provide("goat.elements.TriangleDecorator");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.Moveable");
dojo.require("goat.Component");

dojo.require("goat.configuration.Theme");

// Triangle decorator
// represents a triangle decoration for a line (usually representing a service).

dojo.declare("goat.elements.TriangleDecorator", [], {

	// relationship properties.
	fromComponent : null,
	toComponent : null,
	name : null,
	type : null,

	// object properties
	surface : null,
	typeOffset : 0,

	// gfx objects
	line : null,

	// internals
	stroke : null,
	theme : null,

	// am I deleted?
	removed : false,

	// for the up and coming relationship aspect info..
	aspects : null,

	constructor : function(theme) {
		this.theme = theme;
	},
	makeInvisible : function() {
		this.triangle.setShape( {
			x1 : -1000,
			y1 : -1000,
			x2 : -1000,
			y2 : -1000
		});
	},
	setSurface : function(newSurface) {
		this.surface = newSurface;
	},
	setStroke : function(newStroke) {
		this.stroke = newStroke;
	},
	lineUpdated : function(line) {
		if (this.removed) {
			// console.log("ul EEK.. this line should be dead.. and its
			// aliiiiiive "+this.type+" from "+this.fromComponent.id+" to
			// "+this.toComponent.id);
			// console.log(this);
		}

		if (line != null) {
			var shape = line.getShape();
			var fromx = shape.x1;
			var fromy = shape.y1;
			var tox = shape.x2;
			var toy = shape.y2;

			// A somewhat awkwardly named method ...
			var triangleSize = this.theme.getTriangleSize();
			var deltax = tox - fromx;
			var deltay = toy - fromy;
			// Do a square root to work out the line length
			// Will this hurt us on performance? An approximation would do
			// if so ...
			var lineLength = Math.sqrt(deltax * deltax + deltay * deltay);
			// Assume the triangles are equilateral
			var divider = lineLength / triangleSize;
			// The triangle starts in the middle of the line
			var tx1 = (fromx + tox) / 2;
			var ty1 = (fromy + toy) / 2;
			var tx2 = tx1 - deltax / divider + deltay / divider;
			var ty2 = ty1 - deltay / divider - deltax / divider;
			var tx3 = tx1 - deltax / divider - deltay / divider;
			var ty3 = ty1 - deltay / divider + deltax / divider;

			if (this.triangle == null) {
				this.triangle = this.surface.createPolyline( [ {
					x : tx1,
					y : ty1
				}, {
					x : tx2,
					y : ty2
				}, {
					x : tx3,
					y : ty3
				}, {
					x : tx1,
					y : ty1
				} ]);
				this.triangle.setStroke(this.stroke);

			} else {
				this.triangle.setShape( [ {
					x : tx1,
					y : ty1
				}, {
					x : tx2,
					y : ty2
				}, {
					x : tx3,
					y : ty3
				}, {
					x : tx1,
					y : ty1
				} ]);

				if (this.theme.shouldUseLinearShading()) {
					this.triangle.setFill( {
						type : "linear",
						x1 : tx1,
						y1 : ty1,
						x2 : tx2,
						y2 : ty2,
						colors : [ {
							offset : 0,
							color : this.theme.getServiceBackgroundColor()
						}, {
							offset : 1,
							color : "white"
						} ]
					});
				} else {
					this.triangle.setFill(this.theme
							.getServiceBackgroundColor());

				}
			}
		}
	},
	removeSelf : function() {
		if (!this.removed) {
			this.removed = true;

			this.surface.remove(this.triangle);
			// console.log("Line from "+this.fromComponent.id+" to
			// "+this.toComponent.id+" being marked as deleted");
		}
	},
	getKey : function() {
		var key = "" + this.fromComponent.id + "!" + this.toComponent.id + "!"
				+ this.type + "!" + this.name;
	},
	onComponentMove : function(component) {
		this.updateLine();
	},
	onComponentHidden : function(component) {
		this.updateVisibility();
	},
	onComponentClick : function(component) {
		if (this.removed) {
			// console.log("occ EEK.. this line should be dead.. and its
	// aliiiiiive "+this.type+" from "+this.fromComponent.id+" to
	// "+this.toComponent.id);
}

dojox.gfx.fx.animateStroke( {
	shape : this.triangle,
	duration : 500,
	color : {
		start : "#FF3030",
		end : this.stroke
	},
	width : {
		start : 3,
		end : 2
	},
	join : {
		values : [ "miter", "bevel", "round" ]
	}
}).play();
}
});

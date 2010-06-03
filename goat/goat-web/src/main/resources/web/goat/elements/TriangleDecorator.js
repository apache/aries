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

	// object properties
	surface : null,

	// gfx objects
	line : null,
    triangle : null,
    trianglegroup : null,

	// internals
	stroke : null,
	theme : null,

	// am I deleted?
	removed : false,

    // am I hidden?
    hidden : false,

	constructor : function(theme, surface) {
		this.theme = theme;
        this.surface = surface;
        this.trianglegroup = this.surface.createGroup();
	},
	makeInvisible : function() {
        //console.log("Hiding triangle..");
        if(this.trianglegroup!=null) {
            this.surface.remove(this.trianglegroup);
            this.hidden=true;
        }
   	},
   	setStroke : function(newStroke) {
		this.stroke = newStroke;
	},
	lineUpdated : function(line) {
		//if (this.removed) {
			// console.log("ul EEK.. this line should be dead.. and its
			// aliiiiiive "+this.type+" from "+this.fromComponent.id+" to
			// "+this.toComponent.id);
			// console.log(this);
		//}

        if(this.hidden) {
            //console.log("Unhiding triangle..");
            this.hidden=false;
            this.surface.add(this.trianglegroup);
        }

		if (line != null) {
			var shape = line.getShape();
			var fromx = shape.x1;
			var fromy = shape.y1;
			var tox = shape.x2;
			var toy = shape.y2;

            //avoid processing self-referential relationships
            //not ideal, but serves the purpose until it's done differently.
            if(fromx!=tox && fromy!=toy) {
    
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
    				this.triangle = this.trianglegroup.createPolyline( [ {
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
                }
    
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
            if(this.triangle!=null) {
                this.surface.remove(this.trianglegroup);
                this.trianglegroup=null;
                // console.log("Line from "+this.fromComponent.id+" to
                // "+this.toComponent.id+" being marked as deleted");
            }
		}
	}
});

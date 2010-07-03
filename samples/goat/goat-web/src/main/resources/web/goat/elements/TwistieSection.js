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
// TwistieSection is responsible for creating and renedring the twistie element. It draws the trinagle at 
// x,y coordinate supplied by the owning component. It diplays lines of text derived from a 
// relationship aggregator if the twistie is open and removes them from display when the twistie
// is closed.
dojo.provide("goat.elements.TwistieSection");
dojo.require("goat.elements.ElementBase");
dojo.require("goat.RelationshipAggregator");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.Moveable");
dojo.require("dojo.data.ItemFileWriteStore");


dojo.declare("goat.elements.TwistieSection", goat.elements.ElementBase, {

		/*goat.RelationshipAggregator*/ content: null,
		group: null,
		addsub: null,
		removesub: null,
		
		built: false,
		
		constructor : function(component, /*String*/ type, /*goat.RelationshipAggregator*/ content) {
	        
	        this.component=component;
			this.type=type;
			this.content=content;
			this.x = 0;
			this.y = 0;
			this.hint="row";
			this.width=150;
			this.height=12;
			this.isOpen=false;
			//maintains a list of relations ships to displsy - with dups removed.
			this.items = new Array();
			this.itemTexts = new Array();

			this.name = this.type.substring("relationship.aggregation.".length, this.type.length);

			this.twistieGroup = this.component.group.createGroup();
			this.component.group.remove(this.twistieGroup);
    		this.twistieHandleGroup = this.twistieGroup.createGroup();
	
			this.twistieHandleGroup.connect("onclick",dojo.hitch(this,"twistieHandler"));
			
			this.addsub=dojo.subscribe("goat.relationshipaggregator.add."+this.component.id+"."+this.type, this, this.onAdd);
			this.removesub=dojo.subscribe("goat.relationshipaggregator.remove."+this.component.id+"."+this.type, this, this.onRemove);		
		},
		getContent : function(){
			return this.content;
		},
		getWidth : function(){
			return this.width;
		},
		getHeight: function(){
			return this.height;
		},
		render: function(){
			this.component.group.remove(this.twistieGroup);
			if(this.built) {
				this.twistieHandleGroup.remove(this.twistieHandle);
				this.twistieGroup.remove(this.twistieText);
			}

			//Create here because the component has reset this section's x, y cordinates.
			this.createTwistie();
			this.createText();

			// This may not be the secionn that has asked for a component refresh. If is is not and it is open
			// we need to redraw as the section may have moved as a result of something else above it 
			// being closed. If this IS the the section that requested the resize we shoudl ne need to re-add, but do anyway. 
			if(this.isOpen) {
				this.addItemsToDisplay();
			}
			this.component.group.add(this.twistieGroup);
		},
		update: function(value){
		},
		remove: function(value){
			this.component.group.remove(this.twistieGroup);
			dojo.unsubscribe(this.addsub);
			dojo.unsubscribe(this.removesub);

		},		
		onAdd: function(/*RelationshipElement[]*/args){
			if(!this.inArray(this.items, args.name)) {
					this.items.push(args.name);
			}
			if (!this.built) {
				this.component.refresh();
				this.built = true;
			}
			if (this.isOpen) {
				this.addItemsToDisplay();
				this.component.refresh();
			}

		}, 
		onRemove: function(/*RelationshipElement[]*/args){
			for(var k in this.items) {
				if(this.items[k] == args.nam) {
					delete this.items[k];
				}
			}
			if(this.isOpen) {
				this.addItemsToDisplay;
				this.component.refresh();
			}
		}, 
		addItemsToDisplay: function(){
    		var maxLengthSeen = 0;
    		var extraHeight = 12;
    		if(this.items!=null){
        		extraHeight = extraHeight + (this.items.length * 10);
        		var pyt = this.y + 18;
        		var pxt = this.x + 17;
        		var idx=0;

        		this.removeItemsFromDisplay();
        		for(idx=0; idx<this.items.length; idx++){
            		if(this.isOpen){

                		this.itemTexts[idx] = this.twistieGroup.createText({x: pxt, y: pyt, text: this.items[idx], align: "start"})
                		.setFont({family: "times", size: "8pt"})
                		.setFill("#000000");

                		pyt = pyt + 10;
                		if(maxLengthSeen< this.items[idx].length){
                    		maxLengthSeen = this.items[idx].length;
                		}
            		}
        		}
    		}

    		var extraWidth = this.component.minWidth;
    		if(maxLengthSeen>25){
        		var extraW = maxLengthSeen-25;
        		extraW = extraW * 6;
        		extraWidth = extraWidth + extraW;
    		}

    		if(extraWidth<this.width){
        		extraWidth = this.component.width;
    		}

    		this.height = extraHeight;
    		this.width = extraWidth;
		},

		removeItemsFromDisplay: function(){
    		if(this.itemTexts!=null){
				for (var k in this.itemTexts) {
					this.twistieGroup.remove(this.itemTexts[k]);
    			}
    			this.height = 12;
    			this.width = 150;
			}
		},
		twistieHandler: function() {
    		this.isOpen=!this.isOpen;

    		if(this.isOpen){
            	this.addItemsToDisplay();
    		}else{
        		this.removeItemsFromDisplay();
    		}

    		this.component.refresh();
		},
		createText: function(){
    		var textOffset = this.y + 10;
    		var x = this.x + 17;
    		this.twistieText = this.twistieGroup.createText({x: x, y: textOffset, text: this.name, align: "start"})
    		.setFont({family: "times", size: "8pt"})
    		.setFill("#000000");
		},

		createTwistie: function(){
    		var pys = this.y;
    		var pym = pys+5;
    		var pye = pys+10;
    		var px = this.x + 5;
    		var pxm = this.x + 10;
    		var pxe = this.x + 15;
    		this.twistieHandle = this.twistieHandleGroup.createPolyline([{x:px,y:pys},{x:pxe,y:pym},{x:px,y:pye}]).setFill("#000000");

    		if(this.isOpen){
        		this.twistieHandle.setShape([{x:px,y:pys},{x:pxe,y:pys},{x:pxm,y:pye}]);
    		}
		},
		inArray: function(a, string) {
			for (var k in a) {
				if(a[k] == string) {
					return true;
				}
			}
			return false;
		}
	


});

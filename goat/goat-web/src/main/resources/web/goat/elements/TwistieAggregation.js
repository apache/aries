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
// dojo.provide allows pages use all types declared in this resource
dojo.provide("goat.elements.TwistieAggregation");
dojo.require("goat.elements.ElementBase");
dojo.require("goat.RelationshipAggregator");
dojo.require("goat.TwistieSection");

/*
 * this class works .. but isnt very smart.. it's using the old twistie section code, 
 * which has no way of knowing when it's item list changed inside it.. 
 * 
 * to handle this.. this bit of code merely discards & rebuilds the twistie every time
 * a list change occurs.. this is a little bit overkill.. =)
 */

dojo.declare("goat.elements.TwistieAggregation", goat.elements.ElementBase, {

		/*goat.RelationshipAggregator*/ content: null,
		/*goat.TwistieSection*/ twistie: null,
		group: null,
		addsub: null,
		removesub: null,
		
		built: false,
		
		constructor : function(component, /*String*/ type, /*goat.RelationshipAggregator*/ content) {
	        //console.log("Building TwistieAggregation element");
	        
	        this.component=component;
			this.type=type;
			this.content=content;
			this.hint="row";
			
			
			
			this.group=this.component.group.createGroup();
			this.component.group.remove(this.group);
			
			this.buildTwistie();
			
			//console.log("-Subscribing to.. '"+"goat.relationshipaggregator.add."+this.component.id+"."+this.type+"'");
			//console.log("-Subscribing to.. '"+"goat.relationshipaggregator.remove."+this.component.id+"."+this.type+"'");
			
			this.addsub=dojo.subscribe("goat.relationshipaggregator.add."+this.component.id+"."+this.type, this, this.onAdd);
			this.removesub=dojo.subscribe("goat.relationshipaggregator.remove."+this.component.id+"."+this.type, this, this.onRemove);		
		},
		getContent : function(){
			return this.content;
		},
		getWidth : function(){
			//console.log("Aggregation Element "+this.type+" for "+this.component.id+" returning a width of "+this.twistie.width);
			return this.twistie.width;
		},
		getHeight: function(){
			//console.log("Aggregation Element "+this.type+" for "+this.component.id+" returning a height of "+this.twistie.height);
			return this.twistie.height;
		},
		render: function(){
			if(!this.built){
				this.built=true;
				this.component.group.add(this.group);
			}
			
			//console.log("Setting twistie position to .. "+this.x+" "+this.y);
			this.twistie.updatePosition(this.x,this.y);			
		},
		update: function(value){
			//no-op.. aggregators pull their values from the live content object.
		},
		remove: function(value){
			//console.log("Removing aggregation "+this.type+" from display of component")
			this.component.group.remove(this.group);
			dojo.unsubscribe(this.addsub);
			dojo.unsubscribe(this.removesub);

		},		
		onAdd: function(/*RelationshipElement[]*/args){
		//console.log("I think I'm adding to this object ", this.component);
			//console.log("Aggregation Element notified of Relationship being added");
			//console.log(args[0]);
			this.buildTwistie();
			//notify parent we have altered our size.. 
		}, 
		onRemove: function(/*RelationshipElement[]*/args){
			//console.log("Aggregation Element notified of Relationship being removed");
			//console.log(args[0]);
			this.buildTwistie();
			//notify parent we have altered our size.. 
		}, 
		buildTwistie: function(){
			//flush the old twistie.. if we had one!
			if(this.group!=null){
				//console.log("Flushing old twistie");
				this.component.group.remove(this.group);
				this.group=this.component.group.createGroup();
			}
			
			var _this=this;
			function getItemsCallBack(){
				//filter out (or blend) duplicates.. 
				//will become more important when we need to connect various stuff to different anchors
				var dupeSet = new Object();
				_this.twistie.items=new Array();
				for( var relationshipKey in _this.content.relationships ){
					var /*goat.elements.RelationshipElement*/ relationship = _this.content.relationships[relationshipKey];
					if(dupeSet[relationship.name]==null){
						dupeSet[relationship.name]=relationship;
						_this.twistie.items.push(relationship.name);
					}
				}
				dupeSet=null;
				
				_this.twistie.addItemsToDisplay();
			}	
			var twistieName = this.type.substring("relationship.aggregation.".length, this.type.length);
			this.twistie = new goat.TwistieSection(twistieName, this.group, this.component, this.x, this.y, getItemsCallBack);
			
			//console.log("requesting component refresh as rebuilt twistie may not have same dimensions");
			this.component.refresh();
		}		
});

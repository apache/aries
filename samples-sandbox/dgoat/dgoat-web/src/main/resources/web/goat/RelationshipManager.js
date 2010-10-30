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
//dojo.provide allows pages use all types declared in this resource
dojo.provide("goat.RelationshipManager");
dojo.require("goat.RelationshipAggregator");
dojo.require("goat.Component");
dojo.require("goat.elements.ElementBase");
dojo.require("goat.elements.ElementRegistry");

dojo.declare("goat.RelationshipManager", [], {

	               /*goat.Component*/ owningComponent: null,
	/*goat.RelationshipAggregator[]*/   relationships: null,
	createSub:null,
	removeSub:null,

constructor : function(/*goat.Component*/owningComponent) {
	this.owningComponent = owningComponent;
	this.relationships = new Array();
	
	this.createSub = dojo.subscribe("goat.relationship.create."+owningComponent.id, this, this.registerRelationship);
	this.removeSub = dojo.subscribe("goat.relationship.remove."+owningComponent.id, this, this.removeRelationship);
},
registerRelationship: function(/*goat.elements.RelationshipElement*/relationship){	
	//console.log(">registerRelationship");
	//console.log(relationship);
	var aggregator = this.relationships[relationship.type];
	//console.log("aggregator..b4");
	//console.log(aggregator);
	if(aggregator==null){
		//console.log("agg was null");
		aggregator = new goat.RelationshipAggregator(this.owningComponent, relationship.type);
		//console.log("adding aggregator");
		this.relationships[relationship.type] = aggregator;
	}
	//console.log("aggregator..after");
	//console.log(aggregator);
	//console.log("adding to aggregator");
	aggregator.add(relationship);	
	//console.log("<registerRelationship");
},
removeRelationship: function(/*goat.elements.RelationshipElement*/relationship){
	//console.log("Z: RelationshipManager "+this.owningComponent.id+" handling removal for relationship..");
	//console.log(relationship);
	
	var aggregator = this.relationships[relationship.type];
	aggregator.remove(relationship);
	
	//was this the last relationship we knew about of this type?
	//console.log("Checking if removal results in empty aggregator..");
	var count=0;
	for (var key in aggregator.relationships){
		count++;
	}
	//console.log("Aggregator was managing "+count+" "+aggregator.relationships);
	if(count==0){
		//console.log("Last relationship found, removing.."+relationship.type);
	
		//remove from component..
	    if(this.owningComponent.elements["relationship.aggregation."+relationship.type] !=null ){
	    	
	    	//console.log("Found relationship's renderer inside the component.. issuing remove to it");	    	
	    	this.owningComponent.elements["relationship.aggregation."+relationship.type].removeSelf();
	    	
	    	//console.log("Deleting it");
			delete this.owningComponent.elements["relationship.aggregation."+relationship.type];
		}

	    //console.log("Z: Realtionship manager: Forgetting about the aggregator");
		delete this.relationships[relationship.type];
	}
},
removeSelf: function(){
	dojo.unsubscribe(this.createSub);
	dojo.unsubscribe(this.removeSub);
}

});

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
dojo.provide("goat.RelationshipAggregator");
dojo.require("goat.Relationship");

dojo.declare("goat.RelationshipAggregator", [], {

		type: null,
		relationships: null,
		owningComponent: null,
		componentAppearance: null,
		
		constructor : function(/*goat.Component*/owningComponent, type) {
			this.type=type;
			this.relationships=new Object();
			this.owningComponent = owningComponent;
			this.componentAppearance = owningComponent.getComponentAppearance();
			
			//add this aggregation to the component.
			var property = this.owningComponent.elementRegistry.getProperty(this.owningComponent, this.componentAppearance, "relationship.aggregation."+this.type, this);
			this.owningComponent.elements["relationship.aggregation."+this.type]=property;
		},
		add: function(/*goat.elements.RelationshipElement*/relationship){
			var key = this.getKeyForRelationship(relationship);
			this.relationships[key] = relationship;
			//console.log("Aggregator annoucing add to '"+"goat.relationshipaggregator.add."+this.owningComponent.id+"."+"relationship.aggregation."+this.type+"'");
			dojo.publish("goat.relationshipaggregator.add."+this.owningComponent.id+"."+"relationship.aggregation."+this.type, [relationship]);
		},
		remove: function(/*goat.elements.RelationshipElement*/relationship){
			//console.log("RelationshipAggregator handling remove for..");
			//console.log(relationship);
			var key = this.getKeyForRelationship(relationship);
			//console.log("RelationshipAggregator handling remove for "+key);
			dojo.publish("goat.relationshipaggregator.remove."+this.owningComponent.id+"."+"relationship.aggregation."+this.type, [relationship]);
			delete this.relationships[key];
		},
		getKeyForRelationship: function(/*goat.elements.RelationshipElement*/relationship){
			var key = ""+relationship.fromComponent.id+"!"+relationship.toComponent.id+"!"+relationship.name;
			return key;
		}
			
	});

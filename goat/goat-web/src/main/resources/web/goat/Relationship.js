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
dojo.provide("goat.Relationship");
dojo.require("goat.elements.RelationshipElement");
dojo.require("goat.elements.TriangleDecorator");

/* Relationship represents a relationship which is provided by one component but may be
 * consumed by many. For example a package which is exported by component (bundle) A
 * may be imported by components B, C, D...etc. Each two way relationship is represented
 * by a RelationshipElement, for example A-B, A-C. Relationship maintains a list of 
 * RelationshipElements.
 */

dojo.declare("goat.Relationship", [], {

	key: null,
	sscRelationship: null,
	relationshipElements: null,
	theme: null,

constructor : function(sscRelationship, theme) {
	this.sscRelationship = sscRelationship;
	this.relationshipElements=new Array();
	this.theme = theme;
	this.subs = new Array();

	//Subscribe to providing component deletion
	this.providerSubs = dojo.subscribe("goat.component.delete." + this.sscRelationship.providedBy.id, this, this.removeSelf); 
},
getKey : function(){
	if(this.key==null){
		this.key="!"+this.sscRelationship.type+"!"+this.sscRelationship.name;
	}
	return this.key;
},
update : function(sscRelationship){

    //remove the old relationship elements .. 
    for(var index in this.relationshipElements) {
		this.relationshipElements[index].removeSelf();
	}

	this.relationshipElements=new Array();
	
	this.sscRelationship = sscRelationship;
	
	this.activate();
},
removeElement : function(component) {
	this.relationshipElements[component.id].removeSelf(); 
	delete this.relationshipElements[component.id]; 
	dojo.unsubscribe(this.subs[component.id]);
},
removeSelf : function() {
	for(var index in this.relationshipElements) {
		this.relationshipElements[index].removeSelf(); 
	}
	dojo.unsubscribe(this.providerSubs);
},
activate : function(){
	/*	
	 * Create a relationship element for each consuming component. Use the consuming component because it's
	 * a 1:1 relationship whereas the providing component may provide the element to many different consuming
	 * components.
	 */
	dojo.forEach(this.sscRelationship.consumedBy, function(component){
		
		var relationshipElement = new goat.elements.RelationshipElement(surface, 
					this.sscRelationship.name, this.sscRelationship.type, 
					components[this.sscRelationship.providedBy.id],components[component.id] );

		//Add a service decorator if it is a service relationship
		if (this.sscRelationship.type == "serviceExport") {
			relationshipElement.addDecorator(new goat.elements.TriangleDecorator(this.theme,surface));

		} else if (this.sscRelationship.type == "serviceImport") {
			relationshipElement.addDecorator(new goat.elements.TriangleDecorator(this.theme,surface));

		} else if (this.sscRelationship.type == "Service") {
			relationshipElement.addDecorator(new goat.elements.TriangleDecorator(this.theme,surface));
		}

		
		//Add the relationship to a list and subscript to the deletion of the consuming component
		this.relationshipElements[component.id] = relationshipElement;
		this.subs[component.id] = dojo.subscribe("goat.component.delete." + component.id, this, this.removeElement);
			
	},this);	
}
});

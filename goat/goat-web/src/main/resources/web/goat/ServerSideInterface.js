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
//callback from dwr, used to remove everything on a provider switch.
function forgetAboutEverything(){
	console.log("forgetting about everything.. ");
	
	dojo.forEach(relationships, function(relationship){
		if(relationship!=null){
			console.log("removing relationship "+relationship.getKey());
			delete relationship;
		}
	});
	relationships = new Array();
	
	dojo.forEach(components, function(component){
		if(component!=null){
			console.log("invoking removeself on "+component.id);
			component.removeSelf();
			delete component;
		}
	});
	components = new Array();
	

	console.log("making sure absolutely..");
	initialLayout.reset();
}

/**
 * DWR Callback.<p>
 * 
 * Invoked to add or update a component.<br> 
 * This method uses the component.id field to check if it already knows
 * of the component, and either updates an existing one, or creates a 
 * new one depending on the result.<p>
 * 
 * Updating a component can add, or modify component properties, but 
 * currently cannot delete them.<p>
 * 
 * Children are placeholders still.
 */
function addComponent(component) {
    console.log("******************* Component data **************");
	
	//if we dont know about the id yet, setup a new component.
	if (components[component.id] == null) {
		//create the component display object			
		components[component.id] = new goat.Component( surface, component.id, component.componentProperties);

		//put it somewhere sensible.
		initialLayout.placeComponent(components[component.id]);
	} else {
		console.log("Updating component "+component.id);
		//otherwise, we knew about the component, so update it.
		components[component.id].update(component.id, component.componentProperties);			
	}	
	//console.log("Done adding component");
}

//callback from dwr
function addRelationship(relationship) {
	console.log("******************* Relationship data **************");
	console.log(relationship);
	
	var r=new goat.Relationship(relationship);
	var key=r.getKey();
	
	console.log("checking relationship store for "+key);
	
	if(relationships[key]!=null){
		console.log("Found, issuing update");
		relationships[key].update(relationship);
	}else{
		console.log("Not found, creating new..");
		relationships[key]=r;
		//because we use getKey to test the existence, the constructor for 
		//Relationship is lazy, and we need to kick the instance to tell it
		//we actually plan to use it.		
		r.activate();
	}
}

//call back from componentstatusgrid to hide component.. //TODO: see if this can be made unglobal.
function hideComponent(id){
    components[id].toggleHidden();
}

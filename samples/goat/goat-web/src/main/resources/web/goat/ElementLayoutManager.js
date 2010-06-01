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
dojo.provide("goat.ElementLayoutManager");

//Positions elements within a Component. 
dojo.declare("goat.ElementLayoutManager", [], {

	owningComponent: null,
	layoutElements: null,

constructor : function(owningComponent) {
	//console.log("Building elementLayoutMananger");
	console.log(owningComponent);
	this.owningComponent = owningComponent;
},
doLayout: function(){	
    console.log(">doLayout");
    //console.log(this.owningComponent.elements);
    
	if(this.layoutElements!=null){
		this.owningComponent.group.remove(this.layoutElements);
		//console.log("removing old group");
	}
	//console.log("adding layout element group");
	this.layoutElements = this.owningComponent.group.createGroup();
	
	var yOffset=0; //needed to compensate for how strings get rendered.

	var yMargin=5;
	var xMargin=5;
	var rowPad=2;
	
	var currentYPos=yMargin + yOffset;
	
	var maxWidth = 0;
    var sepYPos = new Array();
	
    // Go through the rendering in two passes, initialisation and then rendering
	for ( var elementName in this.owningComponent.elements) {
		var element = this.owningComponent.elements[elementName];
		element.apply();
	}

	
	//console.log("processing array.. ");
	//walk through each element in the component, and position it relative to 0,0 inside the component.
	for(var elementName in this.owningComponent.elements){
		var element = this.owningComponent.elements[elementName];
		//console.log("Processing element .."+element.type);
		//console.log(element);
		//console.log(this);
	
		var hint = element.hint;
		
		var width=element.getWidth();
		var height=element.getHeight();
        //console.log(" - element size was "+width+" x "+height);				
      
		//or something.. 
		if(hint=="row"){
			element.x=0;
			element.y=currentYPos;
			
			//console.log("Invoking element.render");
			
			element.render();
			
			if(width>maxWidth){
				maxWidth=width;
			}

			currentYPos+=rowPad;			
			currentYPos+=height;
						
			//cant draw the sep line here, as the width of component isnt final yet.
			sepYPos.push(currentYPos);

		}else if(hint=="none"){
			element.render();
		}		
	}
	
	//update comp width with maxWidth seen during element processing.
	//console.log("Adjusting overall component dimensions");	
	this.owningComponent.width=maxWidth;
	this.owningComponent.height = currentYPos+yMargin;
	
	//now we know the width.. we can add our markup..
	dojo.forEach(sepYPos, function(ypos){
		//console.log("Adding sep line at "+ypos+" width "+this.owningComponent.width);
		
		//add separator line
		this.layoutElements.createLine({x1:0, y1:ypos, x2: this.owningComponent.width, y2: ypos})
		.setStroke({width:1, color: '#808080'});
	},this);
	
	console.log("<doLayout");
	
}

});

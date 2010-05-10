dojo.provide("demo.TwistieSection");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.Moveable");
dojo.require("dojo.data.ItemFileWriteStore");

//TwistieSection
// Manages an expandable component in a bundle
//
// TODO:
//  - update renderer to cope with massive import/exports, ie bundle 0)
//    - render max no of rows, add scroller, all needs doing by hand.
//    - needs careful thought
//  - current 'items' array is just text, for the breakout dependency show, 
//    will need individual attach coords for deplines. 
//  - may need to add custom events to issue when mouseover per item.
//  - may need to add custom events to issue when items are clicked? 
//    -item being clickable may be troublesome, maybe use a small handle to click beside each item.
//  - for services, render one triangle per expanded item, adjust any dependency to target triangle.
dojo.declare("demo.TwistieSection", [], {
	
	//object properties	
	parentGroup: null,
	owningBundle: null,
	name: null,
	x: null,
	y: null,
	getItemsCallback: null,
	items: null,
	isOpen: null,
	width: 0,
	height: 0,
	itemsObtained: false,
	
	//groups
	twistieGroup: null,
	twistieHandleGroup: null,
	
	//group items
	twistieHandle: null,
	twistieText: null,
	itemTexts: null,
	
	constructor: function(name, parentGroup, owningBundle, x, y, getItemsCallBack) {
	this.surface=surface;
	this.name=name;
	this.parentGroup=parentGroup;
	this.owningBundle=owningBundle;
	this.x=x;
	this.y=y;
	this.isOpen=false;
	this.getItemsCallback = getItemsCallBack;

	this.items=["Loading..."];
	this.itemTexts = new Array();

	this.twistieGroup = this.parentGroup.createGroup();	
	this.twistieHandleGroup = this.twistieGroup.createGroup();

	this.createText(this.name);

	this.createTwistie();
	
	this.twistieHandleGroup.connect("onclick",dojo.hitch(this,"twistieHandler"));            
},
createText: function(name){
	var textOffset = this.y + 8;
	var x = this.x + 17;
	this.twistieText = this.twistieGroup.createText({x: x, y: textOffset, text: name, align: "start"})
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
	this.twistieHandle = this.twistieHandleGroup.createPolyline([{x:px,y:pys},{x:pxe,y:pym},{x:px,y:pye}])
	//.setStroke({width: 2, color: '#808080'})
	.setFill("#000000");
	
	if(this.isOpen){
		this.twistieHandle.setShape([{x:px,y:pys},{x:pxe,y:pys},{x:pxm,y:pye}]);
	}
},
updatePosition: function(x,y){
	
	this.twistieHandleGroup.remove(this.twistieHandle);
	this.twistieGroup.remove(this.twistieHandle);
	this.twistieGroup.remove(this.twistieText);
	
	this.x=x;
	this.y=y;
	
	if(this.isOpen){
		this.removeItemsFromDisplay();
		this.addItemsToDisplay();
	}

	this.createText(this.name);
	this.createTwistie();
	
},
removeItemsFromDisplay: function(){
	var _this = this;
	if(this.itemTexts!=null){
		dojo.forEach( this.itemTexts , function(t){
			_this.twistieGroup.remove(t);	
		});
	}
	this.height = 0;
	this.width = 0;
},
addItemsToDisplay: function(){
	var maxLengthSeen = 0;
	var extraHeight = 0;
	if(this.items!=null){
		extraHeight = extraHeight + (this.items.length * 10);
		var pyt = this.y + 18;
		var pxt = this.x + 17;
		var idx=0;
		
		//make sure we clean up 1st.
		this.removeItemsFromDisplay();
		
		for(idx=0; idx<this.items.length; idx++){						
			//extra isOpen to try to prevent the add loop still adding while closed..
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
	
    //25 chars or so will fit into minwidth.. 
	var extraWidth = this.owningBundle.minWidth;				
	if(maxLengthSeen>25){
		var extraW = maxLengthSeen-25;
		extraW = extraW * 6;
		extraWidth = extraWidth + extraW;
	}
	
	if(extraWidth<this.width){
		extraWidth = this.owningBundle.width;
	}
	
	this.height = extraHeight;
	this.width = extraWidth;
},
twistieHandler: function() {
	var pys = this.y;
	var pym = pys+5;
	var pye = pys+10;
	this.isOpen=!this.isOpen;
	
	this.twistieHandleGroup.remove(this.twistieHandle);
	this.createTwistie();
	
	if(this.isOpen){	
		if(!this.itemsObtained){
			//hook up the imports to the bundle.			
			this.getItemsCallback();
			this.itemsObtained=true;
		}else{
			this.addItemsToDisplay();
		}
	}else{			
		this.removeItemsFromDisplay();
	}
	this.owningBundle.resize();	
}

});
/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/**
 * This script, when included in a html file, can be used to make collapsible menus
 *
 * Typical usage:
 * <script type="text/javascript" language="JavaScript" src="menus.js"></script>
 */

// Collapse all navigation bar menus
if (document.getElementById){ 
  	document.write('<style type="text/css">.menuitemgroup{display: none;}</style>')
}

// Keep the navigation menu open if the page is a sub directory of that area.
function SetMenu() {
	var open = 'url("/images/BigBulletOpen.png")';
	var path = document.location.href;
	var fields = path.split("/"); 
	var ident = fields[3];
    if(ident != "") {
    	var docel = document.getElementById(ident);
        var title = document.getElementById(ident+'Title');
        title.style.backgroundImage = open;
		docel.style.display = "block";
    }
}

//Switch navigation pane menu between open and closed on click.
function SwitchMenu(obj)
{
var open = 'url("/images/BigBulletOpen.png")';
var close = 'url("/images/BigBullet.png")';
  if(document.getElementById)  {
    var el = document.getElementById(obj);
    var title = document.getElementById(obj+'Title');

    if(el.style.display != "block"){ 
      title.style.backgroundImage = open;
      el.style.display = "block";
    }else{
      title.style.backgroundImage = close;
      el.style.display = "none";
    }
  }// end -  if(document.getElementById) 
}//end - function SwitchMenu(obj)

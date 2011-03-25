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
package org.apache.aries.samples.goat.api;

import java.util.List;
import java.util.Map;

public interface ComponentInfo {
   String getId();
   /**
    * always needed, id's must be unique globally, or within their containing component info.
    * (impl notes.. (for bundles)
    * Id's will probably NOT be bundle id's... we need the id to be fixed between framework restarts,
    * to enable things like storing coords for onscreen renderings of components
    * Id's will probably end up being path based, /component.id/component.id etc .. for sanities sake.
    * Component properties are information that forms part of a component, keys will vary depending on 
    * what the component represents. The GUI will handle rendering based on key names.
    */
   Map<String,String> getComponentProperties();

   
   /**
	* children are only supported in concept currently.. no gui work done yet for them..   
    * List of any contained components for this component.    
    */
   List<ComponentInfo> getChildren(); 
}

/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.testbundleb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.testbundlea.ProcessableBean;

public class BeanForBeanProcessorTest implements ProcessableBean{

    Set<BeanProcessor> bps = new HashSet<BeanProcessor>();
    Set<BeanProcessor> ad_bps = new HashSet<BeanProcessor>();
    Set<BeanProcessor> ai_bps = new HashSet<BeanProcessor>();
    Set<BeanProcessor> bd_bps = new HashSet<BeanProcessor>();
    Set<BeanProcessor> bi_bps = new HashSet<BeanProcessor>();
    
    private List<BeanProcessor> toList(Set<BeanProcessor> s){
        List<BeanProcessor> lbps = new ArrayList<BeanProcessor>();
        lbps.addAll(s);
        return lbps;
    }
    
    public List<BeanProcessor> getProcessedBy() {
        return toList(bps);
    }
    
    public List<BeanProcessor> getProcessedBy(Phase p) {
        switch(p){
          case BEFORE_INIT : return toList(bi_bps);
          case AFTER_INIT : return toList(ai_bps);
          case BEFORE_DESTROY : return toList(bd_bps);
          case AFTER_DESTROY : return toList(ad_bps);
          default: return null;
        }
    }    

    public void processAfterDestroy(BeanProcessor bp) {
        bps.add(bp);
        ad_bps.add(bp);
    }

    public void processAfterInit(BeanProcessor bp) {
        bps.add(bp);
        ai_bps.add(bp);
    }

    public void processBeforeDestroy(BeanProcessor bp) {
        bps.add(bp);
        bd_bps.add(bp);
    }

    public void processBeforeInit(BeanProcessor bp) {
        bps.add(bp);
        bi_bps.add(bp);
    }

}

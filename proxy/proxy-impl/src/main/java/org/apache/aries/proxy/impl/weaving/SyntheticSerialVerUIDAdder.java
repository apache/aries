/*
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
package org.apache.aries.proxy.impl.weaving;

import java.io.IOException;

import org.apache.aries.proxy.impl.gen.Constants;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SyntheticSerialVerUIDAdder extends SerialVersionUIDAdder {

  private static Logger LOGGER = LoggerFactory.getLogger(SyntheticSerialVerUIDAdder.class);

  private WovenProxyAdapter wpa;
  
  public SyntheticSerialVerUIDAdder(WovenProxyAdapter cv) {
    super(cv);
    wpa = cv;
  }

  @Override
  public void visitEnd() {
   
    wpa.setSVUIDGenerated(!!!isHasSVUID());
    super.visitEnd();
  }
  
  private boolean isHasSVUID() {
    try {
      if (computeSVUID() == 0 ) {
        // This means the class has a serial id already
        return true;      
      } else {
        return false;
      }
    } catch (IOException ioe) {
   
      LOGGER.debug(Constants.LOG_ENTRY, "cannot.compute.serial.id", new Object[] { ioe });

    } finally {
      return false;
    }

  }
}

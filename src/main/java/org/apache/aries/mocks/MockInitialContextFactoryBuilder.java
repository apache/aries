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
package org.apache.aries.mocks;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;

public class MockInitialContextFactoryBuilder implements InitialContextFactoryBuilder
{
  private static InitialContextFactory icf;
  
  public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
      throws NamingException
  {
    return icf;
  }
  
  public static void start(Context ctx) throws NamingException
  {
    if (icf == null) {
      NamingManager.setInitialContextFactoryBuilder(new MockInitialContextFactoryBuilder());
    }
    
    icf = Skeleton.newMock(InitialContextFactory.class);
    getSkeleton().setReturnValue(new MethodCall(InitialContextFactory.class, "getInitialContext", Hashtable.class), ctx);
  }
  
  public static Skeleton getSkeleton()
  {
    return Skeleton.getSkeleton(icf);
  }
}
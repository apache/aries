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
package org.apache.aries.versioning.impl.org.apache.aries.versioning.utils;
import static org.junit.Assert.assertEquals;

import org.apache.aries.versioning.utils.SemanticVersioningUtils;
import org.junit.Test;



public class SemanticVersionUtilsTest
{

  @Test
  public void testMethodTransform() {
   
    
    String returnStr = SemanticVersioningUtils.getReadableMethodSignature( "getAccountNum", "(Ljava/lang/String;)Ljava/lang/String;");
    String expectedStr = "method java/lang/String getAccountNum(java/lang/String)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    returnStr = SemanticVersioningUtils.getReadableMethodSignature( "handleNotification", "(Ljavax/management/Notification;Ljava/lang/Object;)V");
    expectedStr = "method void handleNotification(javax/management/Notification, java/lang/Object)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("addItemDeepCopy", "(Lcom/ibm/xml/xci/Cursor$Area;Lcom/ibm/xml/xci/Cursor;Lcom/ibm/xml/xci/Cursor$Profile;Lcom/ibm/xml/xci/Cursor$Profile;ZZZ)Lcom/ibm/xml/xci/Cursor;");
    expectedStr = "method com/ibm/xml/xci/Cursor addItemDeepCopy(com/ibm/xml/xci/Cursor$Area, com/ibm/xml/xci/Cursor, com/ibm/xml/xci/Cursor$Profile, com/ibm/xml/xci/Cursor$Profile, boolean, boolean, boolean)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("createParserAndCompiler", "(Ljavax/xml/transform/Source;Lcom/ibm/xltxe/rnm1/xtq/exec/XTQStaticContext;Lcom/ibm/xltxe/rnm1/xtq/common/utils/ErrorHandler;)Lcom/ibm/xltxe/rnm1/xtq/xquery/drivers/XQueryCompiler;");
    expectedStr = "method com/ibm/xltxe/rnm1/xtq/xquery/drivers/XQueryCompiler createParserAndCompiler(javax/xml/transform/Source, com/ibm/xltxe/rnm1/xtq/exec/XTQStaticContext, com/ibm/xltxe/rnm1/xtq/common/utils/ErrorHandler)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("getAxis", "()Lcom/ibm/xml/xci/exec/Axis;");
    expectedStr = "method com/ibm/xml/xci/exec/Axis getAxis()";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    
  
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("createEmpty", "()Lcom/ibm/xml/xci/dp/cache/dom/InternalNodeData;");
    expectedStr = "method com/ibm/xml/xci/dp/cache/dom/InternalNodeData createEmpty()";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("addElement", "(Lorg/w3c/dom/Node;)V");
    expectedStr = "method void addElement(org/w3c/dom/Node)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
  
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("isExternalFunctionCall", "(Lcom/ibm/xltxe/rnm1/xtq/ast/nodes/FunctionCall;Lcom/ibm/xltxe/rnm1/xtq/xpath/drivers/XPathCompiler;)Z");
    expectedStr = "method boolean isExternalFunctionCall(com/ibm/xltxe/rnm1/xtq/ast/nodes/FunctionCall, com/ibm/xltxe/rnm1/xtq/xpath/drivers/XPathCompiler)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("wrapForTracing", "(Lcom/ibm/xltxe/rnm1/xtq/xslt/runtime/output/ResultTreeSequenceWriterStream$TraceOutputEventGenerator;Lcom/ibm/xml/xci/SessionContext;Lcom/ibm/xml/xci/Cursor;Lcom/ibm/xml/xci/RequestInfo;Lcom/ibm/xltxe/rnm1/xtq/xslt/runtime/output/ResultTreeSequenceWriterStream$DeferredTraceResultTreeSequenceWriterStream;)Lcom/ibm/xml/xci/Cursor;");
    expectedStr = "method com/ibm/xml/xci/Cursor wrapForTracing(com/ibm/xltxe/rnm1/xtq/xslt/runtime/output/ResultTreeSequenceWriterStream$TraceOutputEventGenerator, com/ibm/xml/xci/SessionContext, com/ibm/xml/xci/Cursor, com/ibm/xml/xci/RequestInfo, com/ibm/xltxe/rnm1/xtq/xslt/runtime/output/ResultTreeSequenceWriterStream$DeferredTraceResultTreeSequenceWriterStream)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
    
    returnStr = SemanticVersioningUtils.getReadableMethodSignature("<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/security/Key;Ljava/security/Key;Ljava/security/cert/Certificate;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IJJLjava/lang/String;)V");
    expectedStr = "constructor with parameter list (java/lang/String, java/lang/String, java/lang/String, java/security/Key, java/security/Key, java/security/cert/Certificate, java/lang/String, java/lang/String, java/lang/String, java/lang/String, java/lang/String, java/lang/String, java/lang/String, java/lang/String, java/lang/String, int, long, long, java/lang/String)";
    assertEquals("The return str is incorrect.", expectedStr, returnStr);
    
  }
}

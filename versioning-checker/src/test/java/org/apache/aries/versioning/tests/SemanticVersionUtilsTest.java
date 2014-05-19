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
package org.apache.aries.versioning.tests;

import static org.junit.Assert.assertEquals;

import org.apache.aries.versioning.utils.SemanticVersioningUtils;
import org.junit.Test;


public class SemanticVersionUtilsTest {

    @Test
    public void testMethodTransform() {


        String returnStr = SemanticVersioningUtils.getReadableMethodSignature("getAccountNum", "(Ljava/lang/String;)Ljava/lang/String;");
        String expectedStr = "method java.lang.String getAccountNum(java.lang.String)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);

        returnStr = SemanticVersioningUtils.getReadableMethodSignature("handleNotification", "(Ljavax/management/Notification;Ljava/lang/Object;)V");
        expectedStr = "method void handleNotification(javax.management.Notification, java.lang.Object)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);

        returnStr = SemanticVersioningUtils.getReadableMethodSignature("addItemDeepCopy", "(Lcom/xml/xci/Cursor$Area;Lcom/xml/xci/Cursor;Lcom/xml/xci/Cursor$Profile;Lcom/xml/xci/Cursor$Profile;ZZZ)Lcom/xml/xci/Cursor;");
        expectedStr = "method com.xml.xci.Cursor addItemDeepCopy(com.xml.xci.Cursor$Area, com.xml.xci.Cursor, com.xml.xci.Cursor$Profile, com.xml.xci.Cursor$Profile, boolean, boolean, boolean)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);

        returnStr = SemanticVersioningUtils.getReadableMethodSignature("createParserAndCompiler", "(Ljavax/xml/transform/Source;Lcom/xltxe/rnm1/xtq/exec/XTQStaticContext;Lcom/xltxe/rnm1/xtq/common/utils/ErrorHandler;)Lcom/xltxe/rnm1/xtq/xquery/drivers/XQueryCompiler;");
        expectedStr = "method com.xltxe.rnm1.xtq.xquery.drivers.XQueryCompiler createParserAndCompiler(javax.xml.transform.Source, com.xltxe.rnm1.xtq.exec.XTQStaticContext, com.xltxe.rnm1.xtq.common.utils.ErrorHandler)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);


        returnStr = SemanticVersioningUtils.getReadableMethodSignature("getAxis", "()Lcom/xml/xci/exec/Axis;");
        expectedStr = "method com.xml.xci.exec.Axis getAxis()";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);


        returnStr = SemanticVersioningUtils.getReadableMethodSignature("createEmpty", "()Lcom/xml/xci/dp/cache/dom/InternalNodeData;");
        expectedStr = "method com.xml.xci.dp.cache.dom.InternalNodeData createEmpty()";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);


        returnStr = SemanticVersioningUtils.getReadableMethodSignature("addElement", "(Lorg/w3c/dom/Node;)V");
        expectedStr = "method void addElement(org.w3c.dom.Node)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);


        returnStr = SemanticVersioningUtils.getReadableMethodSignature("isExternalFunctionCall", "(Lcom/xltxe/rnm1/xtq/ast/nodes/FunctionCall;Lcom/xltxe/rnm1/xtq/xpath/drivers/XPathCompiler;)Z");
        expectedStr = "method boolean isExternalFunctionCall(com.xltxe.rnm1.xtq.ast.nodes.FunctionCall, com.xltxe.rnm1.xtq.xpath.drivers.XPathCompiler)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);


        returnStr = SemanticVersioningUtils.getReadableMethodSignature("wrapForTracing", "(Lcom/xltxe/rnm1/xtq/xslt/runtime/output/ResultTreeSequenceWriterStream$TraceOutputEventGenerator;Lcom/xml/xci/SessionContext;Lcom/xml/xci/Cursor;Lcom/xml/xci/RequestInfo;Lcom/xltxe/rnm1/xtq/xslt/runtime/output/ResultTreeSequenceWriterStream$DeferredTraceResultTreeSequenceWriterStream;)Lcom/xml/xci/Cursor;");
        expectedStr = "method com.xml.xci.Cursor wrapForTracing(com.xltxe.rnm1.xtq.xslt.runtime.output.ResultTreeSequenceWriterStream$TraceOutputEventGenerator, com.xml.xci.SessionContext, com.xml.xci.Cursor, com.xml.xci.RequestInfo, com.xltxe.rnm1.xtq.xslt.runtime.output.ResultTreeSequenceWriterStream$DeferredTraceResultTreeSequenceWriterStream)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);


        returnStr = SemanticVersioningUtils.getReadableMethodSignature("<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/security/Key;Ljava/security/Key;Ljava/security/cert/Certificate;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IJJLjava/lang/String;)V");
        expectedStr = "constructor with parameter list (java.lang.String, java.lang.String, java.lang.String, java.security.Key, java.security.Key, java.security.cert.Certificate, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, long, long, java.lang.String)";
        assertEquals("The return str is incorrect.", expectedStr, returnStr);

    }
}

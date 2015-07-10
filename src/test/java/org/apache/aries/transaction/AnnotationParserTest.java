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
package org.apache.aries.transaction;

import static org.apache.aries.transaction.annotations.TransactionPropagationType.Mandatory;
import static org.apache.aries.transaction.annotations.TransactionPropagationType.Required;
import static org.apache.aries.transaction.annotations.TransactionPropagationType.Supports;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.transaction.annotations.TransactionPropagationType;
import org.apache.aries.transaction.parsing.AnnotationParser;
import org.apache.aries.transaction.pojo.AnnotatedPojo;
import org.apache.aries.transaction.pojo.BadlyAnnotatedPojo1;
import org.apache.aries.transaction.pojo.BadlyAnnotatedPojo2;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

public class AnnotationParserTest {

    private TxComponentMetaDataHelper helper;
    private ComponentDefinitionRegistry cdr;
    private Interceptor interceptor;
    private AnnotationParser parser;
    private IMocksControl c;

    @Before
    public void setup() {
        c = EasyMock.createNiceControl();
        helper = c.createMock(TxComponentMetaDataHelper.class);
        cdr = c.createMock(ComponentDefinitionRegistry.class);
        interceptor = c.createMock(Interceptor.class);
        parser = new AnnotationParser(cdr, interceptor, helper);
    }

    @Test
    public void testFindAnnotation() {
        MutableBeanMetadata mbm = createMetaData(AnnotatedPojo.class);
        expect(helper.getComponentMethodTxAttribute(anyObject(ComponentMetadata.class), anyString())).andReturn(null).atLeastOnce();
        expect(cdr.getInterceptors(mbm)).andReturn(new ArrayList<Interceptor>());
        helper.setComponentTransactionData(cdr, mbm, Required, "increment");
        expectLastCall().times(1);
        helper.setComponentTransactionData(cdr, mbm, Supports, "checkValue");
        expectLastCall().times(1);
        helper.setComponentTransactionData(cdr, mbm, Mandatory, "getRealObject");
        expectLastCall().times(1);
        cdr.registerInterceptorWithComponent(mbm , interceptor);
        expectLastCall().times(1);
        c.replay();
        parser.beforeInit(new AnnotatedPojo(), "testPojo", null, mbm);
        c.verify();
    }

    @Test
    public void testAnnotationsOverridenByXML() {
        AnnotatedPojo bean = new AnnotatedPojo();
        MutableBeanMetadata mbm = createMetaData(bean.getClass());

        expect(helper.getComponentMethodTxAttribute(mbm, "increment")).andReturn(TransactionPropagationType.Never);
        expect(helper.getComponentMethodTxAttribute(mbm, "checkValue")).andReturn(null);
        expect(helper.getComponentMethodTxAttribute(mbm, "getRealObject")).andReturn(null);
        expect(cdr.getInterceptors(mbm)).andReturn(Arrays.asList(interceptor));
        helper.setComponentTransactionData(cdr, mbm, Supports, "checkValue");
        expectLastCall().times(1);
        helper.setComponentTransactionData(cdr, mbm, Mandatory, "getRealObject");
        expectLastCall().times(1);

        c.replay();
        parser.beforeInit(bean, "testPojo", null, mbm);
        c.verify();
    }


    @Test(expected=IllegalArgumentException.class)
    public void testNoPrivateAnnotation() {
        BadlyAnnotatedPojo1 bean = new BadlyAnnotatedPojo1();
        MutableBeanMetadata mbm = createMetaData(bean.getClass());
        expect(cdr.getInterceptors(mbm)).andReturn(new ArrayList<Interceptor>());
        expect(helper.getComponentMethodTxAttribute(anyObject(ComponentMetadata.class), anyString())).andReturn(null).atLeastOnce();
        c.replay();
        parser.beforeInit(bean, "testPojo", null, mbm);
        c.verify();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNoStaticAnnotation() {
        BadlyAnnotatedPojo2 bean = new BadlyAnnotatedPojo2();
        MutableBeanMetadata mbm = createMetaData(bean.getClass());
        expect(cdr.getInterceptors(mbm)).andReturn(new ArrayList<Interceptor>());
        expect(helper.getComponentMethodTxAttribute(anyObject(ComponentMetadata.class), anyString())).andReturn(null).atLeastOnce();
        c.replay();
        parser.beforeInit(bean, "testPojo", null, mbm);
        c.verify();
    }

    private MutableBeanMetadata createMetaData(Class<?> clazz) {
        MutableBeanMetadata mbm = new BeanMetadataImpl();
        mbm.setId("testPojo");
        mbm.setClassName(clazz.getName());
        return mbm;
    }
}

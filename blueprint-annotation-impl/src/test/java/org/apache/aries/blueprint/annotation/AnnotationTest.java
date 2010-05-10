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
package org.apache.aries.blueprint.annotation;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.aries.blueprint.jaxb.Tbean;
import org.apache.aries.blueprint.jaxb.Tblueprint;
import org.apache.aries.blueprint.jaxb.Tproperty;
import org.apache.aries.blueprint.jaxb.Tvalue;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import junit.framework.TestCase;

public class AnnotationTest extends TestCase {

    public void testGenerationFile() throws Exception {
        File file = new File("test-annotation.xml");
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        }

        Tblueprint tblueprint = new Tblueprint();
        List<Object> components = tblueprint.getServiceOrReferenceListOrBean();
        Tbean tbean = new Tbean();
        tbean.setDependsOn(null);
        tbean.setId("Bar");
        tbean.setClazz("org.apache.aries.blueprint.sample.Bar");
        List<Object> props = tbean.getArgumentOrPropertyOrAny();

        String value = "Hello Bar";
        Tproperty tp = new Tproperty();
        tp.setName("value");
        //Tvalue tvalue = new Tvalue();
        //tvalue.setContent(value);
        //tp.setValue(tvalue);
        tp.setValueAttribute(value);
        props.add(tp);

        components.add(tbean);

        marshallOBRModel(tblueprint, file);
    }

    private void marshallOBRModel(Tblueprint tblueprint, File blueprintFile)
            throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Tblueprint.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(tblueprint, blueprintFile);

    }
}

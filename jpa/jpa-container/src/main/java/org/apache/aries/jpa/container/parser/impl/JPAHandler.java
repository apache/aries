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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.parser.impl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This code is responsible for parsing the persistence.xml into PersistenceUnits
 */
public class JPAHandler extends DefaultHandler {
    /** The Persistence Units that we have parsed */
    private final Deque<PersistenceUnit> persistenceUnits = new ArrayDeque<PersistenceUnit>();
    /** The name of the current element */
    private String elementName;
    /** The version of the persistence.xml file */
    /** A StringBuilder for caching the information from getCharacters */
    private StringBuilder builder = new StringBuilder();
    /** The bundle that contains this persistence descriptor */
    private Bundle bundle;

    /**
     * Create a new JPA Handler for a bundle
     * 
     * @param bundle
     */
    public JPAHandler(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Collect up the characters, as element's characters may be split across multiple calls. Isn't SAX
     * lovely...
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        builder.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes)
        throws SAXException {
        // Do this setting first as we use it later.
        elementName = (localName == null || "".equals(localName)) ? name : localName;

        if ("persistence-unit".equals(elementName)) {
            String tranTypeSt = attributes.getValue("transaction-type");
            PersistenceUnitTransactionType tranType = tranTypeSt == null ? null : PersistenceUnitTransactionType.valueOf(tranTypeSt);
            persistenceUnits.push(new PersistenceUnit(bundle, 
                                                      attributes.getValue("name"), 
                                                      tranType));
        } else if ("exclude-unlisted-classes".equals(elementName))
            persistenceUnits.peek().setExcludeUnlisted(true);
        else if ("property".equals(elementName))
            persistenceUnits.peek().addProperty(attributes.getValue("name"), attributes.getValue("value"));

    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        String s = builder.toString().trim();
        // This step is VERY important, otherwise we pollute subsequent
        // elements
        builder = new StringBuilder();

        if ("".equals(s))
            return;

        PersistenceUnit pu = persistenceUnits.peek();

        if ("provider".equals(elementName))
            pu.setProviderClassName(s);
        else if ("jta-data-source".equals(elementName))
            pu.setJtaDataSourceName(s);
        else if ("non-jta-data-source".equals(elementName))
            pu.setNonJtaDataSourceName(s);
        else if ("class".equals(elementName))
            pu.addClassName(s);
        else if ("exclude-unlisted-classes".equals(elementName))
            pu.setExcludeUnlisted(Boolean.parseBoolean(s));
        else if ("shared-cache-mode".equals(elementName))
            pu.setSharedCacheMode(SharedCacheMode.valueOf(s));
        else if ("validation-mode".equals(elementName))
            pu.setValidationMode(ValidationMode.valueOf(s));
    }

    @Override
    public void error(SAXParseException spe) throws SAXException {
        // We throw this exception to be caught further up and logged
        // as an error there
        throw spe;
    }

    /**
     * @return The collection of persistence units that we have parsed
     */
    public Collection<PersistenceUnit> getPersistenceUnits() {
        return persistenceUnits;
    }

}

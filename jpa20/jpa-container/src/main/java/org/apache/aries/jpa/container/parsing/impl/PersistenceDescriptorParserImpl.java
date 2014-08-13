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

package org.apache.aries.jpa.container.parsing.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptor;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParser;
import org.apache.aries.jpa.container.parsing.PersistenceDescriptorParserException;
import org.apache.aries.util.io.RememberingInputStream;
import org.osgi.framework.Bundle;

/**
 * This class may be used to parse JPA persistence descriptors. The parser validates
 * using the relevant version of the persistence schema as defined by the xml file. 
 */
public class PersistenceDescriptorParserImpl implements PersistenceDescriptorParser {

  /* (non-Javadoc)
   * @see org.apache.aries.jpa.container.parsing.impl.PersistenceDescriptorParser#parse(org.osgi.framework.Bundle, org.apache.aries.jpa.container.parsing.PersistenceDescriptor)
   */
  public Collection<? extends ParsedPersistenceUnit> parse(Bundle b, PersistenceDescriptor descriptor) throws PersistenceDescriptorParserException {
    Collection<ParsedPersistenceUnit> persistenceUnits = new ArrayList<ParsedPersistenceUnit>();
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    RememberingInputStream is = null;
    boolean schemaFound = false;
    try {
      //Buffer the InputStream so we can mark it, though we'll be in 
      //trouble if we have to read more than 8192 characters before finding
      //the schema!
      is = new RememberingInputStream(descriptor.getInputStream());
      is.mark(Integer.MAX_VALUE);
      SAXParser parser = parserFactory.newSAXParser();
      try{
        parser.parse(is, new SchemaLocatingHandler());
      } catch (EarlyParserReturn epr) {
        //This is not really an exception, but a way to work out which
        //version of the persistence schema to use in validation
        Schema s = epr.getSchema();
        
        if(s != null) {
          schemaFound = true;
          parserFactory.setSchema(s);
          parserFactory.setNamespaceAware(true);
          parser = parserFactory.newSAXParser();
         
          //Get back to the beginning of the stream
          is.reset();
          
          JPAHandler handler = new JPAHandler(b, epr.getVersion());
          parser.parse(is, handler);
          persistenceUnits.addAll(handler.getPersistenceUnits());
        } 
      }
    } catch (Exception e) {
      throw new PersistenceDescriptorParserException(NLS.MESSAGES.getMessage("persistence.description.parse.error", descriptor.getLocation(), b.getSymbolicName(), b.getVersion()), e);
    } finally {
      if(is != null) try {
        is.closeUnderlying();
      } catch (IOException e) {
        //No logging necessary, just consume
      }
    }
    if(!!!schemaFound) {
      throw new PersistenceDescriptorParserException(NLS.MESSAGES.getMessage("persistence.descriptor.schema.not.found", descriptor.getLocation(), b.getSymbolicName(), b.getVersion()));
    }
    return persistenceUnits;
  }

}

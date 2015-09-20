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
package org.apache.aries.blueprint.plugin;

import java.util.Collection;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.aries.blueprint.plugin.model.OsgiServiceRef;

public class OsgiServiceRefWriter {
    private XMLStreamWriter writer;

    public OsgiServiceRefWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }

    public void write(Collection<OsgiServiceRef> serviceRefs) throws XMLStreamException {
        for (OsgiServiceRef serviceBean : serviceRefs) {
            writeServiceRef(serviceBean);
        }
    }

    private void writeServiceRef(OsgiServiceRef serviceBean) throws XMLStreamException {
        writer.writeEmptyElement("reference");
        writer.writeAttribute("id", serviceBean.id);
        writer.writeAttribute("interface", serviceBean.clazz.getName());
        if (serviceBean.filter != null && !"".equals(serviceBean.filter)) {
            writer.writeAttribute("filter", serviceBean.filter);
        }
        writer.writeCharacters("\n");
    }

}

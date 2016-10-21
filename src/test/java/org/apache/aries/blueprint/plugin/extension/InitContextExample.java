/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.extension;

import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.ContextInitializationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Map;

public class InitContextExample implements ContextInitializationHandler {
    @Override
    public void initContext(ContextEnricher contextEnricher) {
        final Map<String, String> customParameters = contextEnricher.getBlueprintConfiguration().getCustomParameters();
        for (final String param : customParameters.keySet()) {
            if (param.startsWith("example.")) {
                final String key = param.split("\\.")[1];
                contextEnricher.addBlueprintContentWriter("enrichContextWithExample-" + key, new XmlWriter() {
                    @Override
                    public void write(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
                        xmlStreamWriter.writeEmptyElement("example");
                        xmlStreamWriter.writeDefaultNamespace("http://exampleNamespace");
                        xmlStreamWriter.writeAttribute("id", key);
                        xmlStreamWriter.writeAttribute("value", customParameters.get(param));
                    }
                });
            }
        }
    }
}

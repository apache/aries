package org.apache.aries.blueprint.plugin.spi;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public interface BlueprintWriter {
    void write(XMLStreamWriter xmlStreamWriter) throws XMLStreamException;
}

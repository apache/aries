package org.apache.aries.blueprint.plugin.spi;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public interface XmlWriter {
    void write(XMLStreamWriter xmlStreamWriter) throws XMLStreamException;
}

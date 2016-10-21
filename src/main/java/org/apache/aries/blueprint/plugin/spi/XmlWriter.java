package org.apache.aries.blueprint.plugin.spi;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Write custom part of blueprint XML depends on context (inside bean or blueprint element)
 */
public interface XmlWriter {
    /**
     * Write custom XML
     * @param xmlStreamWriter xml writer provided by plugin
     * @throws XMLStreamException when exception occurred during writing XML
     */
    void write(XMLStreamWriter xmlStreamWriter) throws XMLStreamException;
}

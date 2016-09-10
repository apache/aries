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

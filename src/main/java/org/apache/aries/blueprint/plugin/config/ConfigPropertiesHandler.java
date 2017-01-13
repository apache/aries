package org.apache.aries.blueprint.plugin.config;

import org.apache.aries.blueprint.annotation.config.ConfigProperties;
import org.apache.aries.blueprint.plugin.spi.ContextEnricher;
import org.apache.aries.blueprint.plugin.spi.CustomDependencyAnnotationHandler;
import org.apache.aries.blueprint.plugin.spi.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

public class ConfigPropertiesHandler implements CustomDependencyAnnotationHandler<ConfigProperties> {
    @Override
    public String handleDependencyAnnotation(AnnotatedElement annotatedElement, String name, ContextEnricher contextEnricher) {
        ConfigProperties configProperties = annotatedElement.getAnnotation(ConfigProperties.class);
        final String pid = configProperties.pid();
        final boolean update = configProperties.update();
        final String id = getId(name, pid, update);
        enrichContext(contextEnricher, pid, update, id);
        return id;
    }

    private void enrichContext(ContextEnricher contextEnricher, final String pid, final boolean update, final String id) {
        contextEnricher.addBean(id, Properties.class);
        contextEnricher.addBlueprintContentWriter("properties/" + id, new XmlWriter() {
            @Override
            public void write(XMLStreamWriter writer) throws XMLStreamException {
                writer.writeEmptyElement("cm-properties");
                writer.writeDefaultNamespace("http://aries.apache.org/blueprint/xmlns/blueprint-cm/v1.2.0");
                writer.writeAttribute("id", id);
                writer.writeAttribute("persistent-id", pid);
                writer.writeAttribute("update", String.valueOf(update));
            }
        });
    }

    @Override
    public String handleDependencyAnnotation(Class<?> aClass, ConfigProperties configProperties, String name, ContextEnricher contextEnricher) {
        final String pid = configProperties.pid();
        final boolean update = configProperties.update();
        final String id = getId(name, pid, update);
        enrichContext(contextEnricher, pid, update, id);
        return id;
    }

    private String getId(String name, String pid, boolean update) {
        return name != null ? name : "properties-" + pid.replace('.', '-') + "-" + update;
    }

    @Override
    public Class<ConfigProperties> getAnnotation() {
        return ConfigProperties.class;
    }

    private Class<?> getClass(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Class<?>) {
            return (Class<?>) annotatedElement;
        }
        if (annotatedElement instanceof Method) {
            return ((Method) annotatedElement).getParameterTypes()[0];
        }
        if (annotatedElement instanceof Field) {
            return ((Field) annotatedElement).getType();
        }
        throw new RuntimeException("Unknown annotated element");
    }
}

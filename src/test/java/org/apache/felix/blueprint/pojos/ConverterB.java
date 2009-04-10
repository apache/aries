package org.apache.felix.blueprint.pojos;

import java.io.File;
import java.net.URI;

import org.osgi.service.blueprint.convert.Converter;

public class ConverterB implements Converter {

    public Object convert(Object source) throws Exception {
        if (source instanceof String) {
            return new URI((String) source);
        }
        throw new Exception("Unable to convert from " + (source != null ? source.getClass().getName() : "<null>") + " to " + URI.class.getName());
    }

    public Class getTargetClass() {
        return URI.class;
    }
}
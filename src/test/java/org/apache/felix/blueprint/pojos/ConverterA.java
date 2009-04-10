package org.apache.felix.blueprint.pojos;

import java.io.File;

import org.osgi.service.blueprint.convert.Converter;

public class ConverterA implements Converter {

    public Object convert(Object source) throws Exception {
        if (source instanceof String) {
            return new File((String) source);
        }
        throw new Exception("Unable to convert from " + (source != null ? source.getClass().getName() : "<null>") + " to " + File.class.getName());
    }

    public Class getTargetClass() {
        return File.class;
    }
}

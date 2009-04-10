package org.apache.felix.blueprint;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.List;

import junit.framework.TestCase;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.ArrayValue;
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.apache.felix.blueprint.parser.Parser;
import org.apache.felix.blueprint.reflect.LocalComponentMetadataImpl;

/**
 * TODO: constructor injection
 * TODO: Dependency#setMethod 
 */
public class ParserTest extends TestCase {

    public void testParseComponent() throws Exception {
        Parser parser = parse("/test-simple-component.xml");
        ComponentDefinitionRegistry registry = parser.getRegistry();
        assertNotNull(registry);
        ComponentMetadata component = registry.getComponentDefinition("pojoA");
        assertNotNull(component);
        assertEquals("pojoA", component.getName());
        Set<String> deps = component.getExplicitDependencies();
        assertNotNull(deps);
        assertEquals(2, deps.size());
        assertTrue(deps.contains("pojoB"));
        assertTrue(deps.contains("pojoC"));
        assertTrue(component instanceof LocalComponentMetadata);
        LocalComponentMetadata local = (LocalComponentMetadata) component;
        assertEquals("org.apache.felix.blueprint.pojos.PojoA", local.getClassName());
        ConstructorInjectionMetadata cns = local.getConstructorInjectionMetadata();
        assertNotNull(cns);
        List<ParameterSpecification> params = cns.getParameterSpecifications();
        assertNotNull(params);
        assertEquals(6, params.size());
        ParameterSpecification param = params.get(0);
        assertNotNull(param);
        assertEquals(0, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof TypedStringValue);
        assertEquals("val0", ((TypedStringValue) param.getValue()).getStringValue());
        assertNull(((TypedStringValue) param.getValue()).getTypeName());
        param = params.get(1);
        assertNotNull(param);
        assertEquals(2, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ReferenceValue);
        assertEquals("val1", ((ReferenceValue) param.getValue()).getComponentName());
        param = params.get(2);
        assertNotNull(param);
        assertEquals(1, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof NullValue);
        param = params.get(3);
        assertNotNull(param);
        assertEquals(3, param.getIndex());
        assertEquals("java.lang.String", param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof TypedStringValue);
        assertEquals("val3", ((TypedStringValue) param.getValue()).getStringValue());
        assertNull(((TypedStringValue) param.getValue()).getTypeName());
        param = params.get(4);
        assertNotNull(param);
        assertEquals(4, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ArrayValue);
        ArrayValue array = (ArrayValue) param.getValue();
        assertNull(array.getValueType());
        assertNotNull(array.getArray());
        assertEquals(3, array.getArray().length);
        assertTrue(array.getArray()[0] instanceof TypedStringValue);
        assertTrue(array.getArray()[1] instanceof ComponentValue);
        assertTrue(array.getArray()[2] instanceof NullValue);
        param = params.get(5);
        assertNotNull(param);
        assertEquals(5, param.getIndex());
        assertNull(param.getTypeName());
        assertNotNull(param.getValue());
        assertTrue(param.getValue() instanceof ReferenceValue);
        assertEquals("pojoB", ((ReferenceValue) param.getValue()).getComponentName());
    }

    public void testParse() throws Exception {
        Parser parser = new Parser();
        parser.parse(new URL[] { getClass().getResource("/test.xml") });
        ComponentDefinitionRegistry registry = parser.getRegistry();
        assertNotNull(registry);

    }


    protected Parser parse(String name) throws Exception {
        Parser parser = new Parser();
        parser.parse(new URL[] { getClass().getResource(name) });
        return parser;
    }
}

package org.apache.aries.spifly;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

public class HeaderParserTest extends TestCase {

    @Test
    public void testMethodWithMultipleParameters() {

        String header = "javax.ws.rs.client.FactoryFinder#find(java.lang.String," +
                "java.lang.String),javax.ws.rs.ext.FactoryFinder#find(java.lang.String,java" +
                ".lang.String) ,javax.ws.rs.other.FactoryFinder#find(java.lang.String,java" +
                ".lang.String)";

        List<HeaderParser.PathElement> pathElements = HeaderParser.parseHeader(header);
        assertEquals(3, pathElements.size());
        assertEquals(pathElements.get(0).getName(), "javax.ws.rs.client.FactoryFinder#find(java.lang.String,java.lang.String)");
        assertEquals(pathElements.get(1).getName(), "javax.ws.rs.ext.FactoryFinder#find(java.lang.String,java.lang.String)");
        assertEquals(pathElements.get(2).getName(), "javax.ws.rs.other.FactoryFinder#find(java.lang.String,java.lang.String)");
    }

}
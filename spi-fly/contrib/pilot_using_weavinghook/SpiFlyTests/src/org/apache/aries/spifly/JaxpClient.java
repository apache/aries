package org.apache.aries.spifly;

import javax.xml.parsers.DocumentBuilderFactory;

public class JaxpClient {
    public Class<?> test() {
        return DocumentBuilderFactory.newInstance().getClass();
    }
}

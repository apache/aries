package org.apache.aries.spifly;

import java.util.List;

import org.osgi.framework.Bundle;

public class WeavingData {
    private final String className;
    private final String methodName;
    private final int argCount;
    
    public WeavingData(String className, String methodName, int argCount) {
        this.className = className;
        this.methodName = methodName;
        this.argCount = argCount;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getArgCount() {
        return argCount;
    }
}

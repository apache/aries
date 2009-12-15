package org.apache.aries.blueprint.pojos;

interface Product<T> {
    void setProperty(T value);
}

public class Primavera implements Product<String> {
    public String prop;

    public void setProperty(String value) {
        prop = value;
    }    
}

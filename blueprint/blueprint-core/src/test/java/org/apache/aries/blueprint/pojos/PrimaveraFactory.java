package org.apache.aries.blueprint.pojos;

interface GenericFactory<T,U> {
    T getObject();
    T getObject(U value);
}

public class PrimaveraFactory implements GenericFactory<Primavera,String> {

    public Primavera getObject() {
        return new Primavera();
    }

    public Primavera getObject(String value) {
        Primavera res = new Primavera();
        res.setProperty(value);
        return res;
    }
}

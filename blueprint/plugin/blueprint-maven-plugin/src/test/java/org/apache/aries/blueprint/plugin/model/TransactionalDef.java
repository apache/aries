package org.apache.aries.blueprint.plugin.model;

public class TransactionalDef {
    final String method;
    final String type;

    public TransactionalDef(String method, String type) {
        this.method = method;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionalDef)) return false;

        TransactionalDef that = (TransactionalDef) o;

        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;

    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}

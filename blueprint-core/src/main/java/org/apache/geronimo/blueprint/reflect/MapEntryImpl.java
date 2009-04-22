package org.apache.geronimo.blueprint.reflect;

import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

public class MapEntryImpl implements MapEntry {

    private NonNullMetadata key;
    private Metadata value;

    public MapEntryImpl() {
    }

    public MapEntryImpl(NonNullMetadata key, Metadata value) {
        this.key = key;
        this.value = value;
    }

    public MapEntryImpl(MapEntry entry) {
        this.key = (NonNullMetadata) MetadataUtil.cloneMetadata(entry.getKey());
        this.value = MetadataUtil.cloneMetadata(entry.getValue());
    }

    public NonNullMetadata getKey() {
        return key;
    }

    public void setKey(NonNullMetadata key) {
        this.key = key;
    }

    public Metadata getValue() {
        return value;
    }

    public void setValue(Metadata value) {
        this.value = value;
    }
}

package org.apache.aries.blueprint.container;

import java.util.concurrent.atomic.AtomicLong;

public class IdSpace {
    private AtomicLong currentId = new AtomicLong(0);
    
    public long nextId() {
        return currentId.getAndIncrement();
    }
}

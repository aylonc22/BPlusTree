package org.example.oldVersion.simpleArena;

import java.nio.ByteBuffer;

public class AllocationResult {
    private final boolean success;
    private final ByteBuffer buffer;

    public AllocationResult(boolean success, ByteBuffer buffer) {
        this.success = success;
        this.buffer = buffer;
    }

    public boolean isSuccess() {
        return success;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}


package org.example;
import java.nio.ByteBuffer;
import java.util.Stack;

public class ArenaAllocator {
    private final ByteBuffer buffer;
    private final Stack<Integer> freeList;
    private final int blockSize;
    private int nextOffset;

    public ArenaAllocator(int size, int blockSize) {
        this.buffer = ByteBuffer.allocate(size);
        this.freeList = new Stack<>();
        this.blockSize = blockSize;
        this.nextOffset = 0;
    }

    public Integer allocate() {
        if (!freeList.isEmpty()) {
            return freeList.pop();
        }
        if (nextOffset + blockSize > buffer.capacity()) {
            throw new OutOfMemoryError("ArenaAllocator out of memory");
        }
        int offset = nextOffset;
        nextOffset += blockSize;
        return offset;
    }

    public void deallocate(int offset) {
        freeList.push(offset);
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public ByteBuffer getByteBufferAt(int offset) {
        ByteBuffer slice = buffer.duplicate();
        slice.position(offset);
        slice.limit(offset + blockSize);
        return slice;
    }
}


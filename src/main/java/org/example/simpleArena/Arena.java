package org.example.simpleArena;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class Arena implements AutoCloseable {

    private final ByteBuffer memoryBlock;
    private static final int DEFAULT_BLOCK_SIZE = 1024 * 1024; // 1MB default size

    private int allocatedBytes;
    private final int totalBytes;
    private boolean isDisposed;
    private int resetCount;
    private Consumer<Arena> onDispose;
    private Consumer<Arena> onReset;

    /**
     * Initialize the Arena allocator.
     *
     * @param initialSize Size of the memory block the allocator will use
     */
    public Arena(int initialSize) {
        if (initialSize <= 0)
            throw new IllegalArgumentException("Initial size must be larger than zero.");

        this.memoryBlock = ByteBuffer.allocateDirect(initialSize);
        this.totalBytes = initialSize;
    }

    /**
     * Initialize the Arena allocator with default size.
     */
    public Arena() {
        this(DEFAULT_BLOCK_SIZE);
    }
public ByteBuffer getBuffer(int startOffset){
    if (isDisposed)
        throw new IllegalStateException("Arena has been disposed.");
    if(startOffset == -1)
        throw  new IllegalArgumentException("startOffset cannot be -1");
    if(startOffset <= allocatedBytes){
        var res = tryAllocate(memoryBlock.getInt(startOffset),startOffset,Byte.class);
        return res.isSuccess()?res.getBuffer():null;
    }
    return null;
}
    /**
     * Allocate memory segment from Arena's pre-allocated block.
     *
     * @param amountOfTs Size of the segment to allocate (in bytes)
     * @return {@code AllocationResult<True,Segment>} if allocation succeeded, {@code AllocationResult<False,null>} otherwise
     */
    public <T>AllocationResult tryAllocate(int amountOfTs,int startOffset,T type) {
        if (isDisposed)
            throw new IllegalStateException("Arena has been disposed.");

        int allocationSize = amountOfTs * sizeOf(type);
        if(allocationSize + startOffset <= allocatedBytes){
            ByteBuffer segment = memoryBlock.duplicate();
            segment.position(startOffset);
            segment.limit(startOffset + allocationSize);

            return new AllocationResult(true,segment);
        }
        return new AllocationResult(false,null);
    }

    /**
     * Allocate memory segment from Arena's pre-allocated block.
     *
     * @param amountOfTs Amount of items to allocate
     * @param <T>        Type of data
     * @return Allocated segment
     */
    public <T>ByteBuffer allocate(int amountOfTs,T type) {
        if (isDisposed)
            throw new IllegalStateException("Arena has been disposed.");

        int allocationSize = amountOfTs * sizeOf(type);
        if (allocationSize + allocatedBytes > totalBytes)
            throw new OutOfMemoryError("Not enough memory left in the Arena.");

        ByteBuffer segment = memoryBlock.duplicate();
        segment.position(allocatedBytes);
        segment.limit(allocatedBytes + allocationSize);
        allocatedBytes += allocationSize;

        return segment;
    }
    //returning amount of bytes the data type has
    //if data type is not primitive throw error;
    private <T>int sizeOf(T dataType) throws IllegalArgumentException{
        if(dataType == Byte.class)
            return 1;
        if(dataType == Short.class)
            return 2;
        if(dataType == Integer.class)
            return 4;
        if(dataType == Long.class)
            return 8;
        if(dataType == Float.class)
            return 4;
        if(dataType == Double.class)
            return 8;
        if(dataType == Character.class)
            return 2;
        if(dataType == Boolean.class)
            return 1;
        throw new IllegalArgumentException("Not a primitive type");
    }

    /**
     * Reset the Arena, useful to reuse the Arena's in an object pool.
     */
    public void reset() {
        if (isDisposed)
            throw new IllegalStateException("Arena has been disposed.");

        resetCount++;
        memoryBlock.clear();
        allocatedBytes = 0;
        if (onReset != null) {
            onReset.accept(this);
        }
    }
    /**
     * Dispose the Arena and free allocated memory.
     */
    public void dispose(){
        if (!isDisposed) {
            memoryBlock.clear();
            isDisposed = true;
            allocatedBytes = 0;
            if (onDispose != null) {
                onDispose.accept(this);
            }
        }
    }
    /**
     * Dispose the Arena and free allocated memory when arena closed
     */
    @Override
    public void close() {
        dispose();
    }
    //region setters && getters
    public boolean isDisposed() {
        return isDisposed;
    }

    public int getAllocatedBytes() {
        return allocatedBytes;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public int getResetCount() {
        return resetCount;
    }

    public void setOnDispose(Consumer<Arena> onDispose) {
        this.onDispose = onDispose;
    }

    public void setOnReset(Consumer<Arena> onReset) {
        this.onReset = onReset;
    }
    //endregion
}
package org.example;

import java.nio.ByteBuffer;

public class BPlusTreeNode {
    private static final int KEY_SIZE = 4;
    private static final int CHILD_POINTER_SIZE = 4;

    private final ByteBuffer buffer;
    private final int maxKeys;
    private final int keySize;
    private final int childPointerSize;

    public BPlusTreeNode(ByteBuffer buffer, int maxKeys,int size,int parent) {
        this.buffer = buffer;
        this.maxKeys = maxKeys;
        this.keySize = KEY_SIZE;
        this.childPointerSize = CHILD_POINTER_SIZE;
        buffer.putInt(buffer.position(),size); // init size
        setNumKeys(0); // Initialize number of keys to 0
        setParentPointer(parent);
    }

    public long getBufferSize(){
        return buffer.getInt(buffer.position() );
    }
    public int getNumKeys() {
        return buffer.getInt(buffer.position() +4);
    }

    public void setNumKeys(int numKeys) {
        buffer.putInt(buffer.position() +4, numKeys);
    }

    public int getKey(int index) {
        return buffer.getInt(buffer.position() +4+4 + index * keySize);
    }

    public void setKey(int index, int key) {
        buffer.putInt(buffer.position() +4+4 + index * keySize, key);
    }

    public int getChildPointer(int index) {
        return buffer.getInt(buffer.position() +4+4 + maxKeys * keySize + index * childPointerSize);
    }

    public void setChildPointer(int index, int pointer) {
        buffer.putInt(buffer.position() +4+4 + maxKeys * keySize + index * childPointerSize, pointer);
    }
    public void setParentPointer(int parentPointer){
        buffer.putInt(buffer.position() +4 +KEY_SIZE + maxKeys * KEY_SIZE + (maxKeys+1) * CHILD_POINTER_SIZE,parentPointer);
    }
}

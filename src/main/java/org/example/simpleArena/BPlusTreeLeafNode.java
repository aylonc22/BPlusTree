package org.example.simpleArena;

import java.nio.ByteBuffer;

public class BPlusTreeLeafNode {
    private static final int KEY_SIZE = 4;
    private static final int VALUE_POINTER_SIZE = 8;

    private final ByteBuffer buffer;
    private final int maxKeys;
    private final int keySize;
    private final int valuePointerSize;

    public BPlusTreeLeafNode(ByteBuffer buffer, int maxKeys,int size) {
        this.buffer = buffer;
        this.maxKeys = maxKeys;
        this.keySize = KEY_SIZE;
        this.valuePointerSize = VALUE_POINTER_SIZE;
        buffer.putInt(size);
        buffer.putInt(4,0); //Initialize current key counter
        buffer.putInt(KEY_SIZE + maxKeys * keySize,-1); //Initialize next leaf pointer to -1 (none)
    }

    public int getBufferSize(){
        return buffer.getInt(0);
    }
    public int getNumKeys(){
        return buffer.getInt(4);
    }
    public void setNumKeys(int numKeys){
        buffer.putInt(4,numKeys);
    }
    public int getKey(int index){
        return buffer.getInt(4+ KEY_SIZE + index * keySize);
    }
    public void setKey(int index,int key){
        buffer.putInt(4 +KEY_SIZE + index * keySize,key);
    }
    public long getValuePointer(int index){
        return buffer.getLong(4 + KEY_SIZE + maxKeys*keySize+8 + index * valuePointerSize);
    }
    public void setValuePointer(int index,long pointer){
        buffer.putLong(4 +KEY_SIZE + maxKeys * keySize+8 + index*valuePointerSize,pointer);
    }
    public int getNextLeafPointer(){
        return buffer.getInt(4 +keySize + maxKeys * KEY_SIZE);
    }
    public void setNextLeafPointer(int pointer){
        buffer.putInt(4 +keySize + maxKeys * KEY_SIZE,pointer);
    }
}

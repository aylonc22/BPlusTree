package org.example.oldVersion.simpleArena;

import java.nio.ByteBuffer;

public class BPlusTreeLeafNode {
    private  final int KEY_SIZE = 4;
    private  final int VALUE_POINTER_SIZE = 8;
    private final int SEGMENT_SIZE = 4;

    private final ByteBuffer buffer;
    private final int maxKeys;

    public BPlusTreeLeafNode(ByteBuffer buffer, int maxKeys,int size,int parent) {
        this.buffer = buffer;
        this.maxKeys = maxKeys;
        buffer.putInt(buffer.position(),size);
       setNumKeys(0); //Initialize current key counter
        setNextLeafPointer(-1); //Initialize next leaf pointer to -1 (none)
        setParentPointer(parent);
    }

    public int getBufferSize(){
        return buffer.getInt(buffer.position());
    }
    public ByteBuffer getBuffer(){
        return buffer;
    }
    public int getNumKeys(){
        return buffer.getInt(buffer.position() +SEGMENT_SIZE);
    }
    public void setNumKeys(int numKeys){
        buffer.putInt(buffer.position() +SEGMENT_SIZE,numKeys);
    }
    public int getKey(int index){
        return buffer.getInt(buffer.position() +SEGMENT_SIZE+ KEY_SIZE + index * KEY_SIZE);
    }
    public void setKey(int index,int key){
        buffer.putInt(buffer.position() +SEGMENT_SIZE +KEY_SIZE + index * KEY_SIZE,key);
    }
    public long getValuePointer(int index){
        return buffer.getLong(buffer.position() +SEGMENT_SIZE + KEY_SIZE + maxKeys*KEY_SIZE + index * VALUE_POINTER_SIZE);
    }
    public void setValuePointer(int index,long pointer){
        buffer.putLong(buffer.position() + SEGMENT_SIZE +KEY_SIZE + maxKeys * KEY_SIZE + index*VALUE_POINTER_SIZE,pointer);
    }
    public int getNextLeafPointer(){
        return buffer.getInt(buffer.position() + SEGMENT_SIZE +KEY_SIZE + maxKeys * KEY_SIZE + maxKeys * VALUE_POINTER_SIZE);
    }
    public void setNextLeafPointer(int pointer){
        buffer.putInt(buffer.position() +SEGMENT_SIZE +KEY_SIZE + maxKeys * KEY_SIZE + maxKeys * VALUE_POINTER_SIZE,pointer);
    }
    public void setParentPointer(int parentPointer){
        buffer.putInt(buffer.position() +SEGMENT_SIZE +KEY_SIZE + maxKeys * KEY_SIZE + maxKeys * VALUE_POINTER_SIZE + 4,parentPointer);
    }
    public int getParentPointer(){
        return buffer.getInt(buffer.position() +SEGMENT_SIZE +KEY_SIZE + maxKeys * KEY_SIZE + (maxKeys+1) * VALUE_POINTER_SIZE + 4);
    }
}

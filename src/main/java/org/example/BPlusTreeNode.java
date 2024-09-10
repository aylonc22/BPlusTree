package org.example;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BPlusTreeNode {
    public boolean isLeaf;
    public List<Integer> keys;
    public List<String> values; // Only for leaf nodes
    public List<Integer> childrenOffsets; // Only for internal nodes
    public int offset;

    public BPlusTreeNode(boolean isLeaf, int offset) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.childrenOffsets = new ArrayList<>();
        this.offset = offset;
    }

    public static BPlusTreeNode deserialize(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        boolean isLeaf = buffer.get() == 1;
        BPlusTreeNode node = new BPlusTreeNode(isLeaf, offset);

        int keyCount = buffer.getInt();
        for (int i = 0; i < keyCount; i++) {
            node.keys.add(buffer.getInt());
        }

        if (isLeaf) {
            for (int i = 0; i < keyCount; i++) {
                int valueLength = buffer.getInt();
                byte[] valueBytes = new byte[valueLength];
                buffer.get(valueBytes);
                node.values.add(new String(valueBytes));
            }
        } else {
            for (int i = 0; i <= keyCount; i++) {
                node.childrenOffsets.add(buffer.getInt());
            }
        }

        return node;
    }

    public void serialize(ByteBuffer buffer) {
        System.out.println("Serializing node with " + keys.size() + " keys at offset " + buffer.position());
        buffer.put((byte) (isLeaf ? 1 : 0));
        buffer.putInt(keys.size());
        for (int key : keys) {
            buffer.putInt(key);
        }

        if (isLeaf) {
            for (String value : values) {
                byte[] valueBytes = value.getBytes();
                buffer.putInt(valueBytes.length);
                buffer.put(valueBytes);
            }
        } else {
            for (int offset : childrenOffsets) {
                buffer.putInt(offset);
            }
        }
        System.out.println("serializing end at offset " + buffer.position());
    }
}





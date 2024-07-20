package org.example;

import java.util.ArrayList;
import java.util.List;

public class BPlusTreeNode {
    boolean isLeaf;
    List<Integer> keys;
    List<BPlusTreeNode> children;
    BPlusTreeNode next;

    public BPlusTreeNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.next = null;
    }
}

package org.example;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BPlusTree {
    private static final int DEFAULT_ORDER = 3;
    private static final int DEFAULT_MB = 1;
    private ByteBuffer buffer;
    public BPlusTreeNode root;
    private int order;

    public BPlusTree() {
        this(DEFAULT_MB, DEFAULT_ORDER);
    }

    public BPlusTree(int MB, int order) {
        if(MB < 1){
            throw new IllegalArgumentException("Memory mus be 1 MB or more");
        }
        if(order < 3){
            throw new IllegalArgumentException("order must be 3 or more");
        }
        this.buffer = ByteBuffer.allocate((1024 * 1024) * MB);
        this.buffer.order(ByteOrder.BIG_ENDIAN);
        this.order = order;
        this.root = new BPlusTreeNode(true, allocateNode()); // Root starts as a leaf
        serializeNode(root);
    }

    private int allocateNode() {
        int position = buffer.position();
        buffer.position(position + 1024); // Allocate space for the node
        return position;
    }

    public void insertMany(HashMap<Integer,String> items){
        for(var item:items.entrySet()){
            insert(item.getKey(), item.getValue());
        }
    }
    public void insert(int key, String value) {
        BPlusTreeNode leaf = findLeaf(root, key);
        int index = leaf.keys.indexOf(key);
        if(index != -1){
            leaf.values.set(index,value);
            serializeNode(leaf);
        }
        else if (leaf.keys.size() < order - 1) {
            insertInLeaf(leaf, key, value);
        } else {
            splitLeaf(leaf, key, value);
        }
    }

    private BPlusTreeNode findLeaf(BPlusTreeNode node, int key) {
        while (!node.isLeaf) {
            int i = 0;
            while (i < node.keys.size() && key >= node.keys.get(i)) {
                i++;
            }
            int childOffset = node.childrenOffsets.get(i);
            node = BPlusTreeNode.deserialize(buffer, childOffset);
        }
        return node;
    }

    private void insertInLeaf(BPlusTreeNode leaf, int key, String value) {
        int index = 0;
        while (index < leaf.keys.size() && key > leaf.keys.get(index)) {
            index++;
        }
        leaf.keys.add(index, key);
        leaf.values.add(index, value);
        serializeNode(leaf);
    }

    private void splitLeaf(BPlusTreeNode leaf, int key, String value) {
        int t = (order - 1) / 2;
        BPlusTreeNode newLeaf = new BPlusTreeNode(true, allocateNode());

        List<Integer> allKeys = new ArrayList<>(leaf.keys);
        List<String> allValues = new ArrayList<>(leaf.values);
        int insertIndex = 0;
        while (insertIndex < allKeys.size() && key > allKeys.get(insertIndex)) {
            insertIndex++;
        }
        allKeys.add(insertIndex, key);
        allValues.add(insertIndex, value);

        newLeaf.keys.addAll(allKeys.subList(t + 1, allKeys.size()));
        newLeaf.values.addAll(allValues.subList(t + 1, allValues.size()));
        leaf.keys = new ArrayList<>(allKeys.subList(0, t + 1));
        leaf.values = new ArrayList<>(allValues.subList(0, t + 1));

        if (leaf == root) {
            BPlusTreeNode newRoot = new BPlusTreeNode(false, allocateNode());
            newRoot.keys.add(newLeaf.keys.get(0));
            newRoot.childrenOffsets.add(leaf.offset);
            newRoot.childrenOffsets.add(newLeaf.offset);
            root = newRoot;
            serializeNode(newRoot);
        } else {
            BPlusTreeNode parent = findParent(root, leaf);
            int index = parent.childrenOffsets.indexOf(leaf.offset);
            parent.keys.add(index, newLeaf.keys.get(0));
            parent.childrenOffsets.add(index + 1, newLeaf.offset);

            if (parent.keys.size() > order - 1) {
                splitInternalNode(parent);
            } else {
                serializeNode(parent);
            }
        }
        serializeNode(leaf);
        serializeNode(newLeaf);
    }

    private void splitInternalNode(BPlusTreeNode node) {
        int t = (order -1) / 2; // Number of keys in each split node
        BPlusTreeNode newInternal = new BPlusTreeNode(false, allocateNode());

        // Calculate the middle index
        int mid = t;

        // Move the keys and children to the new node
        newInternal.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        newInternal.childrenOffsets.addAll(node.childrenOffsets.subList(mid + 1, node.childrenOffsets.size()));

        // Adjust the current node
        node.keys = new ArrayList<>(node.keys.subList(0, mid +1));
        node.childrenOffsets = new ArrayList<>(node.childrenOffsets.subList(0, mid + 1));

        if (node == root) {
            // Create a new root
            BPlusTreeNode newRoot = new BPlusTreeNode(false, allocateNode());
            newRoot.keys.add(node.keys.get(mid));
            newRoot.childrenOffsets.add(node.offset);
            newRoot.childrenOffsets.add(newInternal.offset);
            root = newRoot;
            serializeNode(newRoot);
        } else {
            BPlusTreeNode parent = findParent(root, node);
            int index = parent.childrenOffsets.indexOf(node.offset);
            parent.keys.add(index, node.keys.get(mid));
            parent.childrenOffsets.add(index + 1, newInternal.offset);

            if (parent.keys.size() >= order - 1) {
                splitInternalNode(parent);
            } else {
                serializeNode(parent);
            }
        }
        serializeNode(node);
        serializeNode(newInternal);
    }

    private BPlusTreeNode findParent(BPlusTreeNode node, BPlusTreeNode child) {
        if (!node.isLeaf && node.childrenOffsets.contains(child.offset)) {
            return node;
        }
        for (Integer offset : node.childrenOffsets) {
            BPlusTreeNode n = BPlusTreeNode.deserialize(buffer, offset);
            BPlusTreeNode foundNode = findParent(n, child);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }
    public Object search(int key) {
        BPlusTreeNode leaf = findLeaf(root, key);
        int index = leaf.keys.indexOf(key);
        return index != -1 ? leaf.values.get(index) : null;
    }
    public void delete(int key) {
        BPlusTreeNode leaf = findLeaf(root, key);
        int index = leaf.keys.indexOf(key);

        if (index != -1) {
            leaf.keys.remove(index);
            leaf.values.remove(index);
            if (leaf.keys.size() < (order - 1) / 2 && leaf != root) {
                handleUnderflow(leaf);
            }
        }
        serializeNode(leaf);
    }
    private void handleUnderflow(BPlusTreeNode node) {
        BPlusTreeNode parent = findParent(root, node);
        int index = parent.childrenOffsets.indexOf(node.offset);

        if (index > 0) {
            BPlusTreeNode leftSibling = BPlusTreeNode.deserialize(buffer, parent.childrenOffsets.get(index - 1));
            if (leftSibling.keys.size() > (order - 1) / 2) {
                borrowFromLeftSibling(parent, index, node, leftSibling);
            } else {
                mergeWithLeftSibling(parent, index, node, leftSibling);
            }
            serializeNode(leftSibling);
        } else if (index < parent.childrenOffsets.size() - 1) {
            BPlusTreeNode rightSibling = BPlusTreeNode.deserialize(buffer, parent.childrenOffsets.get(index + 1));
            if (rightSibling.keys.size() > (order - 1) / 2) {
                borrowFromRightSibling(parent, index, node, rightSibling);
            } else {
                mergeWithRightSibling(parent, index, node, rightSibling);
            }
            serializeNode(rightSibling);
        }
        serializeNode(parent);

    }

    private void borrowFromLeftSibling(BPlusTreeNode parent, int index, BPlusTreeNode node, BPlusTreeNode leftSibling) {
        if (node.isLeaf) {
            // Leaf node
            int parentKeyIndex = index - 1;
            int parentKey = parent.keys.get(parentKeyIndex);

            //moving key
            int movingKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);

            node.keys.add(0, movingKey);
            node.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
            parent.keys.set(parentKeyIndex,movingKey );
        } else {
            // Internal node
            int parentKeyIndex = index - 1;
            int parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(0, parentKey);
            node.childrenOffsets.add(0, leftSibling.childrenOffsets.remove(leftSibling.childrenOffsets.size() - 1));
            parent.keys.set(parentKeyIndex, leftSibling.keys.remove(leftSibling.keys.size() - 1));
        }
    }

    private void borrowFromRightSibling(BPlusTreeNode parent, int index, BPlusTreeNode node, BPlusTreeNode rightSibling) {
        if (node.isLeaf) {
            // Leaf node
            int parentKeyIndex = index;
            int parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(parentKey);
            node.values.add(rightSibling.values.remove(0));
            parent.keys.set(parentKeyIndex, rightSibling.keys.remove(0));
        } else {
            // Internal node
            int parentKeyIndex = index;
            int parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(parentKey);
            node.childrenOffsets.add(rightSibling.childrenOffsets.remove(0));
            parent.keys.set(parentKeyIndex, rightSibling.keys.remove(0));
        }
    }

    private void mergeWithLeftSibling(BPlusTreeNode parent, int index, BPlusTreeNode node, BPlusTreeNode leftSibling) {
        int parentKeyIndex = index - 1; // The index of the key in the parent separating the nodes

        // Combine the current node with the left sibling
        leftSibling.keys.addAll(node.keys);
        leftSibling.childrenOffsets.addAll(node.childrenOffsets);

        // Remove the key from the parent and the current node from the parent's children list
        parent.keys.remove(parentKeyIndex);
        parent.childrenOffsets.remove(index);

        // Serialize the updated nodes
        serializeNode(leftSibling);
        serializeNode(parent);

        // Handle the case where the parent node becomes underflowed
        if (parent.keys.size() < (order - 1) / 2 && parent != root) {
            handleUnderflow(parent);
        }
        // Handle the case where the parent node is empty
        if(parent.keys.isEmpty()){
            int midIndex = (leftSibling.keys.size() - 1) / 2;
            parent.keys.add(leftSibling.keys.get(midIndex));
        }
    }

    private void mergeWithRightSibling(BPlusTreeNode parent, int index, BPlusTreeNode node, BPlusTreeNode rightSibling) {
        int parentKeyIndex = index; // The index of the parent key separating `node` and `rightSibling`

        // Merge the current node with the right sibling
        node.keys.addAll(rightSibling.keys);
        if (node.isLeaf) {
            node.values.addAll(rightSibling.values);
        } else {
            node.childrenOffsets.addAll(rightSibling.childrenOffsets);
        }

        // Remove the parent key and the right sibling from the parent’s children list
        parent.keys.remove(parentKeyIndex);
        parent.childrenOffsets.remove(index + 1);

        // Serialize the updated nodes
        serializeNode(node);
        serializeNode(parent);

        // Handle the case where parent node becomes underflowed
        if (parent.keys.size() < (order - 1) / 2 && parent != root) {
            handleUnderflow(parent);
        }
        // Handle the case where the parent node is empty
        if(parent.keys.isEmpty()){
            int midIndex = (rightSibling.keys.size() - 1) / 2;
            parent.keys.add(rightSibling.keys.get(midIndex));
        }
    }

    private void serializeNode(BPlusTreeNode node) {
        buffer.position(node.offset);
        node.serialize(buffer);
    }

    public void printTree(BPlusTreeNode node, String indent) {
        System.out.println(indent + (node.isLeaf ? "Leaf" : "Internal") + " Node:");
        System.out.println(indent + "Keys: " + node.keys);
        if (node.isLeaf) {
            System.out.println(indent + "Values: " + node.values);
        } else {
            System.out.println(indent + "Children Offsets: " + node.childrenOffsets);
            for (Integer offset : node.childrenOffsets) {
                BPlusTreeNode child = BPlusTreeNode.deserialize(buffer, offset);
                printTree(child, indent + "  ");
            }
        }
    }

    public static void main(String[] args) {
        BPlusTree tree = new BPlusTree();

        // Test insertion
        tree.insert(10, "Value10");
        tree.insert(20, "Value20");
        tree.insert(5,"da");

        tree.delete(20);

        // Print the tree
        tree.printTree(tree.root, "");
    }
}












package org.example;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * A B+ Tree implementation with an arena allocator for efficient memory management.
 * The B+ Tree supports insertion, deletion, and search operations.
 */
public class BPlusTree {
    private static final int DEFAULT_ORDER = 3; // Default order (maximum number of children per node)
    private static final int DEFAULT_MB = 1; // Default memory size (in megabytes) for the tree
    private ByteBuffer buffer; // Byte buffer to store the serialized nodes
    private BPlusTreeNode root; // Root node of the B+ Tree
    private int order; // Order of the B+ Tree
    private int lastAllocatedEndOffset = -1;
    private Set<Integer> printedOffsets = new HashSet<>();
    /**
     * Default constructor initializing the B+ Tree with default memory size and order.
     */
    public BPlusTree() {
        this(DEFAULT_MB, DEFAULT_ORDER);
    }

    /**
     * Constructor to initialize the B+ Tree with specified memory size and order.
     *
     * @param MB The memory size in megabytes.
     * @param order The order of the B+ Tree.
     */
    public BPlusTree(int MB, int order) {
        if (MB < 1) {
            throw new IllegalArgumentException("Memory must be 1 MB or more");
        }
        if (order < 3) {
            throw new IllegalArgumentException("Order must be 3 or more");
        }
        // Allocate buffer with the given memory size and set byte order
        this.buffer = ByteBuffer.allocate((1024 * 1024) * MB);
        this.buffer.order(ByteOrder.BIG_ENDIAN);
        this.order = order;
        // Initialize the root as a leaf node and serialize it
        this.root = new BPlusTreeNode(true, allocateNode(true),order);
        serializeNode(root);
    }

    /**
     * Allocate space for a new node in the buffer.
     *
     * @return The position where the node is allocated.
     */
    private int allocateNode(boolean isLeaf) {
        // Calculate the size of a node based on the order
        int maxKeys = order - 1; // Maximum number of keys in the node
        int keySize = 4; // Size of each key (assuming integer keys)
        int offsetSize = 4; // Size of each offset (assuming integer offsets)
        int valueSize = 15; // Maximum size of each value (adjust as needed)

        int nodeSize;
        if (isLeaf) {
            // Size calculation for leaf nodes
            nodeSize = (maxKeys * keySize) + (maxKeys * valueSize);
        } else {
            // Size calculation for internal nodes
            nodeSize = (maxKeys * keySize) + ((order * offsetSize) + (offsetSize * (maxKeys + 1)));
        }

        // Allocate space for the node
        BPlusTreeNode node = BPlusTreeNode.deserialize(buffer,root!=null?lastAllocatedEndOffset:0,order);
        int position = node.getEndOffset();
        if (position + nodeSize > buffer.capacity()) {
            throw new RuntimeException("Buffer capacity exceeded during node allocation");
        }
        buffer.position(position + nodeSize); // Allocate space for the node
        lastAllocatedEndOffset = buffer.position();
        return node.offset;
    }

    /**
     * Insert multiple key-value pairs into the B+ Tree.
     *
     * @param items A map containing key-value pairs to be inserted.
     */
    public void insertMany(HashMap<Integer, String> items) {
        for (var item : items.entrySet()) {
            insert(item.getKey(), item.getValue());
        }
    }

    /**
     * Insert a key-value pair into the B+ Tree.
     *
     * @param key The key to insert.
     * @param value The value associated with the key.
     */
    public void insert(int key, String value) {
        BPlusTreeNode leaf = findLeaf(root, key);
        int index = leaf.keys.indexOf(key);
        if (index != -1) {
            // Update the value if key already exists
            leaf.values.set(index, value);
            serializeNode(leaf);
        } else if (leaf.keys.size() < order - 1) {
            // Insert the key-value pair into the leaf node
            insertInLeaf(leaf, key, value);
        } else {
            // Split the leaf node if it is full
            splitLeaf(leaf, key, value);
        }
    }

    /**
     * Find the leaf node where a key should be located.
     *
     * @param node The current node.
     * @param key The key to find.
     * @return The leaf node containing the key.
     */
    private BPlusTreeNode findLeaf(BPlusTreeNode node, int key) {
        while (!node.isLeaf) {
            int i = 0;
            while (i < node.keys.size() && key >= node.keys.get(i)) {
                i++;
            }
            int childOffset = node.childrenOffsets.get(i);
            node = BPlusTreeNode.deserialize(buffer, childOffset,order);
        }
        return node;
    }

    /**
     * Insert a key-value pair into a leaf node.
     *
     * @param leaf The leaf node.
     * @param key The key to insert.
     * @param value The value associated with the key.
     */
    private void insertInLeaf(BPlusTreeNode leaf, int key, String value) {
        int index = 0;
        while (index < leaf.keys.size() && key > leaf.keys.get(index)) {
            index++;
        }
        leaf.keys.add(index, key);
        leaf.values.add(index, value);
        serializeNode(leaf);
    }

    /**
     * Split a leaf node and distribute its keys and values between the original and new leaf nodes.
     *
     * @param leaf The leaf node to split.
     * @param key The key to insert into the leaf.
     * @param value The value associated with the key.
     */
    private void splitLeaf(BPlusTreeNode leaf, int key, String value) {
        int t = (order - 1) / 2; // Number of keys in each split node
        BPlusTreeNode newLeaf = new BPlusTreeNode(true, allocateNode(true),order);

        // Prepare lists to redistribute keys and values
        List<Integer> allKeys = new ArrayList<>(leaf.keys);
        List<String> allValues = new ArrayList<>(leaf.values);
        int insertIndex = 0;
        while (insertIndex < allKeys.size() && key > allKeys.get(insertIndex)) {
            insertIndex++;
        }
        allKeys.add(insertIndex, key);
        allValues.add(insertIndex, value);

        // Split the keys and values between the old and new leaf nodes
        newLeaf.keys.addAll(allKeys.subList(t + 1, allKeys.size()));
        newLeaf.values.addAll(allValues.subList(t + 1, allValues.size()));
        leaf.keys = new ArrayList<>(allKeys.subList(0, t + 1));
        leaf.values = new ArrayList<>(allValues.subList(0, t + 1));

        // Update the root if necessary
        if (leaf == root) {
            BPlusTreeNode newRoot = new BPlusTreeNode(false, allocateNode(false),order);
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

    /**
     * Split an internal node and distribute its keys and children between the original and new internal nodes.
     *
     * @param node The internal node to split.
     */
    private void splitInternalNode(BPlusTreeNode node) {
        int t = (order - 1) / 2; // Number of keys in each split node
        BPlusTreeNode newInternal = new BPlusTreeNode(false, allocateNode(false),order);

        // Calculate the middle index
        int mid = t;

        // Move the keys and children to the new node
        newInternal.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        newInternal.childrenOffsets.addAll(node.childrenOffsets.subList(mid + 1, node.childrenOffsets.size()));

        // Adjust the current node
        node.keys = new ArrayList<>(node.keys.subList(0, mid + 1));
        node.childrenOffsets = new ArrayList<>(node.childrenOffsets.subList(0, mid + 1));

        if (node == root) {
            // Create a new root
            BPlusTreeNode newRoot = new BPlusTreeNode(false, allocateNode(false),order);
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

    /**
     * Find the parent of a given child node.
     *
     * @param node The current node.
     * @param child The child node.
     * @return The parent node, or null if not found.
     */
    private BPlusTreeNode findParent(BPlusTreeNode node, BPlusTreeNode child) {
        if (!node.isLeaf && node.childrenOffsets.contains(child.offset)) {
            return node;
        }
        for (Integer offset : node.childrenOffsets) {
            BPlusTreeNode n = BPlusTreeNode.deserialize(buffer, offset,order);
            BPlusTreeNode foundNode = findParent(n, child);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }

    /**
     * Search for a key in the B+ Tree and return its associated value.
     *
     * @param key The key to search for.
     * @return The value associated with the key, or null if the key is not found.
     */
    public String search(int key) {
        BPlusTreeNode leaf = findLeaf(root, key);
        int index = leaf.keys.indexOf(key);
        return index != -1 ? leaf.values.get(index) : null;
    }

    /**
     * Delete a key from the B+ Tree.
     *
     * @param key The key to delete.
     */
    public void delete(int key) {
        BPlusTreeNode leaf = findLeaf(root, key);
        int index = leaf.keys.indexOf(key);

        if (index != -1) {
            leaf.keys.remove(index);
            leaf.values.remove(index);
            // Handle underflow if necessary
            if (leaf.keys.size() < (order - 1) / 2 && leaf != root) {
                handleUnderflow(leaf);
            }
        }
        serializeNode(leaf);
    }

    /**
     * Handle the underflow situation in a node by either borrowing from or merging with siblings.
     *
     * @param node The node with underflow.
     */
    private void handleUnderflow(BPlusTreeNode node) {
        BPlusTreeNode parent = findParent(root, node);
        int index = parent.childrenOffsets.indexOf(node.offset);

        if (index > 0) {
            BPlusTreeNode leftSibling = BPlusTreeNode.deserialize(buffer, parent.childrenOffsets.get(index - 1),order);
            if (leftSibling.keys.size() > (order - 1) / 2) {
                borrowFromLeftSibling(parent, index, node, leftSibling);
            } else {
                mergeWithLeftSibling(parent, index, node, leftSibling);
            }
            serializeNode(leftSibling);
        } else if (index < parent.childrenOffsets.size() - 1) {
            BPlusTreeNode rightSibling = BPlusTreeNode.deserialize(buffer, parent.childrenOffsets.get(index + 1),order);
            if (rightSibling.keys.size() > (order - 1) / 2) {
                borrowFromRightSibling(parent, index, node, rightSibling);
            } else {
                mergeWithRightSibling(parent, index, node, rightSibling);
            }
            serializeNode(rightSibling);
        }
        serializeNode(parent);
    }

    /**
     * Borrow a key and child from the left sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param leftSibling The left sibling node.
     */
    private void borrowFromLeftSibling(BPlusTreeNode parent, int index, BPlusTreeNode node, BPlusTreeNode leftSibling) {
        if (node.isLeaf) {
            // Leaf node: borrow a key-value pair from the left sibling
            int parentKeyIndex = index - 1;
            int parentKey = parent.keys.get(parentKeyIndex);

            int movingKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);
            node.keys.add(0, movingKey);
            node.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
            parent.keys.set(parentKeyIndex, movingKey);
        } else {
            // Internal node: borrow a key and child from the left sibling
            int parentKeyIndex = index - 1;
            int parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(0, parentKey);
            node.childrenOffsets.add(0, leftSibling.childrenOffsets.remove(leftSibling.childrenOffsets.size() - 1));
            parent.keys.set(parentKeyIndex, leftSibling.keys.remove(leftSibling.keys.size() - 1));
        }
    }

    /**
     * Borrow a key and child from the right sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param rightSibling The right sibling node.
     */
    private void borrowFromRightSibling(BPlusTreeNode parent, int index, BPlusTreeNode node, BPlusTreeNode rightSibling) {
        if (node.isLeaf) {
            // Leaf node: borrow a key-value pair from the right sibling
            int parentKeyIndex = index;
            int parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(parentKey);
            node.values.add(rightSibling.values.remove(0));
            parent.keys.set(parentKeyIndex, rightSibling.keys.remove(0));
        } else {
            // Internal node: borrow a key and child from the right sibling
            int parentKeyIndex = index;
            int parentKey = parent.keys.get(parentKeyIndex);

            node.keys.add(parentKey);
            node.childrenOffsets.add(rightSibling.childrenOffsets.remove(0));
            parent.keys.set(parentKeyIndex, rightSibling.keys.remove(0));
        }
    }

    /**
     * Merge a node with its left sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param leftSibling The left sibling node.
     */
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
        if (parent.keys.isEmpty()) {
            int midIndex = (leftSibling.keys.size() - 1) / 2;
            parent.keys.add(leftSibling.keys.get(midIndex));
        }
    }

    /**
     * Merge a node with its right sibling.
     *
     * @param parent The parent node.
     * @param index The index of the node in the parent's children list.
     * @param node The current node.
     * @param rightSibling The right sibling node.
     */
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
        if (parent.keys.isEmpty()) {
            int midIndex = (rightSibling.keys.size() - 1) / 2;
            parent.keys.add(rightSibling.keys.get(midIndex));
        }
    }

    /**
     * Serialize a node to the ByteBuffer.
     *
     * @param node The node to serialize.
     */
    private void serializeNode(BPlusTreeNode node) {
        buffer.position(node.offset);
        node.serialize(buffer);
        buffer.position(node.getEndOffset());
    }

    /**
     * Prints the structure of the B+ Tree starting from the root node, with custom indentation.
     * This method is intended for cases where you want to control the indentation level manually.
     *
     * @param indent A string used for indentation to represent the tree's structure visually.
     *               This allows the caller to specify how deeply indented the output should be.
     */
    public void printTree(String indent) {
        // Clear the set of printed offsets to ensure a fresh start for printing.
        printedOffsets.clear();

        // Call the recursive printTree method to start printing from the root node.
        printTree(root, indent);
    }
    /**
     * Prints the structure of the B+ Tree starting from the root node, with default indentation.
     * This method is a convenient wrapper that starts printing from the root with an empty
     * indentation string. It's useful for a standard printout of the entire tree.
     */
    public void printTree() {
        // Clear the set of printed offsets to ensure a fresh start for printing.
        printedOffsets.clear();

        // Call the recursive printTree method to start printing from the root node.
        // Use an empty string for default indentation.
        printTree(root, "");
    }
    /**
     * Print the structure of the B+ Tree.
     *
     * @param node The starting node (usually the root).
     * @param indent The indentation for tree levels.
     */
    private void printTree(BPlusTreeNode node, String indent) {
        // Check if this node has already been printed
        if(printedOffsets.contains(node.offset)){
            return; // Skip printing if already printed
        }

        // Print the node's details
        System.out.println(indent + (node.isLeaf ? "Leaf" : "Internal") + " Node:");
        System.out.println(indent + "Keys: " + node.keys);
        if (node.isLeaf) {
            System.out.println(indent + "Values: " + node.values);
        } else {
            System.out.println(indent + "Children Offsets: " + node.childrenOffsets);
            // Recursively print child nodes
            for (Integer offset : node.childrenOffsets) {
                BPlusTreeNode child = BPlusTreeNode.deserialize(buffer, offset,order);
                printTree(child, indent + "  ");
            }
        }

        // Mark this node as printed
        printedOffsets.add((node.offset));
    }
}

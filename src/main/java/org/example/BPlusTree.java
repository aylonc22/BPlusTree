package org.example;

import java.util.Collections;
import java.util.List;

public class BPlusTree {
    private BPlusTreeNode root;
    private final int order;

    public BPlusTree(int order) {
        if(order<3){
            throw new IllegalArgumentException("Order Must be at least 3");
        }
        this.order = order;
        this.root = new BPlusTreeNode(true);
    }
    public BPlusTree(){
        this(3);
    }

    // Find the appropriate leaf node for insertion
    private BPlusTreeNode findLeaf(int key){
        BPlusTreeNode node = root;
        while (!node.isLeaf){
            int i = 0;
            while(i<node.keys.size() && key >= node.keys.get(i)){
                i++;
            }
            node = node.children.get(i);
        }
        return node;
    }

    // Insert a key into the B+ Tree
    public void insert(int key){
        BPlusTreeNode leaf = findLeaf(key);
        insertIntoLeaf(leaf,key);

        // Split the leaf node if it exceeds the order
        if(leaf.keys.size() > order - 1){
            splitLeaf(leaf);
        }
    }

    public void insertMany(List<Integer> keys){
        Collections.sort(keys);
        keys.forEach(this::insert);

    }

    // Insert into the leaf node
    private void insertIntoLeaf(BPlusTreeNode leaf,int key){
        int pos = Collections.binarySearch(leaf.keys,key);
        if(pos<0){
            pos = -(pos + 1);
        }
        leaf.keys.add(pos,key);
    }

    // Split a leaf node and update parent nodes
    private void splitLeaf(BPlusTreeNode leaf){
        int mid = (order + 1) / 2;
        var newLeaf = new BPlusTreeNode(true);

        // Move half the keys to the new leaf node
        newLeaf.keys.addAll(leaf.keys.subList(mid,leaf.keys.size()));
        leaf.keys.subList(mid,leaf.keys.size()).clear();

        newLeaf.next = leaf.next;
        leaf.next = newLeaf;

        // If the root splits, create a new root
        if(leaf == root){
            var newRoot = new BPlusTreeNode(false);
            newRoot.keys.add(newLeaf.keys.get(0));
            newRoot.children.add(leaf);
            newRoot.children.add(newLeaf);
            root = newRoot;
        }
        else {
            insertIntoParent(leaf,newLeaf,newLeaf.keys.get(0));
        }
    }

    // Insert into the parent node after a leaf split
    private void insertIntoParent(BPlusTreeNode left,BPlusTreeNode right,int key){
        var parent = findParent(root,left);
        if(parent == null){
            throw  new RuntimeException("Parent node not found for insertion");
        }

        int pos = Collections.binarySearch(parent.keys,key);
        if(pos<0){
            pos = -(pos + 1);
        }
        parent.keys.add(pos,key);
        parent.children.add(pos+1,right);

        // Split the internal node if it exceeds the order
        if(parent.keys.size() > order - 1){
            splitInternal(parent);
        }
    }

    // Split an internal node
    private void splitInternal(BPlusTreeNode internal){
        int mid = (order + 1) / 2;
        var newInternal = new BPlusTreeNode(false);

        newInternal.keys.addAll(internal.keys.subList(mid + 1,internal.keys.size()));
        internal.keys.subList(mid + 1,internal.keys.size()).clear();

        newInternal.children.addAll(internal.children.subList(mid+1,internal.children.size()));
        internal.children.subList(mid + 1,internal.children.size()).clear();

        if(internal == root){
            var newRoot = new BPlusTreeNode(false);
            newRoot.keys.add(internal.keys.get(mid));
            newRoot.children.add(internal);
            newRoot.children.add(newInternal);
            root = newRoot;
        }
        else {
            insertIntoParent(internal,newInternal,internal.keys.remove(mid));
        }
    }

    // Find the parent node of a given node
    private BPlusTreeNode findParent(BPlusTreeNode current,BPlusTreeNode target){
        if(current.isLeaf || current.children.isEmpty()){
            return null;
        }

        for(int i=0;i<current.children.size();i++){
            var child = current.children.get(i);

            if(child == target){
                return current;
            }

            var possibleParent = findParent(child,target);
            if(possibleParent != null){
                return possibleParent;
            }
        }
        // Parent not found
        return null;
    }

    //Search for a key in the B+ Tree
    public boolean search(int key){
        var node = findLeaf(key);
        int pos = Collections.binarySearch(node.keys,key);
        return pos >=0;
    }
    //Display the tree (for debugging purposes)
    public void printTree(){
        printNode(root,0);
    }
    private void printNode(BPlusTreeNode node,int level){
        System.out.println("Level " + level + ": " + node.keys);
        if(!node.isLeaf){
            for(var child: node.children){
                printNode(child,level + 1);
            }
        }
    }
}

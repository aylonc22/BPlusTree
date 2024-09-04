package org.example.oldVersion;

import org.example.oldVersion.simpleArena.Arena;
import org.example.oldVersion.simpleArena.BPlusTreeLeafNode;

import java.nio.ByteBuffer;
import java.util.*;

public class BPlusTree {
    public final Arena arena;
    private int rootNodeOffset;
    private final int nodeSize;
    private final int leafNodeSize;
    private final int maxKeys;
    private final int  SEGMENT_SIZE = 4;
    private final int KEY_SIZE = 4;
    private final int VALUE_SIZE = 8;
    private final int NEXT_LEAF_POINTER_SIZE = 4;
    private final int PARENT_POINTER = 4;

    public BPlusTree(int size,int maxKeys) {
        if(maxKeys<3){
            throw new IllegalArgumentException("Order Must be at least 3");
        }
        this.maxKeys = maxKeys;
        this.arena = size!=-1? new Arena(size) : new Arena();

        this.nodeSize = calculateNodeSize();
        this.leafNodeSize = calculateLeafNodeSize();

        this.rootNodeOffset = -1; // no root initially
    }
    public BPlusTree(){
        this(-1,3);
    }
    // Calculate size of internal node
    private int calculateNodeSize(){
        return SEGMENT_SIZE+KEY_SIZE + maxKeys * KEY_SIZE + (maxKeys+1)*NEXT_LEAF_POINTER_SIZE+PARENT_POINTER; // Number of keys + max keys (key size + child pointer size +1 more children)
    }
    // Calculate size of leaf node
    private int calculateLeafNodeSize() {
        return SEGMENT_SIZE+KEY_SIZE + maxKeys * (KEY_SIZE + VALUE_SIZE) + NEXT_LEAF_POINTER_SIZE+PARENT_POINTER; // Number of keys + (key size + value pointer size) + next leaf pointer size
    }
public void insertMany(HashMap<Integer,Long> items){
        for(var item:items.entrySet()){
            insert(item.getKey(),item.getValue());
        }
}
    public void insert(int key,long value){
        //Search logic return -1 when key not found...
        if(value == -1){
            throw new IllegalArgumentException("Value cannot be equal to -1!");
        }
        if(rootNodeOffset == -1){
            //Tree is empty, create root leaf node
            var rootBuffer = arena.allocate(leafNodeSize,Byte.class);
            var rootNode = new BPlusTreeLeafNode(rootBuffer,maxKeys,leafNodeSize,-1);
            rootNode.setNumKeys(1);
            rootNode.setKey(0,key);
            rootNode.setValuePointer(0,value);
            rootNodeOffset = rootBuffer.position();
        }
        else {

            //Check if the root needs to be split
            var rootBuffer = arena.getBuffer(rootNodeOffset);
            if(needSplitRoot(rootBuffer) && !isLeafNode(rootBuffer)){
              splitRoot();
                printTree();
            }

            //Insert into the appropriate node
            insertIntoNode(rootNodeOffset,key,value);
        }
}
private void insertIntoNode(int nodeOffset,int key,long value){

        var nodeBuffer = arena.getBuffer(nodeOffset);
    if(key == 6 ){
        printBuffer(nodeBuffer);
    }
        if(isLeafNode(nodeBuffer)){
            insertIntoLeaf(nodeBuffer,key,value);
        }
        else {

            int childOffset = findChild(nodeBuffer,key);
            var childBuffer = arena.getBuffer(childOffset);
            if(needSplit(childBuffer) && !isLeafNode(childBuffer)){
                splitChild(nodeBuffer,childBuffer);
                childOffset = findChild(nodeBuffer,key);
            }
            insertIntoNode(childOffset,key,value);
        }

}
//Check if a node is a leaf node
private boolean isLeafNode(ByteBuffer nodeBuffer){
    // Logic to determine if it's a leaf node (based on structure)
    return leafNodeSize==nodeBuffer.getInt(nodeBuffer.position());

}

//Find child index based on the key
private int findChild(ByteBuffer nodeBuffer,int key){
    //Get the current number of keys in the lead node
        int numKeys = nodeBuffer.getInt(MAX_KEYS_POSITION(nodeBuffer.position()));
        int index = 0;
        while(index < numKeys && key>= nodeBuffer.getInt(KEY_POSITION(nodeBuffer.position(),index))){
            index++;
        }
        return  nodeBuffer.getInt(CHILD_POSITION(nodeBuffer.position(),index));
}
//Insert into a leaf node
private void insertIntoLeaf(ByteBuffer leafBuffer,int key,long value){
        //Get the current number of keys in the lead node
    int numKeys = leafBuffer.getInt(MAX_KEYS_POSITION(leafBuffer.position()));

    //Find the index where the new key should be inserted
    int index = 0;
    while (index < numKeys && key >= leafBuffer.getInt(KEY_POSITION(leafBuffer.position(),index))){
        index++;
    }

    //Check if the key already exists (if so,update the value)
    if (index < numKeys && key == leafBuffer.getInt(KEY_POSITION(leafBuffer.position(), index))) {
        leafBuffer.putLong(VALUE_POSITION(leafBuffer.position(), index), value);
        return;
    }

    // Shift existing keys and values to the right to make space for the new key
    for (int i = numKeys; i > index; i--) {
        int srcKeyPosition = KEY_POSITION(leafBuffer.position(),i-1);
        int destKeyPosition = KEY_POSITION(leafBuffer.position(),i);
        int srcValuePosition = VALUE_POSITION(leafBuffer.position(),i-1);
        int destValuePosition = VALUE_POSITION(leafBuffer.position(),i);

        // Shift keys
        leafBuffer.putInt(destKeyPosition, leafBuffer.getInt(srcKeyPosition));

        // Shift values
        leafBuffer.putLong(destValuePosition, leafBuffer.getLong(srcValuePosition));
    }

    //Insert the new key and value at the determined index
    leafBuffer.putInt(KEY_POSITION(leafBuffer.position(),index),key);
    leafBuffer.putLong(VALUE_POSITION(leafBuffer.position(),index),value);

    //Update the number of keys in the leaf
    leafBuffer.putInt(leafBuffer.position() +KEY_SIZE,numKeys + 1);

    //Check if we need to split the leaf
    if(needSplit(leafBuffer)){
        //Find sibling leaf node
        var siblingBuffer = findSibling(leafBuffer);
        if(siblingBuffer != null){
            //Try to redistribute keys with sibling
            if(redistribute(leafBuffer,siblingBuffer)){
                //Redistribution successful, no need to split
                return;
            }
        }
        //Perform split if redistribution was not successful or no sibling was found
        splitLeaf(leafBuffer);
    }

}
    private ByteBuffer findSibling(ByteBuffer leafBuffer) {
        // Find the parent node
        int parentOffset = leafBuffer.getInt(PARENT_POSITION(leafBuffer.position(), true));
        if (parentOffset == -1) {
            return null; // No parent, no sibling
        }

        // Retrieve the parent node buffer
        ByteBuffer parentBuffer = arena.getBuffer(parentOffset);
        int numKeys = parentBuffer.getInt(MAX_KEYS_POSITION(parentBuffer.position()));

        // Find the position of the current leaf in the parent's child pointers
        int leafPosition = leafBuffer.position();
        int leafIndex = 0;
        while (leafIndex <= numKeys && parentBuffer.getInt(CHILD_POSITION(parentBuffer.position(), leafIndex)) != leafPosition) {
            leafIndex++;
        }

        // Return the sibling if it exists
        if (leafIndex > 0 && leafIndex <= numKeys) {
            // Check the previous sibling
            ByteBuffer prevSiblingBuffer = arena.getBuffer(parentBuffer.getInt(CHILD_POSITION(parentBuffer.position(), leafIndex - 1)));
            if (prevSiblingBuffer != null && isLeafNode(prevSiblingBuffer)) {
                return prevSiblingBuffer;
            }

            // Check the next sibling
            if (leafIndex < numKeys) {
                ByteBuffer nextSiblingBuffer = arena.getBuffer(parentBuffer.getInt(CHILD_POSITION(parentBuffer.position(), leafIndex + 1)));
                if (nextSiblingBuffer != null && !isLeafNode(nextSiblingBuffer)) {
                    return nextSiblingBuffer;
                }
            }
        }
        return null;
    }

    private boolean redistribute(ByteBuffer currentNodeBuffer,ByteBuffer siblingBuffer) {
        boolean isLeftSibling = siblingBuffer.position() < currentNodeBuffer.position();

        if (isLeftSibling) {
            return redistributeFromCurrentToLeftSibling(currentNodeBuffer, siblingBuffer);

        } else {
            return redistributeFromCurrentToRightSibling(currentNodeBuffer, siblingBuffer);
        }
    }

    private boolean redistributeFromCurrentToLeftSibling(ByteBuffer currentNodeBuffer, ByteBuffer leftSiblingBuffer) {
        // Check if redistribution is possible
        if (leftSiblingBuffer.getInt(MAX_KEYS_POSITION(leftSiblingBuffer.position()))>=maxKeys ){
            return false; // Redistribution not possible
        }

        //Determine if the node is a leaf
        if(isLeafNode(currentNodeBuffer)){
            //Fetching key and value to move
            int movedKey = currentNodeBuffer.getInt(KEY_POSITION(currentNodeBuffer.position(),0));
            long movedValue = currentNodeBuffer.getLong(VALUE_POSITION(currentNodeBuffer.position(),0));

            //Fetching && update numKey and shifting the current node
            int numKey = currentNodeBuffer.getInt(MAX_KEYS_POSITION(currentNodeBuffer.position())) ;
            shiftLeftLeaf(numKey,currentNodeBuffer);

            //Changing num key in current node
            currentNodeBuffer.putInt(MAX_KEYS_POSITION(currentNodeBuffer.position()),numKey-1);

            //Inserting key and value to left sibling
            int leftSiblingNumKey = leftSiblingBuffer.getInt(MAX_KEYS_POSITION(leftSiblingBuffer.position()));
            leftSiblingBuffer.putInt(KEY_POSITION(leftSiblingBuffer.position(),leftSiblingNumKey),movedKey);
            leftSiblingBuffer.putLong(VALUE_POSITION(leftSiblingBuffer.position(),leftSiblingNumKey),movedValue);

            //Increment numKey in left sibling
            leftSiblingBuffer.putInt(MAX_KEYS_POSITION(leftSiblingBuffer.position()),leftSiblingNumKey+1);
        }
        else{
            //Fetching key and pointers to move
            int movedKey = currentNodeBuffer.getInt(KEY_POSITION(currentNodeBuffer.position(),0));
            int movedPointer1 = currentNodeBuffer.getInt(CHILD_POSITION(currentNodeBuffer.position(),0));
            int movedPointer2 = currentNodeBuffer.getInt(CHILD_POSITION(currentNodeBuffer.position(),1));

            //Fetching numKey and shifting the current node
            int numKey = currentNodeBuffer.getInt(MAX_KEYS_POSITION(currentNodeBuffer.position()));
            shiftLeftNode(numKey,currentNodeBuffer);

            //Changing num key in current node
            currentNodeBuffer.putInt(MAX_KEYS_POSITION(currentNodeBuffer.position()),numKey-1);

            //Inserting key and child to left sibling
            int leftSiblingNumKey = leftSiblingBuffer.getInt(MAX_KEYS_POSITION(leftSiblingBuffer.position()));
            leftSiblingBuffer.putInt(KEY_POSITION(leftSiblingBuffer.position(),leftSiblingNumKey),movedKey);
            leftSiblingBuffer.putInt(CHILD_POSITION(leftSiblingBuffer.position(),leftSiblingNumKey),movedPointer1);
            leftSiblingBuffer.putInt(CHILD_POSITION(leftSiblingBuffer.position(),leftSiblingNumKey)+1,movedPointer2);

            //Increment numKey in left sibling
            leftSiblingBuffer.putInt(MAX_KEYS_POSITION(leftSiblingBuffer.position()),leftSiblingNumKey+1);
        }

        return true;
    }

   //TODO
    //convert it to current to right as i have in current to left
    private boolean redistributeFromCurrentToRightSibling(ByteBuffer currentNodeBuffer, ByteBuffer rightSiblingBuffer) {
        int numKeysCurrent = currentNodeBuffer.getInt(MAX_KEYS_POSITION(currentNodeBuffer.position()));
        int numKeysRightSibling = rightSiblingBuffer.getInt(MAX_KEYS_POSITION(rightSiblingBuffer.position()));

        // Check if redistribution is possible
        if (numKeysRightSibling <= 0 || numKeysCurrent >= maxKeys - 1) {
            return false; // Redistribution not possible
        }

        // Move one key from the current node to the right sibling
        int movedKey = currentNodeBuffer.getInt(KEY_POSITION(currentNodeBuffer.position(), numKeysCurrent - 1));
        long movedValue = currentNodeBuffer.getLong(VALUE_POSITION(currentNodeBuffer.position(), numKeysCurrent - 1));

        // Insert the new key into the right sibling
        rightSiblingBuffer.putInt(KEY_POSITION(rightSiblingBuffer.position(), numKeysRightSibling), movedKey);
        rightSiblingBuffer.putLong(VALUE_POSITION(rightSiblingBuffer.position(), numKeysRightSibling), movedValue);

        // Update the right sibling's key count
        rightSiblingBuffer.putInt(MAX_KEYS_POSITION(rightSiblingBuffer.position()), numKeysRightSibling + 1);

        // Update the current node's key count
        currentNodeBuffer.putInt(MAX_KEYS_POSITION(currentNodeBuffer.position()), numKeysCurrent - 1);

        // Shift the remaining keys in the current node to fill the gap
        for (int i = numKeysCurrent - 1; i > 0; i--) {
            int keyPosition = KEY_POSITION(currentNodeBuffer.position(), i - 1);
            int newKeyPosition = KEY_POSITION(currentNodeBuffer.position(), i);
            int valuePosition = VALUE_POSITION(currentNodeBuffer.position(), i - 1);
            int newValuePosition = VALUE_POSITION(currentNodeBuffer.position(), i);

            currentNodeBuffer.putInt(newKeyPosition, currentNodeBuffer.getInt(keyPosition));
            currentNodeBuffer.putLong(newValuePosition, currentNodeBuffer.getLong(valuePosition));
        }

        return true;
    }
private void splitLeaf(ByteBuffer leafBuffer){
        //Allocate a new buffer for the new leaf node
        var newLeafBuffer = arena.allocate(leafNodeSize,Byte.class);
        var newLeafNode = new BPlusTreeLeafNode(newLeafBuffer,maxKeys,leafNodeSize,-1);

        //Get the current number of keys in the leaf node
        int numKeys = leafBuffer.getInt(MAX_KEYS_POSITION(leafBuffer.position()));
        //Calculate the split index
        int splitIndex = numKeys / 2;
        //Set the number of keys in the new leaf node
        newLeafNode.setNumKeys(numKeys - splitIndex);

        //Copy the keys and values to the new leaf node
        for(int i=splitIndex;i<numKeys;i++){
            newLeafNode.setKey(i-splitIndex,leafBuffer.getInt(KEY_POSITION(leafBuffer.position(),i)));
            newLeafNode.setValuePointer(i - splitIndex,leafBuffer.getLong(VALUE_POSITION(leafBuffer.position(),i)));
        }

        //Update the number of keys in the current leaf node
        leafBuffer.putInt(MAX_KEYS_POSITION(leafBuffer.position()),splitIndex);

        //Update the next leaf pointer in the current leaf node
        leafBuffer.putInt(NEXT_LEAF_POSITION(leafBuffer.position()),newLeafBuffer.position());
        //Update the parent of the new leaf node as the leaf node
        newLeafBuffer.putInt(PARENT_POSITION(newLeafBuffer.position(),true),
                leafBuffer.getInt(PARENT_POSITION(leafBuffer.position(),true)));
        //Insert the new leaf node into the parent node
        insertIntoParentNode(leafBuffer,newLeafNode,leafBuffer.getInt(KEY_POSITION(leafBuffer.position(),splitIndex)));
}
private void insertIntoParentNode(ByteBuffer currentNodeBuffer,BPlusTreeLeafNode newLeafNode,int medianKey){
        int parentNodeOffset = currentNodeBuffer.getInt(PARENT_POSITION(currentNodeBuffer.position(),true));

        if(parentNodeOffset == -1){
            var newRootBuffer = arena.allocate(nodeSize,Byte.class);
            var newRootNode = new BPlusTreeNode(newRootBuffer,maxKeys,nodeSize,-1);
            newRootNode.setNumKeys(1);
            newRootNode.setKey(0,medianKey);
            newRootNode.setChildPointer(0,currentNodeBuffer.position());
            newRootNode.setChildPointer(1,newLeafNode.getBuffer().position());
            newLeafNode.setParentPointer(newRootBuffer.position());
            rootNodeOffset = newRootBuffer.position();
        }
        else{
            var parentNodeBuffer = arena.getBuffer(parentNodeOffset);
            if(needSplit(parentNodeBuffer)){
                splitChild(parentNodeBuffer,currentNodeBuffer);
                insertIntoParentNode(parentNodeBuffer,newLeafNode,medianKey);
            }
            else {
                insertIntoFullParent(parentNodeBuffer,medianKey,newLeafNode.getBuffer().position());
            }
        }
}
private void insertIntoFullParent(ByteBuffer parentNodeBuffer,int medianKey,int newChildOffset){
    int numKeys = parentNodeBuffer.getInt(parentNodeBuffer.position() + SEGMENT_SIZE);
    int insertIndex = 0;

    // Find the index to insert the new key
    while (insertIndex < numKeys && medianKey > parentNodeBuffer.getInt(KEY_POSITION(parentNodeBuffer.position(),insertIndex))) {
        insertIndex++;
    }

    // Shift keys to the right
    for (int i = numKeys; i > insertIndex; i--) {
        parentNodeBuffer.putInt(KEY_POSITION(parentNodeBuffer.position(),i), parentNodeBuffer.getInt(KEY_POSITION(parentNodeBuffer.position(),i-1)));
    }

    // Shift child pointers to the right
    for (int i = numKeys + 1; i > insertIndex + 1; i--) {
        parentNodeBuffer.putInt(CHILD_POSITION(parentNodeBuffer.position(),i), parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(),i-1)));
    }

    // Insert the new key and update the child pointer
    parentNodeBuffer.putInt(KEY_POSITION(parentNodeBuffer.position(),insertIndex), medianKey);
    parentNodeBuffer.putInt(CHILD_POSITION(parentNodeBuffer.position(),insertIndex+1), newChildOffset);

    // Update the number of keys in the parent
    parentNodeBuffer.putInt(MAX_KEYS_POSITION(parentNodeBuffer.position()), numKeys + 1);
}
private boolean needSplit(ByteBuffer childBuffer){
        return childBuffer.getInt(MAX_KEYS_POSITION(childBuffer.position())) > maxKeys-1 ;
}
private boolean needSplitRoot(ByteBuffer rootBuffer){
        return rootBuffer.getInt(MAX_KEYS_POSITION(rootBuffer.position()))== maxKeys;
}
private void splitRoot() {
       printTree();
        // Allocate a new buffer for the new root node
        var newRootBuffer = arena.allocate(nodeSize, Byte.class);
        var newRootNode = new BPlusTreeNode(newRootBuffer, maxKeys, nodeSize, -1);

        // Get the current root buffer
        var rootBuffer = arena.getBuffer(rootNodeOffset);
       printBuffer(rootBuffer,"old buffer");
        int numKeys = rootBuffer.getInt(MAX_KEYS_POSITION(rootBuffer.position()));

        // Determine the index to split the current root node
        int splitIndex = (numKeys + 1) / 2;

        // Allocate a new buffer for the new child node
        var newChildBuffer = arena.allocate(nodeSize, Byte.class);
        var newChildNode = new BPlusTreeNode(newChildBuffer, maxKeys, nodeSize, newRootBuffer.position());

        // Move the right half of the keys and child pointers to the new child node
        int newChildKeys = numKeys - splitIndex;
        newChildNode.setNumKeys(newChildKeys);

        for (int i = 0; i < newChildKeys; i++) {
            newChildNode.setKey(i, rootBuffer.getInt(KEY_POSITION(rootBuffer.position(), splitIndex + 1 + i)));
            newChildNode.setChildPointer(i, rootBuffer.getInt(CHILD_POSITION(rootBuffer.position(), splitIndex + 1 + i)));
        }

        // Update the number of keys in the original root node
        rootBuffer.putInt(MAX_KEYS_POSITION(rootBuffer.position()), splitIndex);
        //Update Parent of child nodes to the new root
            rootBuffer.putInt(PARENT_POSITION(rootBuffer.position(),false),newRootBuffer.position());
            newChildBuffer.putInt(PARENT_POSITION(newChildBuffer.position(),false),newRootBuffer.position());

        // Update the new root node
        newRootNode.setNumKeys(1);
        newRootNode.setKey(0, rootBuffer.getInt(KEY_POSITION(rootBuffer.position(), splitIndex)));
        newRootNode.setChildPointer(0, rootBuffer.position());
        newRootNode.setChildPointer(1, newChildBuffer.position());
        printBuffer(newRootBuffer,"new root");
        // Update pointers to the new root
        rootNodeOffset = newRootBuffer.position();
    }
private void splitChild(ByteBuffer parentNodeBuffer, ByteBuffer childNodeBuffer){
        int numKeys = parentNodeBuffer.getInt(MAX_KEYS_POSITION(parentNodeBuffer.position()));
        int childIndex = 0;

        while(childIndex<numKeys && parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(),childIndex)) != childNodeBuffer.position()){
            childIndex++;
        }

        var newChildBuffer = arena.allocate(nodeSize,Byte.class);
        var newChildNode = new BPlusTreeNode(newChildBuffer,maxKeys,nodeSize,parentNodeBuffer.position());

        int splitIndex = childNodeBuffer.getInt(MAX_KEYS_POSITION(childNodeBuffer.position())) / 2;
        for(int i = splitIndex + 1;i<childNodeBuffer.getInt(MAX_KEYS_POSITION(childNodeBuffer.position()));i++){
            newChildNode.setKey(i - splitIndex - 1,childNodeBuffer.get(KEY_POSITION(childNodeBuffer.position(),i)));
            newChildNode.setChildPointer(i-splitIndex -1, childNodeBuffer.getInt(CHILD_POSITION(childNodeBuffer.position(), i)));
        }

        childNodeBuffer.putInt(MAX_KEYS_POSITION(childNodeBuffer.position()),splitIndex + 1);
        newChildBuffer.putInt(MAX_KEYS_POSITION(newChildBuffer.position()),splitIndex);

        for(int i=numKeys; i> childIndex;i--){
            parentNodeBuffer.putInt(KEY_POSITION(parentNodeBuffer.position(),i),parentNodeBuffer.getInt(KEY_POSITION(parentNodeBuffer.position(),i-1)));
            parentNodeBuffer.putInt(CHILD_POSITION(parentNodeBuffer.position(),i), parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(),i-1)));
        }

    parentNodeBuffer.putInt(KEY_POSITION(parentNodeBuffer.position(), childIndex), childNodeBuffer.getInt(KEY_POSITION(childNodeBuffer.position(),splitIndex)));
    parentNodeBuffer.putInt(CHILD_POSITION(parentNodeBuffer.position(),childIndex), newChildBuffer.position());

    parentNodeBuffer.putInt(MAX_KEYS_POSITION(parentNodeBuffer.position()), numKeys + 1);
    if(childNodeBuffer.position()==rootNodeOffset){
        rootNodeOffset = parentNodeBuffer.position();
    }
}
public long search(int key){
        if(rootNodeOffset == -1){
            return -1;
        }
        else {
            return searchInNode(rootNodeOffset,key);
        }
}
private long searchInNode(int nodeOffset,int key){
        var nodeBuffer = arena.getBuffer(nodeOffset);

        if(isLeafNode(nodeBuffer)){
            return searchInLeaf(nodeBuffer,key);
        }
        else {
            int childOffset = findChild(nodeBuffer,key);
            return searchInNode(childOffset,key);
        }
}
private long searchInLeaf(ByteBuffer leafBuffer,int key){
        int numKeys = leafBuffer.getInt(leafBuffer.position()+4);
        for(int i=0;i<numKeys;i++){
            int currentKey = leafBuffer.getInt(KEY_POSITION(leafBuffer.position(),i));
            if(currentKey == key){
                return leafBuffer.getLong(VALUE_POSITION(leafBuffer.position(),i));
            }
        }
        return -1;
}

    public void printTree(){
        //Manage offSets to handle duplicates
        List<Integer> offSets = new ArrayList<>();
        if (rootNodeOffset == -1) {
            System.out.println("The tree is empty.");
            return;
        }

        Queue<Integer> queue = new LinkedList<>();
        queue.add(rootNodeOffset);

        while (!queue.isEmpty()) {
            int currentNodeOffset = queue.poll();
            if(offSets.contains(currentNodeOffset)){
                continue;
            }
            offSets.add(currentNodeOffset);
            ByteBuffer nodeBuffer = arena.getBuffer(currentNodeOffset);
            int numKeys = nodeBuffer.getInt(MAX_KEYS_POSITION(nodeBuffer.position()));
            // Print node keys
            System.out.print((nodeBuffer.position()==rootNodeOffset?"Root":(!isLeafNode(nodeBuffer)?"Node":"Leaf")) +" at offset " + currentNodeOffset + " [");
            for (int i = 0; i < numKeys; i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(nodeBuffer.getInt(KEY_POSITION(nodeBuffer.position(),i)));
            }
            System.out.println("]");
            //printBuffer(nodeBuffer);
            // Add child pointers to the queue for internal nodes
            if (!isLeafNode(nodeBuffer)) {
                for (int i = 0; i < numKeys+1; i++) {
                    int childOffset = nodeBuffer.getInt(CHILD_POSITION(nodeBuffer.position(),i));
                    queue.add(childOffset);
                }
            }
        }
    }
public void close(){
        arena.close();
}

//region Testing
    /**
     * For testing purposes
     * @param buffer buffer We want to ShowCase
     */
    private void printBuffer(ByteBuffer buffer,String msg){
        System.out.println(msg);
        printBuffer(buffer);
    }
    private void printBuffer(ByteBuffer buffer){
        int size = buffer.getInt(buffer.position());
        if(size == nodeSize){
            printNodeBuffer(buffer);
        }
        else{
            printLeafBuffer(buffer);
        }
}
private void printNodeBuffer(ByteBuffer nodeBuffer){
    try{
        System.out.println("NodeBuffer");
        System.out.println("OffSet: " + nodeBuffer.position());
        System.out.println("Size: " + nodeBuffer.getInt(nodeBuffer.position()));
        int numKeys = nodeBuffer.getInt(MAX_KEYS_POSITION(nodeBuffer.position()));
        System.out.println("Keys: " + numKeys);
        for(int i=0;i<numKeys;i++){
            System.out.println("Key: " + nodeBuffer.getInt(KEY_POSITION(nodeBuffer.position(),i)));
            System.out.println("Left ChildOffSet: " + nodeBuffer.getInt(CHILD_POSITION(nodeBuffer.position(),i)));
            System.out.println("Right ChildOffSet: " + nodeBuffer.getInt(CHILD_POSITION(nodeBuffer.position(),i+1)));
        }
        System.out.println("ParentOffSet: " + nodeBuffer.getInt(PARENT_POSITION(nodeBuffer.position(),false)));
        System.out.println();
    }
    catch (Exception e){
        System.out.println("Buffer is not Node");
    }
}
private void printLeafBuffer(ByteBuffer leafBuffer){
   try {
       System.out.println("LeafBuffer");
       System.out.println("OffSet: " + leafBuffer.position());
       System.out.println("Size: " + leafBuffer.getInt(leafBuffer.position()));
       int numKeys = leafBuffer.getInt(MAX_KEYS_POSITION(leafBuffer.position()));
       System.out.println("Keys && Values: " + numKeys);
       for(int i=0;i<numKeys;i++){
           System.out.println("Key: " + leafBuffer.getInt(KEY_POSITION(leafBuffer.position(),i)));
           System.out.println("Value: " + leafBuffer.getLong(VALUE_POSITION(leafBuffer.position(),i)));
       }
       System.out.println("NextLeaf: " + leafBuffer.getInt(NEXT_LEAF_POSITION(leafBuffer.position())));
       System.out.println("ParentOffSet: " + leafBuffer.getInt(PARENT_POSITION(leafBuffer.position(),true)));
   }
   catch (Exception e){
       System.out.println("Buffer is not Leaf");
   }
}
//endregion
//region Positions
    //SEGMENT_SIZE_POSITION = SEGMENT.Position()
    private int MAX_KEYS_POSITION(int bufferPosition){
        return bufferPosition + SEGMENT_SIZE;
    }
    private int KEY_POSITION(int bufferPosition,int index){
        return MAX_KEYS_POSITION(bufferPosition) +KEY_SIZE+ KEY_SIZE*index;
    }
    private int VALUE_POSITION(int bufferPosition,int index){
        return KEY_POSITION(bufferPosition,maxKeys) +VALUE_SIZE * index;
    }
    private int CHILD_POSITION(int bufferPosition,int index){
        return KEY_POSITION(bufferPosition,maxKeys) +NEXT_LEAF_POINTER_SIZE+ NEXT_LEAF_POINTER_SIZE * index;
    }
    private int PARENT_POSITION(int bufferPosition,boolean isLeaf){
        return !isLeaf?CHILD_POSITION(bufferPosition,maxKeys):NEXT_LEAF_POSITION(bufferPosition)+PARENT_POINTER;
    }
    private int NEXT_LEAF_POSITION(int bufferPosition){
        return VALUE_POSITION(bufferPosition,maxKeys);
    }
    //endregion
    // region Helper Methods

    /**
     *
     * @param numKeysCurrent num keys after change
     * @param currentNodeBuffer the buffer at hand
     * Shift the remaining keys and values in the current node to fill the gap
     */
    private void shiftLeftLeaf(int numKeysCurrent,ByteBuffer currentNodeBuffer){
        for (int i = 1; i < numKeysCurrent; i++) {
            int keyPosition = KEY_POSITION(currentNodeBuffer.position(), i);
            int newKeyPosition = KEY_POSITION(currentNodeBuffer.position(), i - 1);
            int valuePosition = VALUE_POSITION(currentNodeBuffer.position(), i );
            int newValuePosition = VALUE_POSITION(currentNodeBuffer.position(), i- 1);

            currentNodeBuffer.putInt(newKeyPosition, currentNodeBuffer.getInt(keyPosition));
            currentNodeBuffer.putLong(newValuePosition, currentNodeBuffer.getLong(valuePosition));
        }
    }
    /**
     *
     * @param numKeysCurrent num keys after change
     * @param currentNodeBuffer the buffer at hand
     * Shift the remaining keys and child in the current node to fill the gap
     */
    private void shiftLeftNode(int numKeysCurrent,ByteBuffer currentNodeBuffer){
       {
            // Shift keys to the left
            for (int i = 1; i < numKeysCurrent; i++) {
                int keyPosition = KEY_POSITION(currentNodeBuffer.position(), i);
                int newKeyPosition = KEY_POSITION(currentNodeBuffer.position(), i - 1);
                currentNodeBuffer.putInt(newKeyPosition, currentNodeBuffer.getInt(keyPosition));
            }

            // Shift child pointers to the left
            for (int i = 1; i <= numKeysCurrent; i++) {
                int childPosition = CHILD_POSITION(currentNodeBuffer.position(), i);
                int newChildPosition = CHILD_POSITION(currentNodeBuffer.position(), i - 1);
                currentNodeBuffer.putInt(newChildPosition, currentNodeBuffer.getInt(childPosition));
            }
        }
    }
    // endregion
}

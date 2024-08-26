package org.example;

import org.example.simpleArena.Arena;
import org.example.simpleArena.BPlusTreeLeafNode;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class BPlusTree {
    /**
     * Example usage of the {@link Arena} class.
     * <p>
     * {@link Arena} is an arena allocator for bytes.
     * <p>
     * Segment Structure:
     * <p>
     *   Field Name |Start Position |Bytes | Description
     *   segmentSize|     0         | 4
     *   Key Size   |     4         |  4   |
     *   Key        |     4         |  4   | depends on of MaxKeys and go by the formula (4+4 + index * 4)
     *   Value      | 4+ 4+(MaxKeys*4)+4+(index * 8)|  8   | can hold only primitive types
     *   NextPointer|4+4+(MaxKeys*4)+4|  4   | can be either nextLeafPointer and nextChildPointer
     * </p>
     * <pre>{@code
     * ByteBuffer buffer = ByteBuffer.allocate(1024);
     * buffer.put((byte) 65);
     * buffer.put((byte) 66);
     * byte firstByte = buffer.get(0);
     * }</pre>
     *
     * @see java.nio.ByteBuffer
     */
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
    //Positions
    //SEGMENT_SIZE_POSITION = SEGMENT.Position
    private int MAX_KEYS_POSITION(int bufferPosition){
     return bufferPosition + SEGMENT_SIZE;
    }
    private int KEY_POSITION(int bufferPosition,int index){
        return MAX_KEYS_POSITION(bufferPosition) + KEY_SIZE*index;
    }
    private int VALUE_POSITION(int bufferPosition,int index){
        return KEY_POSITION(bufferPosition,maxKeys) + VALUE_SIZE * index;
    }
    private int CHILD_POSITION(int bufferPosition,int index){
        return KEY_POSITION(bufferPosition,maxKeys+1) + NEXT_LEAF_POINTER_SIZE * index;
    }
    private int PARENT_POSITION(int bufferPosition,boolean isNode){
        return isNode?CHILD_POSITION(bufferPosition,maxKeys+1):VALUE_POSITION(bufferPosition,maxKeys+1);
    }



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
            System.out.println("////////////////////////////");
            printTree();
        }
}
    public void insert(int key,long value){
        if(key == 4){
            int i = 0;
        }
        if(search(key)!= -1){
            throw new IllegalArgumentException("Key must be unique!");
        }
        if(value == -1){
            throw new IllegalArgumentException("Value cannot be equal to -1!");
        }
        if(rootNodeOffset == -1){
            var rootBuffer = arena.allocate(leafNodeSize,Byte.class);
            var rootNode = new BPlusTreeLeafNode(rootBuffer,maxKeys,leafNodeSize,-1);
            rootNode.setNumKeys(1);
            rootNode.setKey(0,key);
            rootNode.setValuePointer(0,value);
            rootNodeOffset = rootBuffer.position();
        }
        else {
            var rootBuffer = arena.getBuffer(rootNodeOffset);
            if(needSplitRoot(rootBuffer) && !isLeafNode(rootBuffer)){
               var newRootBuffer = arena.allocate(nodeSize,Byte.class);
               new BPlusTreeNode(newRootBuffer,maxKeys,nodeSize,-1);
               splitChild(newRootBuffer,rootBuffer);
               rootNodeOffset = newRootBuffer.position();
;               rootBuffer.putInt(rootBuffer.position()+ SEGMENT_SIZE + KEY_SIZE +  KEY_SIZE * maxKeys + NEXT_LEAF_POINTER_SIZE * (maxKeys+1) ,newRootBuffer.position());
            }
            insertIntoNode(rootNodeOffset,key,value);

        }
}
private void insertIntoNode(int nodeOffset,int key,long value){
        var nodeBuffer = arena.getBuffer(nodeOffset);

        if(isLeafNode(nodeBuffer)){
            insetIntoLeaf(nodeBuffer,key,value);
        }
        else {

            int childOffset = findChild(nodeBuffer,key);
            var childBuffer = arena.getBuffer(childOffset);
            if(needSplit(childBuffer)){
                splitChild(nodeBuffer,childBuffer);
            }
            insertIntoNode(childOffset,key,value);
        }

}
private boolean isLeafNode(ByteBuffer nodeBuffer){
    // Logic to determine if it's a leaf node (based on structure)
    return leafNodeSize==nodeBuffer.getInt(nodeBuffer.position());

}

private int findChild(ByteBuffer nodeBBuffer,int key){
        int numKeys = nodeBBuffer.getInt(nodeBBuffer.position() + SEGMENT_SIZE);
        int index = 0;
        while(index < numKeys && key>= nodeBBuffer.getInt(4 + 4 + index * 4)){
            index++;
        }
        return  nodeBBuffer.getInt(nodeBBuffer.position() + SEGMENT_SIZE + 4 + maxKeys * 4 + index * 4);
}
private void insetIntoLeaf(ByteBuffer leafBuffer,int key,long value){
    int numKeys = leafBuffer.getInt(leafBuffer.position() +SEGMENT_SIZE);
    int index = 0;
    while (index < numKeys && key >= leafBuffer.getInt(leafBuffer.position() +SEGMENT_SIZE + MAX_KEYS_POSITION + index * 4)){
        index++;
    }

    // Shift existing keys and values to the right to make space for the new key
    for (int i = numKeys; i > index; i--) {
        int srcKeyPosition = leafBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + (i - 1) * KEY_SIZE;
        int destKeyPosition = leafBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + i * KEY_SIZE;
        int srcValuePosition = leafBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + maxKeys * KEY_SIZE + (i - 1) * VALUE_SIZE;
        int destValuePosition = leafBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + maxKeys * KEY_SIZE + i * VALUE_SIZE;

        // Shift keys
        leafBuffer.putInt(destKeyPosition, leafBuffer.getInt(srcKeyPosition));

        // Shift values
        leafBuffer.putLong(destValuePosition, leafBuffer.getLong(srcValuePosition));
    }

    //Insert the new key and value at the determined index
    leafBuffer.putInt(leafBuffer.position() +SEGMENT_SIZE + 4 + index * 4,key);
    leafBuffer.putLong(leafBuffer.position() +SEGMENT_SIZE +4 + maxKeys*4 + index * 8,value);

    //Update the number of keys in the leaf
    leafBuffer.putInt(leafBuffer.position() +KEY_SIZE,numKeys + 1);

    if(needSplit(leafBuffer)){
        splitLeaf(leafBuffer);
    }

}
private void splitLeaf(ByteBuffer leafBuffer){
        var newLeafBuffer = arena.allocate(leafNodeSize,Byte.class);
        var newLeafNode = new BPlusTreeLeafNode(newLeafBuffer,maxKeys,leafNodeSize,-1);

        int splitIndex = leafBuffer.getInt(leafBuffer.position() +KEY_SIZE) / 2;
        newLeafNode.setNumKeys(leafBuffer.getInt(leafBuffer.position() +KEY_SIZE) - splitIndex);

        for(int i=splitIndex;i<leafBuffer.getInt(leafBuffer.position() +KEY_SIZE);i++){
            newLeafNode.setKey(i-splitIndex,leafBuffer.getInt(leafBuffer.position() +SEGMENT_SIZE+MAX_KEYS_POSITION + i * KEY_SIZE));
            newLeafNode.setValuePointer(i - splitIndex,leafBuffer.getLong(leafBuffer.position() +SEGMENT_SIZE + KEY_SIZE + maxKeys * KEY_SIZE + i * VALUE_SIZE));
        }


        leafBuffer.putInt(leafBuffer.position() +KEY_SIZE,splitIndex);
        leafBuffer.putInt(leafBuffer.position() +SEGMENT_SIZE + KEY_SIZE + maxKeys * KEY_SIZE + maxKeys * VALUE_SIZE,newLeafBuffer.position());

        insertIntoParentNode(leafBuffer,newLeafNode,leafBuffer.getInt(leafBuffer.position() +SEGMENT_SIZE+ KEY_SIZE + splitIndex * KEY_SIZE));
}
private void insertIntoParentNode(ByteBuffer currentNodeBuffer,BPlusTreeLeafNode newLeafNode,int medianKey){
        int parentNodeOffset = currentNodeBuffer.getInt(currentNodeBuffer.position() +SEGMENT_SIZE + KEY_SIZE + maxKeys * KEY_SIZE + maxKeys * VALUE_SIZE + NEXT_LEAF_POINTER_SIZE);

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
    //TODO
    //check validation of the buffer positions in the new methods
private void insertIntoFullParent(ByteBuffer parentNodeBuffer,int medianKey,int newChildOffset){
    int numKeys = parentNodeBuffer.getInt(parentNodeBuffer.position() + SEGMENT_SIZE);
    int insertIndex = 0;

    // Find the index to insert the new key
    while (insertIndex < numKeys && medianKey > parentNodeBuffer.getInt(parentNodeBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + insertIndex * KEY_SIZE)) {
        insertIndex++;
    }

    // Shift keys to the right
    for (int i = numKeys; i > insertIndex; i--) {
        parentNodeBuffer.putInt(parentNodeBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + i * KEY_SIZE, parentNodeBuffer.getInt(parentNodeBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + (i - 1) * KEY_SIZE));
    }

    // Shift child pointers to the right
    for (int i = numKeys + 1; i > insertIndex + 1; i--) {
        parentNodeBuffer.putInt(parentNodeBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + maxKeys * KEY_SIZE + i * NEXT_LEAF_POINTER_SIZE, parentNodeBuffer.getInt(parentNodeBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + maxKeys * KEY_SIZE + (i - 1) * NEXT_LEAF_POINTER_SIZE));
    }

    // Insert the new key and update the child pointer
    parentNodeBuffer.putInt(parentNodeBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + insertIndex * KEY_SIZE, medianKey);
    parentNodeBuffer.putInt(parentNodeBuffer.position() + SEGMENT_SIZE + MAX_KEYS_POSITION + maxKeys * KEY_SIZE + (insertIndex + 1) * NEXT_LEAF_POINTER_SIZE, newChildOffset);

    // Update the number of keys in the parent
    parentNodeBuffer.putInt(parentNodeBuffer.position() + SEGMENT_SIZE, numKeys + 1);
}
private boolean needSplit(ByteBuffer childBuffer){
        return childBuffer.getInt(childBuffer.position()+KEY_SIZE) > maxKeys-1 ;
}
private boolean needSplitRoot(ByteBuffer rootBuffer){
        return rootBuffer.getInt(rootBuffer.position()+KEY_SIZE)>= maxKeys-1;
}
private void splitChild(ByteBuffer parentNodeBuffer, ByteBuffer childNodeBuffer){
        int numKeys = parentNodeBuffer.getInt(parentNodeBuffer.position()+MAX_KEYS_POSITION);
        int childIndex = 0;

        while(childIndex<numKeys && parentNodeBuffer.getInt(parentNodeBuffer.position()+MAX_KEYS_POSITION + KEY_SIZE + maxKeys * KEY_SIZE + childIndex *PARENT_POINTER) != childNodeBuffer.position()){
            childIndex++;
        }

        var newChildBuffer = arena.allocate(nodeSize,Byte.class);
        var newChildNode = new BPlusTreeNode(newChildBuffer,maxKeys,nodeSize,parentNodeBuffer.position());

        int splitIndex = childNodeBuffer.getInt(childNodeBuffer.position()+KEY_SIZE) / 2;
        for(int i = splitIndex + 1;i<childNodeBuffer.getInt(childNodeBuffer.position()+4);i++){
            newChildNode.setKey(i - splitIndex - 1,childNodeBuffer.get(childNodeBuffer.position()+4+ 4 + i*4));
            newChildNode.setChildPointer(i-splitIndex -1, childNodeBuffer.getInt(childNodeBuffer.position()+4 + 4 + maxKeys * 4 + i * 4));
        }

        childNodeBuffer.putInt(childNodeBuffer.position()+4,splitIndex + 1);
        newChildBuffer.putInt(newChildBuffer.position()+MAX_KEYS_POSITION,splitIndex);
        if(childNodeBuffer.position() == rootNodeOffset)
            printBuffer(newChildBuffer);

        for(int i=numKeys; i> childIndex;i--){
            parentNodeBuffer.putInt(parentNodeBuffer.position()+4 + 4 + i * 4,parentNodeBuffer.getInt(parentNodeBuffer.position()+4 + (i -1) * 4));
            parentNodeBuffer.putInt(parentNodeBuffer.position()+4+4 + maxKeys * 4 + i * 4, parentNodeBuffer.getInt(parentNodeBuffer.position()+4+4 + maxKeys * 4 + (i - 1) * 4));
        }

    parentNodeBuffer.putInt(parentNodeBuffer.position()+4 +4 + childIndex * 4, childNodeBuffer.getInt(childNodeBuffer.position()+4 + splitIndex * 4));
    parentNodeBuffer.putInt(parentNodeBuffer.position()+4 + 4 + maxKeys * 4 + childIndex * 4, newChildBuffer.position());

    parentNodeBuffer.putInt(parentNodeBuffer.position()+4, numKeys + 1);
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
            int currentKey = leafBuffer.getInt(leafBuffer.position()+4 + 4 + i * 4);
            if(currentKey == key){
                return leafBuffer.getLong(leafBuffer.position()+4 + 4 + maxKeys * 4 + i * 8);
            }
        }
        return -1;
}
public void delete(int key){
        if(rootNodeOffset == -1){
            return;
        }
        else {
            deleteFromNode(rootNodeOffset,key);
        }
}
private void deleteFromNode(int nodeOffset,int key){
        var nodeBuffer = arena.getBuffer(nodeOffset);

        if(isLeafNode(nodeBuffer)){
            deleteFromLeaf(nodeBuffer,key);
        }
        else{
            int childOffset = findChild(nodeBuffer,key);
            deleteFromNode(childOffset,key);

            var childBuffer = arena.getBuffer(nodeOffset);
            if(needResitributeOrMerge(childBuffer)){
                redistributeOrMerge(nodeBuffer,childBuffer,childOffset);
            }
        }
}
private void deleteFromLeaf(ByteBuffer leafBuffer,int key){
        int numKeys = leafBuffer.getInt(4);
        for(int i = 0; i < numKeys; i++){
            int currentKEy = leafBuffer.getInt(4 + 4 + i * 4);
            if(currentKEy == key){
                shiftKeyAndValuesLeft(leafBuffer,i,numKeys);
                leafBuffer.putInt(4,numKeys-1);
                return;
            }
        }
}
private void shiftKeyAndValuesLeft(ByteBuffer leafBuffer,int index, int numKeys){
        for(int i=index; i<numKeys - 1 ;i++){
            leafBuffer.putInt(4 + 4 + i*4,leafBuffer.getInt(4 + 4 +(i+1)*4));
            leafBuffer.putLong(4 + 4 + maxKeys * 4 + i * 8,leafBuffer.getLong(4 + 4 + maxKeys * 4 + (i + 1)*8));
        }
}
private boolean needResitributeOrMerge(ByteBuffer childBuffer){
        return childBuffer.get(4)< (maxKeys + 1) / 2;
}
private void redistributeOrMerge(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,int childOffset){
        int numKeys = parentNodeBuffer.get(4);
        int childIndex = 0;
        while (childIndex< numKeys && parentNodeBuffer.getInt(4 + 4 + maxKeys * 4 + childIndex * 4) != childOffset){
            childIndex++;
        }

        if(childIndex > 0){
            var leftSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(4 + maxKeys * 4 + (childIndex -1) * 4));
            if(leftSiblingBuffer.getInt(4) > (maxKeys + 1)/2){
                redistributeFromLeftSibling(parentNodeBuffer,childBuffer,leftSiblingBuffer,childIndex);
                return;
            }
        }

        if(childIndex< numKeys -1){
            var rightSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(4 + 4 + maxKeys * 4 + (childIndex + 1) * 4));
            if(rightSiblingBuffer.getInt(4) > (maxKeys + 1)/2){
                redistributeFromRightSibling(parentNodeBuffer,childBuffer,rightSiblingBuffer,childIndex);
                return;
            }
        }

    if(childIndex > 0){
        var leftSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(4 + maxKeys * 4 + (childIndex -1) * 4));
        mergeWithLeftSibling(parentNodeBuffer,childBuffer,childOffset,leftSiblingBuffer,childIndex);
    }
    else {
        var rightSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(4 + 4 + maxKeys * 4 + (childIndex + 1) * 4));
        mergeWithRightSibling(parentNodeBuffer,childBuffer,childOffset,rightSiblingBuffer,childIndex);
    }
}

private void redistributeFromLeftSibling(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,ByteBuffer leftSiblingBuffer,int childIndex){
       int numKeys = childBuffer.getInt(4);
       int numLeftKeys = leftSiblingBuffer.get(4);

       int newKey = parentNodeBuffer.getInt(4 + 4 + (childIndex - 1) * 4);
       int newPointer = parentNodeBuffer.getInt(4 + 4 + maxKeys * 4 + (childIndex -1) * 4);

       for(int i= numKeys -1;i>= 0 ; i--){
           childBuffer.putInt(4 + 4+ (i + 1) * 4,childBuffer.getInt(4 + 4 + i * 4));
           childBuffer.putInt(4 + 4 + maxKeys * 4 + (i + 1) *4,childBuffer.getInt(4 + 4 + maxKeys * 4 + i * 4));
       }

       childBuffer.putInt(4 + 4,newKey);
       childBuffer.putInt(4 + 4 + maxKeys * 4,newPointer);

    parentNodeBuffer.putInt(4+ 4 + (childIndex - 1) * 4, leftSiblingBuffer.getInt(4+4 + (numLeftKeys - 1) * 4));
    parentNodeBuffer.putLong(4 +4 + maxKeys * 4 + (childIndex - 1) * 4, leftSiblingBuffer.getLong(4 + 4 + maxKeys * 4 + (numLeftKeys - 1) * 4));


       leftSiblingBuffer.putInt(4,numLeftKeys-1);
       childBuffer.putInt(4,numKeys+1);
}
    private void redistributeFromRightSibling(ByteBuffer parentNodeBuffer, ByteBuffer childBuffer, ByteBuffer rightSiblingBuffer, int childIndex) {
        int numKeys = childBuffer.getInt(4);
        int numLeftKeys = rightSiblingBuffer.get(4);

        int newKey = parentNodeBuffer.getInt(4 + 4 + (childIndex - 1) * 4);
        int newPointer = parentNodeBuffer.getInt(4 + 4 + maxKeys * 4 + (childIndex -1) * 4);


        childBuffer.putInt(4 + 4,newKey);
        childBuffer.putInt(4 + 4 + maxKeys * 4,newPointer);

        parentNodeBuffer.putInt(4+ 4 + (childIndex - 1) * 4, rightSiblingBuffer.getInt(4+4 + (numLeftKeys - 1) * 4));
        parentNodeBuffer.putLong(4 +4 + maxKeys * 4 + (childIndex - 1) * 4, rightSiblingBuffer.getLong(4 + 4 + maxKeys * 4 + (numLeftKeys - 1) * 4));


        rightSiblingBuffer.putInt(4,numLeftKeys-1);
        childBuffer.putInt(4,numKeys+1);
    }
    private void mergeWithLeftSibling(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,int childOffset,ByteBuffer leftSiblingBuffer,int childIndex){
        int numKeys = childBuffer.getInt(4);
        int numRightKeys = leftSiblingBuffer.getInt(4);

        childBuffer.putInt(4,numKeys + numRightKeys + 1);

        for(int i=0;i<numRightKeys; i++){
            childBuffer.putInt(4+4 + (numKeys + 1) * 4,leftSiblingBuffer.getInt(4 + i * 4));
            childBuffer.putInt(4 +4 + maxKeys* 4 + (maxKeys + i) * 4,leftSiblingBuffer.getInt(4+4+maxKeys*4 + i * 4));
        }

        parentNodeBuffer.putInt(4 + 4 + maxKeys * 4 + (childIndex - 1) * 4,childOffset);
        parentNodeBuffer.putInt(4,parentNodeBuffer.getInt(4)-1);
    }

    private void mergeWithRightSibling(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,int childOffset,ByteBuffer rightSiblingBuffer,int childIndex){
        int numKeys = childBuffer.getInt(4);
        int numRightKeys = rightSiblingBuffer.getInt(4);

        childBuffer.putInt(4, numKeys + numRightKeys + 1);

        for (int i = 0; i < numRightKeys; i++) {
            childBuffer.putInt(4+ 4 + (numKeys + i) * 4, rightSiblingBuffer.getInt(4+4 + i * 4));
            childBuffer.putLong(4 +4 + maxKeys * 4 + (numKeys + i) * 4, rightSiblingBuffer.getInt(4 +4 + maxKeys * 4 + i * 4));
        }

        parentNodeBuffer.putInt(4+4 + childIndex * 4, rightSiblingBuffer.getInt(4));
        parentNodeBuffer.putLong(4+4 + maxKeys * 4 + childIndex * 4, rightSiblingBuffer.get(4+4 + maxKeys * 4));
        parentNodeBuffer.putInt(4, parentNodeBuffer.getInt(0) - 1);
    }
    public void printTree(){
        if (rootNodeOffset == -1) {
            System.out.println("The tree is empty.");
            return;
        }

        Queue<Integer> queue = new LinkedList<>();
        queue.add(rootNodeOffset);

        while (!queue.isEmpty()) {
            int currentNodeOffset = queue.poll();
            ByteBuffer nodeBuffer = arena.getBuffer(currentNodeOffset);
            int numKeys = nodeBuffer.getInt(nodeBuffer.position() + MAX_KEYS_POSITION);

            // Print node keys
            System.out.print((!isLeafNode(nodeBuffer)?"Node":"Leaf") +" at offset " + currentNodeOffset + " [");
            for (int i = 0; i < numKeys; i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(nodeBuffer.getInt(nodeBuffer.position() +SEGMENT_SIZE +MAX_KEYS_POSITION + i * KEY_SIZE));
            }
            System.out.println("]");

            // Add child pointers to the queue for internal nodes
            if (!isLeafNode(nodeBuffer)) {
                for (int i = 0; i < numKeys+1; i++) {
                    int childOffset = nodeBuffer.getInt(nodeBuffer.position()+4 +4 + maxKeys * 4 + i * 4);
                    queue.add(childOffset);
                }
            }
        }
    }
public void close(){
        arena.close();
}

    /**
     * For testing purposes
     * @param buffer buffer We want to ShowCase
     */
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
        int numKeys = nodeBuffer.getInt(nodeBuffer.position() + SEGMENT_SIZE);
        System.out.println("Keys: " + numKeys);
        for(int i=0;i<numKeys;i++){
            System.out.println("Key: " + nodeBuffer.getInt(nodeBuffer.position() + SEGMENT_SIZE + KEY_SIZE + KEY_SIZE * i));
            System.out.println("Left ChildOffSet: " + nodeBuffer.getInt(nodeBuffer.position() + SEGMENT_SIZE + KEY_SIZE +  KEY_SIZE * maxKeys + NEXT_LEAF_POINTER_SIZE * i ));
            System.out.println("Right ChildOffSet: " + nodeBuffer.getInt(nodeBuffer.position() + SEGMENT_SIZE + KEY_SIZE +  KEY_SIZE * maxKeys + NEXT_LEAF_POINTER_SIZE * (i + 1) ));
        }
        System.out.println("ParentOffSet: " + nodeBuffer.getInt(nodeBuffer.position()+ SEGMENT_SIZE + KEY_SIZE +  KEY_SIZE * maxKeys + NEXT_LEAF_POINTER_SIZE * (maxKeys+1)));
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
       int numKeys = leafBuffer.getInt(leafBuffer.position() + SEGMENT_SIZE);
       System.out.println("Keys && Values: " + numKeys);
       for(int i=0;i<numKeys;i++){
           System.out.println("Key: " + leafBuffer.getInt(leafBuffer.position() + SEGMENT_SIZE + KEY_SIZE + KEY_SIZE * i));
           System.out.println("Value: " + leafBuffer.getLong(leafBuffer.position() + SEGMENT_SIZE + KEY_SIZE +  KEY_SIZE * maxKeys + VALUE_SIZE * i ));
       }
       //buffer.position() + SEGMENT_SIZE +KEY_SIZE + maxKeys * KEY_SIZE + maxKeys * VALUE_POINTER_SIZE
       System.out.println("NextLeaf: " + leafBuffer.getInt(leafBuffer.position()+ SEGMENT_SIZE + KEY_SIZE +  KEY_SIZE * maxKeys + VALUE_SIZE * maxKeys));
       System.out.println("ParentOffSet: " + leafBuffer.getInt(leafBuffer.position()+ SEGMENT_SIZE + KEY_SIZE +  KEY_SIZE * maxKeys + VALUE_SIZE * maxKeys + NEXT_LEAF_POINTER_SIZE));
   }
   catch (Exception e){
       System.out.println("Buffer is not Leaf");
   }
}
}

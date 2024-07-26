package org.example;

import org.example.simpleArena.Arena;
import org.example.simpleArena.BPlusTreeLeafNode;

import java.nio.ByteBuffer;
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
     *   Value      | 4+ 4+(MaxKeys*4)+8+(index * 8)|  8   | can hold only primitive types
     *   NextPointer|4+4+(MaxKeys*4)*8|  8   | can be either nextLeafPointer and nextChildPointer
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
    private final Arena arena;
    private int rootNodeOffset;
    private final int nodeSize;
    private final int leafNodeSize;
    private final int maxKeys;

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
        return 4 + maxKeys * (4 + 8); // Number of keys + (key size + child pointer size)
    }
    // Calculate size of leaf node
    private int calculateLeafNodeSize() {
        return 4 + maxKeys * (4 + 8) + 8; // Number of keys + (key size + value pointer size) + next leaf pointer size
    }
public void insert(int key,long value){
        if(rootNodeOffset == -1){
            var rootBuffer = arena.allocate(leafNodeSize,Byte.class);
            var rootNode = new BPlusTreeLeafNode(rootBuffer,maxKeys,leafNodeSize);
            rootNode.setNumKeys(1);
            rootNode.setKey(0,key);
            rootNode.setValuePointer(0,value);
            rootNode.setNextLeafPointer(-1);//No nex leaf initially
            rootNodeOffset = rootBuffer.position();
        }
        else {
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
            insertIntoNode(childOffset,key,value);

            var childBuffer = arena.getBuffer(childOffset);
            if(needSplit(childBuffer)){
                splitChild(nodeBuffer,childBuffer);
            }
        }

}
private boolean isLeafNode(ByteBuffer nodeBuffer){
    // Logic to determine if it's a leaf node (based on structure)
    return nodeBuffer.getInt(nodeBuffer.position() + 4 + 4 + maxKeys * 4) != -1;
}

private int findChild(ByteBuffer nodeBBuffer,int key){
        int numKeys = nodeBBuffer.getInt(4);
        int index = 0;
        while(index < numKeys && key>= nodeBBuffer.getInt(4 + 4 + index * 4)){
            index++;
        }
        return  nodeBBuffer.getInt(4 + 4 + maxKeys * 4 + index * 8);
}
private void insetIntoLeaf(ByteBuffer leafBuffer,int key,long value){
    int numKeys = leafBuffer.getInt(4);
    int index = 0;
    while (index < numKeys && key >= leafBuffer.getInt(4 + index * 4)){
        index++;
    }

    leafBuffer.putInt(4 + 4 + index * 4,key);
    leafBuffer.putLong(4 +4 + maxKeys*4 + index * 8,value);
    leafBuffer.putInt(4,numKeys + 1);

}
private boolean needSplit(ByteBuffer childBuffer){
        return childBuffer.getInt(0) > maxKeys;
}
private void splitChild(ByteBuffer parentNodeBuffer, ByteBuffer childNodeBuffer){
        int numKeys = parentNodeBuffer.getInt(4);
        int childIndex = 0;

        while(childIndex<numKeys && parentNodeBuffer.getInt(4 + 4 + maxKeys * 4 + childIndex *4) != childNodeBuffer.position()){
            childIndex++;
        }

        var newChildBuffer = arena.allocate(nodeSize,Byte.class);
        var newChildNode = new BPlusTreeNode(newChildBuffer,maxKeys,nodeSize);

        int splitIndex = childNodeBuffer.getInt(4) / 2;
        for(int i = splitIndex + 1;i<childNodeBuffer.getInt(4);i++){
            newChildNode.setKey(i - splitIndex - 1,childNodeBuffer.get(4+ 4 + i*4));
            newChildNode.setChildPointer(i-splitIndex -1, childNodeBuffer.getInt(4 + 4 + maxKeys * 4 + i * 4));
        }

        childNodeBuffer.putInt(4,splitIndex + 1);
        newChildBuffer.putInt(4,newChildBuffer.getInt(0) - splitIndex - 1);

        for(int i=numKeys; i> childIndex;i--){
            parentNodeBuffer.putInt(4 + 4 + i * 4,parentNodeBuffer.getInt(4 + (i -1) * 4));
            parentNodeBuffer.putInt(4+4 + maxKeys * 4 + i * 4, parentNodeBuffer.getInt(4+4 + maxKeys * 4 + (i - 1) * 4));
        }

    parentNodeBuffer.putInt(4 +4 + childIndex * 4, childNodeBuffer.getInt(4 + splitIndex * 4));
    parentNodeBuffer.putInt(4 + 4 + maxKeys * 4 + childIndex * 4, newChildBuffer.position());

    parentNodeBuffer.putInt(4, numKeys + 1);
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
        int numKeys = leafBuffer.getInt(4);
        for(int i=0;i<numKeys;i++){
            int currentKey = leafBuffer.getInt(4 + 4 + i * 4);
            if(currentKey == key){
                return leafBuffer.getLong(4 + 4 + maxKeys * 4 + i * 8);
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
            int numKeys = nodeBuffer.getInt(4);

            // Print node keys
            System.out.print("Node at offset " + currentNodeOffset + " [");
            for (int i = 0; i < numKeys; i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(nodeBuffer.getInt(4 +4 + i * 4));
            }
            System.out.println("]");

            // Add child pointers to the queue for internal nodes
            if (!isLeafNode(nodeBuffer)) {
                for (int i = 0; i <= numKeys; i++) {
                    int childOffset = nodeBuffer.getInt(4 +4 + maxKeys * 4 + i * 4);
                    queue.add(childOffset);
                }
            }
        }
    }
public void close(){
        arena.close();
}

}

package org.example.oldVersion.simpleArena;

public class tempMethods {
    //public void delete(int key){
//        if(rootNodeOffset == -1){
//            return;
//        }
//        else {
//            deleteFromNode(rootNodeOffset,key);
//        }
//}
//private void deleteFromNode(int nodeOffset,int key){
//        var nodeBuffer = arena.getBuffer(nodeOffset);
//
//        if(isLeafNode(nodeBuffer)){
//            deleteFromLeaf(nodeBuffer,key);
//        }
//        else{
//            int childOffset = findChild(nodeBuffer,key);
//            deleteFromNode(childOffset,key);
//
//            var childBuffer = arena.getBuffer(nodeOffset);
//            if(needRedistributeOrMerge(childBuffer)){
//                redistributeOrMerge(nodeBuffer,childBuffer,childOffset);
//            }
//        }
//}
//private void deleteFromLeaf(ByteBuffer leafBuffer,int key){
//        int numKeys = leafBuffer.getInt(MAX_KEYS_POSITION(leafBuffer.position()));
//        for(int i = 0; i < numKeys; i++){
//            int currentKey = leafBuffer.getInt(KEY_POSITION(leafBuffer.position(),i));
//            if(currentKey == key){
//                shiftKeyAndValuesLeft(leafBuffer,i,numKeys);
//                leafBuffer.putInt(MAX_KEYS_POSITION(leafBuffer.position()),numKeys-1);
//                return;
//            }
//        }
//}
//private void shiftKeyAndValuesLeft(ByteBuffer leafBuffer,int index, int numKeys){
//        for(int i=index; i<numKeys - 1 ;i++){
//            leafBuffer.putInt(KEY_POSITION(leafBuffer.position(),i),leafBuffer.getInt(KEY_POSITION(leafBuffer.position(),i+1)));
//            leafBuffer.putLong(VALUE_POSITION(leafBuffer.position(),i),leafBuffer.getLong(VALUE_POSITION(leafBuffer.position(),i+1)));
//        }
//}
//private boolean needRedistributeOrMerge(ByteBuffer childBuffer){
//        return childBuffer.get(MAX_KEYS_POSITION(childBuffer.position()))< (maxKeys + 1) / 2;
//}
//private void redistributeOrMerge(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,int childOffset){
//        int numKeys = parentNodeBuffer.get(MAX_KEYS_POSITION(parentNodeBuffer.position()));
//        int childIndex = 0;
//        while (childIndex< numKeys && parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(), childIndex)) != childOffset){
//            childIndex++;
//        }
//
//        if(childIndex > 0){
//            var leftSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(), childIndex-1)));
//            if(leftSiblingBuffer.getInt(MAX_KEYS_POSITION(leftSiblingBuffer.position())) > (maxKeys + 1)/2){
//                redistributeFromLeftSibling(parentNodeBuffer,childBuffer,leftSiblingBuffer,childIndex);
//                return;
//            }
//        }
//
//        if(childIndex< numKeys -1){
//            var rightSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(), childIndex+1)));
//            if(rightSiblingBuffer.getInt(MAX_KEYS_POSITION(rightSiblingBuffer.position())) > (maxKeys + 1)/2){
//                redistributeFromRightSibling(parentNodeBuffer,childBuffer,rightSiblingBuffer,childIndex);
//                return;
//            }
//        }
//
//    if(childIndex > 0){
//        var leftSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(), childIndex-1)));
//        mergeWithLeftSibling(parentNodeBuffer,childBuffer,childOffset,leftSiblingBuffer,childIndex);
//    }
//    else {
//        var rightSiblingBuffer = arena.getBuffer(parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position() ,childIndex+1)));
//        mergeWithRightSibling(parentNodeBuffer,childBuffer,childOffset,rightSiblingBuffer,childIndex);
//    }
//}
//
//private void redistributeFromLeftSibling(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,ByteBuffer leftSiblingBuffer,int childIndex){
//       int numKeys = childBuffer.getInt(MAX_KEYS_POSITION(childBuffer.position()));
//       int numLeftKeys = leftSiblingBuffer.get(MAX_KEYS_POSITION(leftSiblingBuffer.position()));
//
//       int newKey = parentNodeBuffer.getInt(KEY_POSITION(parentNodeBuffer.position(), childIndex-1));
//       int newPointer = parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(), childIndex-1));
//
//       for(int i= numKeys -1;i>= 0 ; i--){
//           childBuffer.putInt(KEY_POSITION(childBuffer.position(),i+1),childBuffer.getInt(KEY_POSITION(childBuffer.position(),i)));
//           childBuffer.putInt(CHILD_POSITION(childBuffer.position(),i+1),childBuffer.getInt(CHILD_POSITION(childBuffer.position(),i)));
//       }
//
//       childBuffer.putInt(KEY_POSITION(childBuffer.position(), 1),newKey);
//       childBuffer.putInt(CHILD_POSITION(childBuffer.position(),1),newPointer);
//
//    parentNodeBuffer.putInt(KEY_POSITION(parentNodeBuffer.position(), childIndex-1), leftSiblingBuffer.getInt(KEY_POSITION(leftSiblingBuffer.position(),numLeftKeys-1)));
//    parentNodeBuffer.putInt(CHILD_POSITION(parentNodeBuffer.position(), childIndex-1), leftSiblingBuffer.getInt(CHILD_POSITION(leftSiblingBuffer.position(),numLeftKeys-1)));
//
//
//       leftSiblingBuffer.putInt(MAX_KEYS_POSITION(leftSiblingBuffer.position()),numLeftKeys-1);
//       childBuffer.putInt(MAX_KEYS_POSITION(childBuffer.position()),numKeys+1);
//}
//    private void redistributeFromRightSibling(ByteBuffer parentNodeBuffer, ByteBuffer childBuffer, ByteBuffer rightSiblingBuffer, int childIndex) {
//        int numKeys = childBuffer.getInt(MAX_KEYS_POSITION(childBuffer.position()));
//        int numLeftKeys = rightSiblingBuffer.get(MAX_KEYS_POSITION(rightSiblingBuffer.position()));
//
//        int newKey = parentNodeBuffer.getInt(KEY_POSITION(parentNodeBuffer.position(), childIndex - 1));
//        int newPointer = parentNodeBuffer.getInt(CHILD_POSITION(parentNodeBuffer.position(), childIndex-1));
//
//
//        childBuffer.putInt(4 + 4,newKey);
//        childBuffer.putInt(4 + 4 + maxKeys * 4,newPointer);
//
//        parentNodeBuffer.putInt(4+ 4 + (childIndex - 1) * 4, rightSiblingBuffer.getInt(4+4 + (numLeftKeys - 1) * 4));
//        parentNodeBuffer.putLong(4 +4 + maxKeys * 4 + (childIndex - 1) * 4, rightSiblingBuffer.getLong(4 + 4 + maxKeys * 4 + (numLeftKeys - 1) * 4));
//
//
//        rightSiblingBuffer.putInt(4,numLeftKeys-1);
//        childBuffer.putInt(4,numKeys+1);
//    }
//    private void mergeWithLeftSibling(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,int childOffset,ByteBuffer leftSiblingBuffer,int childIndex){
//        int numKeys = childBuffer.getInt(4);
//        int numRightKeys = leftSiblingBuffer.getInt(4);
//
//        childBuffer.putInt(4,numKeys + numRightKeys + 1);
//
//        for(int i=0;i<numRightKeys; i++){
//            childBuffer.putInt(4+4 + (numKeys + 1) * 4,leftSiblingBuffer.getInt(4 + i * 4));
//            childBuffer.putInt(4 +4 + maxKeys* 4 + (maxKeys + i) * 4,leftSiblingBuffer.getInt(4+4+maxKeys*4 + i * 4));
//        }
//
//        parentNodeBuffer.putInt(4 + 4 + maxKeys * 4 + (childIndex - 1) * 4,childOffset);
//        parentNodeBuffer.putInt(4,parentNodeBuffer.getInt(4)-1);
//    }
//
//    private void mergeWithRightSibling(ByteBuffer parentNodeBuffer,ByteBuffer childBuffer,int childOffset,ByteBuffer rightSiblingBuffer,int childIndex){
//        int numKeys = childBuffer.getInt(4);
//        int numRightKeys = rightSiblingBuffer.getInt(4);
//
//        childBuffer.putInt(4, numKeys + numRightKeys + 1);
//
//        for (int i = 0; i < numRightKeys; i++) {
//            childBuffer.putInt(4+ 4 + (numKeys + i) * 4, rightSiblingBuffer.getInt(4+4 + i * 4));
//            childBuffer.putLong(4 +4 + maxKeys * 4 + (numKeys + i) * 4, rightSiblingBuffer.getInt(4 +4 + maxKeys * 4 + i * 4));
//        }
//
//        parentNodeBuffer.putInt(4+4 + childIndex * 4, rightSiblingBuffer.getInt(4));
//        parentNodeBuffer.putLong(4+4 + maxKeys * 4 + childIndex * 4, rightSiblingBuffer.get(4+4 + maxKeys * 4));
//        parentNodeBuffer.putInt(4, parentNodeBuffer.getInt(0) - 1);
//    }
}

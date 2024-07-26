import org.example.BPlusTree;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BPlusTreeTest {
   @Test
    public void should_create_tree(){

      var tree = new BPlusTree();
      tree.printTree();
   }
   @Test
    public void should_fail_to_create_tree_on_small_order(){
       assertThrows(IllegalArgumentException.class,()->new BPlusTree(1000,2));
   }
   @Test
   public void should_find_insertion(){
      var tree = new BPlusTree();
      //tree.insert(3);
      //assertTrue(tree.search(3));
   }
   @Test
   public void should_notFind_insertion(){
      var tree = new BPlusTree();
      //tree.insert(3);
      //assertFalse(tree.search(4));
   }
   @Test
   public void should_inert_many(){
      var tree = new BPlusTree();
      //tree.insertMany(new ArrayList<>(List.of(1,2,3)));
   }
   @Test
   public  void should_split_root(){
      var tree = new  BPlusTree();
      //tree.insertMany(new ArrayList<>(List.of(1,2,3,4,5,6,7,8,9)));
   }
}

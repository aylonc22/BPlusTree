import org.example.oldVersion.BPlusTree;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class BPlusTreeTest {
   @Test
    public void should_create_tree(){

      var tree = new BPlusTree();
      tree.insert(3,4);
      tree.printTree();
   }
   @Test
   public void should_fail_to_insert_existing_key(){
      var tree = new BPlusTree();
      tree.insert(1,1);
      assertThrows(IllegalArgumentException.class,()->tree.insert(1,4));
   }
   @Test
   public void should_fail_to_insert_negative_1(){
      var tree = new BPlusTree();
      assertThrows(IllegalArgumentException.class,()->tree.insert(1,-1));
   }
   @Test
    public void should_fail_to_create_tree_on_small_order(){
       assertThrows(IllegalArgumentException.class,()->new BPlusTree(1000,2));
   }
   @Test
   public void should_find_insertion(){
      var tree = new BPlusTree();
      tree.insert(3,4);
      assertEquals(4,tree.search(3));
   }
   @Test
   public void should_notFind_insertion(){
      var tree = new BPlusTree();
      tree.insert(3,4);
      assertNotEquals(0,tree.search(2));
   }
   @Test
   public void should_fix_order_in_leaf(){
      var tree = new BPlusTree();
      tree.insert(3,4);
      tree.insert(2,4);
      tree.printTree();
   }
   @Test
   public void should_inert_many(){
      var tree = new BPlusTree();
      var items = new HashMap<Integer,Long>();
      for(int i = 0;i<10;i++){
         items.put(i,50 + (long)(Math.random() * 51));
      }
      tree.insertMany(items);
      tree.printTree();
   }
   @Test
   public  void should_split_root(){
      var tree = new BPlusTree();
      var items = new HashMap<Integer,Long>();
      for(int i = 0;i<4;i++){
         items.put(i,50 + (long)(Math.random() * 51));
      }
      tree.insertMany(items);
      tree.printTree();
   }
}

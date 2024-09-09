
import org.example.BPlusTree;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class BPlusTreeTest {
   @Test
    public void should_create_tree(){

      var tree = new BPlusTree();
      tree.insert(3,"4");
      tree.printTree(tree.root,"test");
   }
   @Test
    public void should_fail_to_create_tree_on_small_order(){
       assertThrows(IllegalArgumentException.class,()->new BPlusTree(1000,2));
   }
   @Test
   public void should_fail_to_create_tree_on_small_memory(){
      assertThrows(IllegalArgumentException.class,()->new BPlusTree(0,3));
   }
   @Test
   public void should_find_insertion(){
      var tree = new BPlusTree();
      tree.insert(3,"4");
      assertEquals("4",tree.search(3));
   }
   @Test
   public void should_notFind_insertion(){
      var tree = new BPlusTree();
      tree.insert(3,"4");
      assertNotEquals(0,tree.search(2));
   }
   @Test
   public void should_fix_order_in_leaf(){
      var tree = new BPlusTree();
      tree.insert(3,"4");
      tree.insert(2,"4");
      tree.printTree(tree.root,"");
   }
   @Test
   public void should_inert_many(){
      var tree = new BPlusTree();
      var items = new HashMap<Integer,String>();
      for(int i = 0;i<10;i++){
         items.put(i, String.valueOf((int)(50 + Math.random() * 51)));
      }
      tree.insertMany(items);
      tree.printTree(tree.root,"");
   }
   @Test
   public  void should_split_root(){
      var tree = new BPlusTree();
      var items = new HashMap<Integer,String>();
      for(int i = 0;i<4;i++){
         items.put(i, String.valueOf((int)(50 + Math.random() * 51)));
      }
      tree.insertMany(items);
      tree.printTree(tree.root,"");
   }
   @Test
   public void should_delete_from_root(){
      var tree = new BPlusTree();
      tree.insert(0,"test");
      tree.insert(1,"test2");

      tree.delete(1);
      tree.printTree(tree.root,"");

   }
   @Test
   public void should_handle_under_flow_from_left_borrow(){
      var tree = new BPlusTree();
      tree.insert(0,"test");
      tree.insert(1,"test2");
      tree.insert(2,"test");

      tree.delete(2);
      tree.printTree(tree.root,"");
   }
   @Test
   public void should_handle_under_flow_from_left_merge(){
      var tree = new BPlusTree();
      tree.insert(0,"test");
      tree.insert(1,"test2");
      tree.insert(2,"test");

      tree.delete(2);
      tree.delete(1);
      tree.printTree(tree.root,"");

   }

   @Test
   public void should_handle_under_flow_from_right_borrow(){
      var tree = new BPlusTree();
      var items = new HashMap<Integer,String>();
      for(int i = 0;i<5;i++){
         items.put(i, String.valueOf((int)(50 + Math.random() * 51)));
      }
      tree.insertMany(items);

      tree.delete(4);
      tree.printTree(tree.root,"");
   }
}

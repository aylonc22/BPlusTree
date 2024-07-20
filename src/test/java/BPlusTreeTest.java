import org.example.BPlusTree;
import org.example.BPlusTreeNode;
import org.junit.Test;
import static org.junit.Assert.*;

public class BPlusTreeTest {
   @Test
    public void should_create_tree(){
       var tree = new BPlusTree(3);
       tree.insert(10 );
       tree.insert(11 );
       tree.printTree();
   }
   @Test
    public void should_fail_to_create_tree_on_small_order(){
       assertThrows(IllegalArgumentException.class,()->new BPlusTree(2));
   }
}

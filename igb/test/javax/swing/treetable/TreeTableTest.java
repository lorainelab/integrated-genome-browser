package javax.swing.treetable;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;

public class TreeTableTest {

    public static void main(String[] args)  {
      //	treeTable.getColumnModel().getColumn(1).setCellRenderer(new IndicatorRenderer());
      //	treeTable.getTree().addTreeExpansionListener(rl);
      JFrame frame = new JFrame("TreeTableTest");
      frame.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent we) {
	    System.exit(0);
	  }
	});


      /*
	TreeTableModel model = new TestTreeTableModel();
	JTreeTable  treetable = new JTreeTable(model);

      Container cpane = frame.getContentPane();
      cpane.add(new JScrollPane(treetable));
      frame.pack();
      frame.show();
      */
    }


  /*
  class TestTreeTableModel extends AbstractTreeTableModel  {
    TestTreeTableModel() {
      
    }
  }
  */

}

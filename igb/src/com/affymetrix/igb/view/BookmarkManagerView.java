/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.view;

import com.affymetrix.swing.dnd.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.bookmarks.*;
import com.affymetrix.igb.menuitem.BookMarkAction;
import com.affymetrix.igb.parsers.BookmarksParser;
import com.affymetrix.igb.prefs.IPlugin;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

/**
 *  A panel for viewing and re-arranging bookmarks in a heirarchy.
 */
public class BookmarkManagerView extends JPanel implements TreeSelectionListener, IPlugin {
  JTree tree;
  BottomThing thing;

  DefaultTreeModel tree_model = new DefaultTreeModel(null, true);
  JMenuBar mbar = new JMenuBar();

  // refresh_action is an action that is useful during debugging, but should go away later.
  // Action refresh_action;
  Action import_action;
  Action export_action;
  Action delete_action;
  Action add_separator_action;
  Action add_folder_action;
  Action add_bookmark_action;

  BookmarkTreeCellRenderer renderer;

  /** Creates a new instance of Class */
  public BookmarkManagerView() {
    super();

    tree = new AutoScrollingJTree();
    tree.setModel(tree_model);

    JScrollPane scroll_pane = new JScrollPane(tree);

    this.setLayout(new BorderLayout());
    scroll_pane.setMinimumSize(new Dimension(50,50));
    this.add(scroll_pane, BorderLayout.CENTER);

    thing = new BottomThing(tree);
    //thing.setPreferredSize(new Dimension(100, 50));
    this.add(thing, BorderLayout.SOUTH);
    tree.addTreeSelectionListener(thing);

    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
    tree.setRootVisible(true);
    tree.setShowsRootHandles(true);
    tree.setEditable(false); // too much work to allow direct editing of item names

    renderer = new BookmarkTreeCellRenderer();
    tree.setCellRenderer(renderer);

    ToolTipManager.sharedInstance().registerComponent(tree);

    DataFlavor[] flavors = new DataFlavor[] {
      BookmarkManagerView.TreeNodeTransferable.my_flavor
    };

    DoubleTree dub_t = new BookmarkManagerView.DoubleTree(tree, flavors);

    // Would love to try using setDragEnabled(true) because this gives a better
    // selection mechanism. But that would involve a re-write of much of this code
    // adding a custom TransferHandler.
    //     tree.setDragEnabled(true);

    //refresh_action = makeRefreshAction();
    export_action = makeExportAction();
    import_action = makeImportAction();
    delete_action = makeDeleteAction();
    add_separator_action = makeAddAction(tree, 0);
    add_folder_action = makeAddAction(tree, 1);
    add_bookmark_action = makeAddAction(tree, 2);

    setUpMenuBar();
    //setUpToolBar();
    setUpPopupMenu();

    // Start with an empty bookmark list.
    // If later a call is made to setIGB(), that will cause the list from 
    // the BookMarkAction to be installed instead.
    this.setBList(new BookmarkList("Bookmarks"));

    tree.addTreeSelectionListener(this);
    this.validate();
  }

  public void setIGB(IGB igb) {
    if (igb != null) {
      igb.bmark_action.setBookmarkManager(this);
    }
    thing.setIGB(igb);
  }

  //boolean insert(JTree tree, TreePath tree_path, Bookmark b) {
  //  DefaultMutableTreeNode node = (DefaultMutableTreeNode) new BookmarkList(b);    
  //  return insert(tree, tree_path, new DefaultMutableTreeNode[] {node});
  //}
  
  boolean insert(JTree tree, TreePath tree_path, DefaultMutableTreeNode[] nodes) {
    if (tree_path == null) {
      return false;
    }
    DefaultMutableTreeNode tree_node = (DefaultMutableTreeNode) tree_path.getLastPathComponent();
    if (tree_node == null) {
      return false;
    }

    // Highlight the drop location while we perform the drop
    tree.setSelectionPath(tree_path);

    DefaultMutableTreeNode parent = null;
    int row = tree.getRowForPath(tree_path);
    if (tree_node.getAllowsChildren() && dropInto(row)) {
      parent = tree_node;
    } else {
      parent = (DefaultMutableTreeNode) tree_node.getParent();
    }
    int my_index = 0;
    if (parent != null) {
      my_index = parent.getIndex(tree_node);
    } else if (tree_node.isRoot()) {
      parent = tree_node;
      my_index = -1;
    }

    // Copy or move each source object to the target
    // if we count backwards, we can always add new nodes at (my_index + 1)
    for (int i = nodes.length-1; i >= 0; i--) {
      DefaultMutableTreeNode node = nodes[i];
      try {
        ((DefaultTreeModel) tree.getModel()).insertNodeInto(node, parent, my_index+1);
      } catch (IllegalStateException e) {
        // Cancelled by user
        return false;
      }
    }

    return true;
  }

  public void setBList(BookmarkList blist) {
    tree_model.setRoot(blist);
    // selecting, then clearing the selection, makes sure that valueChanged() gets called.
    tree.setSelectionRow(0);
    tree.clearSelection();
  }

  public TreeModel getTreeModel() {
    return tree_model;
  }

  static void setAccelerator(Action a) {
    KeyStroke ks = UnibrowPrefsUtil.getAccelerator("Bookmark Manager / "+a.getValue(Action.NAME));
    a.putValue(Action.ACCELERATOR_KEY, ks);
  }


  /** A JPanel that listens for TreeSelectionEvents, displays
   *  the name(s) of the selected item(s), and may allow you to edit them.
   */
  static class BottomThing extends JPanel implements TreeSelectionListener, ActionListener, FocusListener {
    JLabel type_label = new JLabel("Type:");
    JLabel type_label_2 = new JLabel("");
    JLabel name_label = new JLabel("Name:");
    JTextField name_text_field = new JTextField(30);
    BookmarkListEditor bl_editor;
    TreePath selected_path = null;
    BookmarkList selected_bl = null;

    private JTree tree;
    private IGB uni = null;
    private DefaultTreeModel def_tree_model;

    Action properties_action;
    Action goto_action;

    BottomThing(JTree tree) {
      if (tree==null) throw new IllegalArgumentException();

      this.tree = tree;
      this.def_tree_model = (DefaultTreeModel) tree.getModel();

      properties_action = makePropertiesAction();
      properties_action.setEnabled(false);
      goto_action = makeGoToAction();
      goto_action.setEnabled(false);

      Box type_box = new Box(BoxLayout.X_AXIS);
      type_box.add(type_label);
      type_box.add(Box.createHorizontalStrut(5));
      type_box.add(type_label_2);
      type_box.add(Box.createHorizontalGlue());

      Box name_box = new Box(BoxLayout.X_AXIS);
      name_box.add(name_label);
      name_box.add(Box.createHorizontalStrut(5));
      name_box.add(name_text_field);
      this.name_text_field.setEnabled(false);
      name_text_field.addActionListener(this);
      name_text_field.addFocusListener(this);

      Box button_box = new Box(BoxLayout.X_AXIS);
      button_box.add(Box.createHorizontalGlue());
      JButton edit_button = new JButton(properties_action);
      button_box.add(edit_button);
      button_box.add(Box.createHorizontalStrut(5));
      JButton goto_button = new JButton(goto_action);
      button_box.add(goto_button);
      button_box.add(Box.createHorizontalGlue());

      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.add(type_box);
      this.add(name_box);
      this.add(button_box);

      bl_editor = new BookmarkListEditor(def_tree_model);
    }

    /** Sets the instance of IGB.  This is the instance
     *  in which the bookmarks will be opened when the "GoTo" button
     *  is pressed.
     *  @param uni an instance of Unibrow; null is ok.
     */
    void setIGB(IGB uni) {
      this.uni = uni;
    }

    public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
      Object source = e.getSource();
      assert source == tree;
      if (source != tree) {
        return;
      }

      TreePath[] selections = tree.getSelectionPaths();
      type_label_2.setText("");
      name_text_field.setText("");
      if (selections == null || selections.length != 1) {
        name_text_field.setText("");
        name_text_field.setEnabled(false);
        properties_action.setEnabled(false);
        goto_action.setEnabled(false);
        return;
      }
      else {
        selected_path = selections[0];
        selected_bl = (BookmarkList) selected_path.getLastPathComponent();
        Object user_object = selected_bl.getUserObject();
        //bl_editor.setBookmarkList(selected_bl);
        if (user_object instanceof Bookmark) {
          Bookmark bm = (Bookmark) user_object;
          if (!bm.isUnibrowControl()) {
            type_label_2.setText("External Bookmark");
          } else {
            type_label_2.setText("Internal Bookmark");
          }
          name_text_field.setText(bm.getName());
          name_text_field.setEnabled(true);
          properties_action.setEnabled(true);
          goto_action.setEnabled(uni != null);
        } else if (user_object instanceof Separator) {
          type_label_2.setText("Separator");
          name_text_field.setText("Separator");
          name_text_field.setEnabled(false);
          properties_action.setEnabled(false);
          goto_action.setEnabled(false);
        } else {
          type_label_2.setText("Bookmark List");
          name_text_field.setText(user_object.toString());
          // don't allow editing the root bookmark list name: see rename()
          name_text_field.setEnabled(selected_bl != def_tree_model.getRoot());
          properties_action.setEnabled(selected_bl != def_tree_model.getRoot());
          goto_action.setEnabled(false);
        }
      }
    }

    public void rename(BookmarkList bl, String name) {
      if (bl == def_tree_model.getRoot()) {
        // I do not allow re-naming the root node because the current BookmarkParser
        // class cannot actually read the name of a bookmark list, so any
        // name change would be lost after saving and re-loading.
        return;
      }
      if (name == null || name.length()==0) return;
      Object user_object = selected_bl.getUserObject();
      if (user_object instanceof Bookmark) {
        Bookmark bm = (Bookmark) user_object;
        bm.setName(name);
        def_tree_model.nodeChanged(bl);
      } else if (user_object instanceof String) {
        selected_bl.setUserObject(name);
        def_tree_model.nodeChanged(bl);
      }
    }

    public void actionPerformed(ActionEvent e) {
      Object source = e.getSource();
      if (source == name_text_field && e.getID() == ActionEvent.ACTION_PERFORMED) {
        rename(selected_bl, name_text_field.getText());
      }
    }

    public void focusGained(FocusEvent e) {
    }

    /** Allows renaming the selected BookmarkList when the text field loses
     *  focus due to tabbing.
     */
    public void focusLost(FocusEvent e) {
      if (e.getSource() == name_text_field) {
        //System.out.println("Lost focus! "+name_text_field.getText());
        rename(selected_bl, name_text_field.getText());
      }
    }

    public Action getPropertiesAction() {
      return properties_action;
    }
    
    private Action makePropertiesAction() {
      Action a = new AbstractAction("Properties ...") {
        public void actionPerformed(ActionEvent ae) {
          if (selected_bl == null || selected_bl.getUserObject() instanceof Separator) {
            setEnabled(false);
          } else {
            // Give the new window the same icon that the parent frame of this item has
            JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, tree);
            if (frame != null) {bl_editor.setIconImage(frame.getIconImage());}

            // then open it
            bl_editor.openDialog(selected_bl);
          }
        }
      };
      setAccelerator(a);
      return a;
    }

    public Action getGoToAction() {
      return goto_action;
    }

    private Action makeGoToAction() {
      Action a = new AbstractAction("Go To") {
        public void actionPerformed(ActionEvent ae) {
          if (uni==null || selected_bl == null || !(selected_bl.getUserObject() instanceof Bookmark)) {
            setEnabled(false);
          } else {
            Bookmark bm = (Bookmark) selected_bl.getUserObject();
            BookmarkController.viewBookmark(uni, bm);
          }
        }
      };
      setAccelerator(a);
      return a;
    }
  }

  public static void main(String[] args) throws Exception {
    JFrame frame = new JFrame("BookmarkManagerView");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.setBounds(10, 10, 800, 500);
    Image icon = com.affymetrix.igb.IGB.getIcon();
    if (icon != null) {frame.setIconImage(icon);}

    BookmarkManagerView foo = new BookmarkManagerView();
    foo.setBList(new BookmarkList("BOOKMARKS"));
    frame.getContentPane().add(foo, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
  }

  public void valueChanged(TreeSelectionEvent e) {
    if (e.getSource() != tree) return;
    TreePath[] paths = e.getPaths();
    int selections = tree.getSelectionCount();
    //import_action.setEnabled(selections != 0);
    //export_action.setEnabled(selections != 0);
    delete_action.setEnabled(selections != 0);
    add_separator_action.setEnabled(selections != 0);
    add_folder_action.setEnabled(selections != 0);
    add_bookmark_action.setEnabled(selections != 0);
    //  the "properties" and "go to" actions belong to the BottomThing and it will enable or disable them
  }

  void setUpMenuBar() {
    JMenuBar menu_bar = new JMenuBar();
    JMenu bookmarks_menu = new JMenu("Bookmarks");

    //bookmarks_menu.add(refresh_action);
    bookmarks_menu.add(add_folder_action);
    bookmarks_menu.add(add_separator_action);
    bookmarks_menu.add(add_bookmark_action);
    bookmarks_menu.addSeparator();
    bookmarks_menu.add(thing.getPropertiesAction());
    bookmarks_menu.add(thing.getGoToAction());
    bookmarks_menu.addSeparator();
    bookmarks_menu.add(delete_action);
    bookmarks_menu.addSeparator();
    bookmarks_menu.add(import_action);
    bookmarks_menu.add(export_action);

    menu_bar.add(bookmarks_menu);
    this.add(menu_bar, BorderLayout.NORTH);
  }

  void setUpPopupMenu() {
    final JPopupMenu popup = new JPopupMenu();
    popup.add(add_folder_action);
    popup.add(add_separator_action);
    popup.add(add_bookmark_action);
    popup.addSeparator();
    popup.add(thing.getPropertiesAction());
    popup.add(thing.getGoToAction());
    popup.addSeparator();
    popup.add(delete_action);
    popup.addSeparator();
    popup.add(import_action);
    popup.add(export_action);
    MouseAdapter mouse_adapter = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (popup.isPopupTrigger(e)) {
          popup.show(tree, e.getX(), e.getY());
        }
      }
      public void mouseReleased(MouseEvent e) {
        if (popup.isPopupTrigger(e)) {
          popup.show(tree, e.getX(), e.getY());
        }
      }
    };
    tree.addMouseListener(mouse_adapter);
  }

  void setUpToolBar() {
    JToolBar tool_bar = new JToolBar(JToolBar.VERTICAL);
    tool_bar.setFloatable(false);
    //tool_bar.add(refresh_action);
    tool_bar.add(add_folder_action);
    tool_bar.add(add_separator_action);
    tool_bar.add(add_bookmark_action);
    tool_bar.addSeparator();
    tool_bar.add(thing.getPropertiesAction());
    tool_bar.add(thing.getGoToAction());
    tool_bar.addSeparator();
    tool_bar.add(delete_action);
    tool_bar.addSeparator();
    tool_bar.add(import_action);
    tool_bar.add(export_action);
    this.add(tool_bar, BorderLayout.EAST);
  }

  Action makeRefreshAction() {
    Action a = new AbstractAction("Refresh") {
      public void actionPerformed(ActionEvent ae) {
        tree_model.reload();
      }
    };
    setAccelerator(a);
    return a;
  }

  Action makeImportAction() {
    Action a = new AbstractAction("Import ...") {
      public void actionPerformed(ActionEvent ae) {
        BookmarkList bl = (BookmarkList) tree_model.getRoot();
        BookMarkAction.importBookmarks(bl, null);
        tree_model.reload();
      }
    };
    setAccelerator(a);
    return a;
  }

  Action makeExportAction() {
    Action a = new AbstractAction("Export ...") {
      public void actionPerformed(ActionEvent ae) {
        BookmarkList bl = (BookmarkList) tree_model.getRoot();
        BookMarkAction.exportBookmarks(bl, null); // already contains a null check on bookmark list
      }
    };
    setAccelerator(a);
    return a;
  }

  Action makeDeleteAction() {
    Action a = new AbstractAction("Delete ...") {
      public void actionPerformed(ActionEvent ae) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths==null) {
          return;
        }
        Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, tree);
        int yes = JOptionPane.showConfirmDialog(frame,
          "Delete these "+paths.length+" selected bookmarks?",
          "Delete?", JOptionPane.YES_NO_OPTION);
        if (yes == JOptionPane.YES_OPTION) {
          for (int i=0; i<paths.length; i++) {
            TreePath path = paths[i];
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getParent() != null) {
              tree_model.removeNodeFromParent(node);
            }
          }
        }
      }
    };
    setAccelerator(a);
    return a;
  }

  Action makeAddAction(final JTree tree, final int type) {
    String title;
    if (type==0) {
      title = "New Separator";
    } else if (type==1) {
      title = "New Folder";
    } else if (type==2) {
      title = "New Bookmark";
    } else {
      title = "New ???";
    }

    Action a = new AbstractAction(title) {
      public void actionPerformed(ActionEvent ae) {
        TreePath path = tree.getSelectionModel().getSelectionPath();
        if (path==null) {
          System.out.println("No selection");
          return;
        }
        BookmarkList bl = null;
        if (type==0) {
          Separator s = new Separator();
          bl = new BookmarkList(s);
        } else if (type==1) {
          bl = new BookmarkList("Folder");
        } else if (type==2) {
          try {
            Bookmark b = new Bookmark("Bookmark", Bookmark.constructURL(Collections.EMPTY_MAP));
            bl = new BookmarkList(b);
          } catch (MalformedURLException mue) {
            mue.printStackTrace();
          }
        }
        if (bl != null) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) bl;
          insert(tree, path, new DefaultMutableTreeNode[] {node});
        }
      }
    };
    setAccelerator(a);
    return a;
  }

  /** Returns true or false to indicate that if an item is inserted at
   *  the given row it will be inserted "into" (true) or "after" (false)
   *  the item currently at that row.  Will return true only if the given
   *  row contains a folder and that folder is currently expanded or empty
   *  or is the root node.
   */
  boolean dropInto(int row) {
    boolean into = false;
    TreePath path = tree.getPathForRow(row);
    if (path == null) {
      // not necessarily an error
      return false;
    }
    if (row == 0) { // node is root [see DefaultMutableTreeNode.isRoot()]
      into = true;
    }
    else if (tree.isExpanded(path)) {
      into = true;
    }
    else {
      TreeNode node = (TreeNode) path.getLastPathComponent();
      if (node.getAllowsChildren() && node.getChildCount() == 0) {
        into = true;
      }
    }
    return into;
  }

  /** This implementation always returns null. */
  public Object getPluginProperty(Object o) {
    return null;
  }
  
  /** If the key is {@link IPlugin#TEXT_KEY_IGB}, this will
   *  make a call to {@link #setIGB(IGB)}.  Any other key
   *  will be ignored.
   */
  public void putPluginProperty(Object key, Object value) {
    if (IPlugin.TEXT_KEY_IGB.equals(key)) {
      this.setIGB((IGB) value);
    }
  }
  
  public void destroy() {
    this.setIGB(null);
    tree.removeTreeSelectionListener(this);
    thing = null;
    tree = null;
  }
  
  static class TreeNodeTransferable implements Transferable {

    public static final DataFlavor my_flavor = new DataFlavor(BookmarkList[].class, "Array of BookmarkList objects");
    BookmarkList[] nodes;

    public TreeNodeTransferable(BookmarkList[] nodes) {
      if (nodes==null) throw new IllegalArgumentException();
      this.nodes = nodes;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      if (! isDataFlavorSupported(flavor) ) { throw new UnsupportedFlavorException(flavor); }
      else return nodes;
    }

    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] { my_flavor };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor.equals(my_flavor);
    }
  }


 /** A JTree that handles Drag and Drop to itself. */
 class DoubleTree extends JTreeDropTarget implements DragGestureListener, DragSourceListener {

    BookmarkList[] nodes_being_moved;

    public DoubleTree(JTree tree, DataFlavor[] flavors) {
      super();
      setTree(tree);
      setFlavors(flavors);
      DragSource dragSource = DragSource.getDefaultDragSource();
      dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    /** If it was a COPY, do nothing special, but if it was a MOVE, delete the original. */
    public void dragDropEnd(DragSourceDropEvent dsde) {
      if (dsde.getDropSuccess() && dsde.getDropAction() == DnDConstants.ACTION_MOVE) {
        for (int i=0; i<nodes_being_moved.length; i++) {
          //DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
          //System.out.println("NOT Removing: "+nodes[i]+", from parent: "+nodes[i].getParent());
          ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(nodes_being_moved[i]);
        }
      }
    }

    Collection dragging_paths = new Vector();

    public void dragGestureRecognized(DragGestureEvent dge) {
      dragging_paths.clear();

      Point location = dge.getDragOrigin();
      TreePath dragPath = tree.getClosestPathForLocation(location.x, location.y);
      if (dragPath != null && tree.isPathSelected(dragPath)) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
          // TODO
          // Might want to find a different way to put the node data into a Transferable.
          // This way, the entire tree model always gets serialized, because tree nodes
          // internally refer both to their parents and children.
          // Better maybe to make a deep copy of only the nodes we want
          // (with children, but without their parents) and transfer that.
          // It works fine as is, but is inefficient.

          nodes_being_moved = new BookmarkList[paths.length];
          for (int i=0; i<paths.length; i++) {
            nodes_being_moved[i] = (BookmarkList) paths[i].getLastPathComponent();
            dragging_paths.add(paths[i]);
          }
          Transferable t = new TreeNodeTransferable(nodes_being_moved);
          dge.startDrag((Cursor) null, t, this);
        }
      }
    }

    public void dragEnter(DragSourceDragEvent dsde) {
      //System.out.println("DragEnter: "+dsde);
    }

    public void dragExit(DragSourceEvent dse) {
      //System.out.println("DragExit: "+dse);
    }

    public void dragOver(DragSourceDragEvent dsde) {
      //System.out.println("DragOver: "+dsde);
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
      //System.out.println("dropActionChanged: "+dsde);
    }

    protected boolean dropImpl(DropTargetDropEvent dtde) {

      Transferable transferable = dtde.getTransferable();
      Point location = dtde.getLocation();

      BookmarkList[] nodes = null;
      try {
        nodes = (BookmarkList[]) transferable.getTransferData(
          BookmarkManagerView.TreeNodeTransferable.my_flavor);
      } catch (Exception e) {
        JFrame frame =
            (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this.tree);
        IGB.errorPanel(frame, "Error", "Error in Drag-and-drop", e);
        return false;
      }
      if (nodes==null || nodes.length==0) {
        return false;
      }

      TreePath tree_path = tree.getClosestPathForLocation(location.x, location.y);
      if (isAcceptableDropPath(tree_path)) {
        // Highlight the drop location while we perform the drop
        //tree.setSelectionPath(tree_path);

        return insert(tree, tree_path, nodes);
      } else {
        return false;
      }
    }

    /** Returns false if the things currently being dragged contains the given path
     *  or an ancestor of the given path.
     *  Called from {@link #isAcceptableDropLocation(DropTargetDragEvent)}.
     */
    protected boolean isAcceptableDropPath(TreePath path) {
      boolean b = super.isAcceptableDropPath(path);
      if (b==false) return b;

      boolean forbidden = false;
      Iterator iter = dragging_paths.iterator();
      while (iter.hasNext() && forbidden==false) {
        TreePath tp = (TreePath) iter.next();
        forbidden |= (tp.equals(path) || tp.isDescendant(path));
      }
      return !(forbidden);
    }

    protected int last_selected_row = -1;  // used by dragUnderFeedback()

    protected void dragUnderFeedback(int row) {
      renderer.setUnderlinedRow(-1);
      renderer.setOutlinedRow(-1);
      if (row >= 0) {
        boolean drop_in = dropInto(row);
        if (drop_in) {
         renderer.setOutlinedRow(row);
        } else {
         renderer.setUnderlinedRow(row);
        }
      }
      if (row != last_selected_row) {
        tree.repaint();
      }
      last_selected_row = row;
    }

  }
}

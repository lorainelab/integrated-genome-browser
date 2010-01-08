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

package com.affymetrix.igb.bookmarks;

import com.affymetrix.genoviz.swing.DisplayUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;

public final class BookmarkListEditor {
  
  /** The name of the JFrame. */
  public static final String TITLE = "Bookmark Editor";

  private final JPanel central_component = new JPanel();

  private final JLabel type_label = new JLabel("Type:");
  private final JLabel type_label_2 = new JLabel("");

  private final JPanel main_box = new JPanel();
  
  private final JLabel name_label = new JLabel("Name:", JLabel.TRAILING);
  private final JTextField name = new JTextField(30);

  private final BookmarkTableComponent ucb_editor = new BookmarkTableComponent();
  
  private final JButton submit_button;
  private final JButton cancel_button;
  
  private final JFrame frame = new JFrame(TITLE);

  private BookmarkList the_bookmark_list = null;
  
  private DefaultTreeModel tree_model = null;

   /** Creates a new instance of BookmarkListEditor. */
  public BookmarkListEditor(DefaultTreeModel t) {
    this.tree_model = t;
    
    name_label.setLabelFor(name);

    Box type_box = new Box(BoxLayout.X_AXIS);
    type_box.add(type_label);
    type_box.add(Box.createHorizontalStrut(5));
    type_box.add(type_label_2);
    type_box.add(Box.createHorizontalGlue());

    Box line1 = new Box(BoxLayout.X_AXIS);
    line1.add(name_label);
    line1.add(Box.createHorizontalStrut(5));
    line1.add(name);
        
    // Setting the layout to BorderLayout allows the JTable in the JScrollPane to
    // re-size itself dynamically correctly.
    central_component.setLayout(new BorderLayout());

    //central_component.setBorder(new javax.swing.border.EtchedBorder(javax.swing.border.EtchedBorder.LOWERED));

    Action cancel_action = new AbstractAction("Cancel") {
      public void actionPerformed(ActionEvent e) {
        frame.setVisible(false);
        saveWindowLocation();
      }
    };

    Action submit_action = new AbstractAction("Apply Changes") {
      public void actionPerformed(ActionEvent e) {
        boolean success = BookmarkListEditor.this.applyChanges();
        if (success) {
          frame.setVisible(false);
          saveWindowLocation();
        }
      }
    };
    cancel_button = new JButton(cancel_action);
    submit_button = new JButton(submit_action);
    
    Box line3 = new Box(BoxLayout.X_AXIS);
    line3.add(Box.createHorizontalGlue());
    line3.add(submit_button);
    line3.add(Box.createHorizontalStrut(5));
    line3.add(cancel_button);
    line3.add(Box.createHorizontalGlue());

    Box top_box = new Box(BoxLayout.Y_AXIS);
    top_box.add(type_box);
    top_box.add(line1);
    main_box.setLayout(new BorderLayout());
    main_box.add(top_box, BorderLayout.NORTH);
    main_box.add(central_component, BorderLayout.CENTER);
    main_box.add(line3, BorderLayout.SOUTH);

    setEnabled(false);
    frame.getContentPane().add(main_box);
    
    Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(TITLE, new Rectangle(400, 400));
    if (pos != null) {
      UnibrowPrefsUtil.setWindowSize(frame, pos);
    }

   frame.addWindowListener( new WindowAdapter() {
			@Override
      public void windowClosing(WindowEvent evt) {
        saveWindowLocation();
      }
   });
 }
  
  // Writes the window location to the persistent preferences.
  void saveWindowLocation() {
    UnibrowPrefsUtil.saveWindowLocation(frame, TITLE);
  }
  
  
  public void openDialog(BookmarkList bl) {
    this.setBookmarkList(bl);
    //frame.pack();
    frame.doLayout();
    frame.repaint();
    
    DisplayUtils.bringFrameToFront(frame);
  }

  boolean using_ucb_editor = true;
  
  public void setBookmarkList(BookmarkList bookmark_list) {
    this.the_bookmark_list = bookmark_list;
    Object o = null;
    central_component.removeAll();
    if (bookmark_list != null) {o = bookmark_list.getUserObject();}
    if (o instanceof Bookmark) {
      Bookmark bm = (Bookmark) o;
      name.setText(bm.getName());
      central_component.add(ucb_editor.getComponent());
      using_ucb_editor = true;
      
      if (bm.isUnibrowControl()) {
        type_label_2.setText("Internal Bookmark");
        //central_component.add(ucb_editor.getComponent());
      } else {
        type_label_2.setText("External Bookmark");
        //central_component.add(url_component);
      }
      
      this.setBookmark(bm);
      this.setEnabled(true);
    } else if (o instanceof String) {
      name.setText((String) o);
      type_label_2.setText("Bookmark List");
      this.setBookmark(null);
      this.setEnabled(true);
    } else if (o instanceof Separator) {
      name.setText("Separator");
      type_label_2.setText("");
      this.setBookmark(null);
      this.setEnabled(false);
    } else {
      name.setText("");
      type_label_2.setText("");
      this.setBookmark(null);
      this.setEnabled(false);
    }
  }

  /** Tries to reset the bookmark from the GUI.
   *  @return true for sucess, false otherwise.
   */
  boolean applyChanges() {
    if (the_bookmark_list == null) {
      return false;
    }
    Object o = the_bookmark_list.getUserObject();
    if (o instanceof Bookmark) {
      Bookmark the_bm = (Bookmark) o;
      the_bm.setName(name.getText());
      boolean ok = ucb_editor.setBookmarkFromGUI(the_bm);
      if (ok) {
        //the_bookmark_list.setUserObject(the_bm);
        this.setBookmarkList(the_bookmark_list);
      } else {
        return false;
      }
    } else if (o instanceof String) {
      the_bookmark_list.setUserObject(name.getText());
    }
    if (tree_model != null) {
      tree_model.nodeChanged(the_bookmark_list);
    }
    return true;
  }

  void setEnabled(boolean b) {
    submit_button.setEnabled(b);
    name.setEnabled(b);
  }

  void setBookmark(Bookmark bm) {
    ucb_editor.setGUIFromBookmark(bm);
  }
  
  public void setIconImage(java.awt.Image image) {
    if (image != null) {frame.setIconImage(image);}
  }

  /** For testing, brings-up an editor with a bookmark in it. */
  public static void main(String[] args) {
    BookmarkListEditor editor = new BookmarkListEditor(null);
    editor.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Bookmark bm = null;
    try {
      bm = new Bookmark("Test Bookmark", 
        "http://localhost:7085/UnibrowControl?seqid=chr21&end=33127379&version=Human_Apr_2003_Encode&start=32664811&data_url=http%3A%2F%2F205.217.46.81%3A9091%2FQueryServlet%2Fdas%2FHuman_Apr_2003%2Ffeatures%3Fsegment%3Dchr21%3A32664811%2C33127378%3Btype%3DAffy_u133_targets%3Bminmin%3D0%3Bmaxmax%3D46976537&data_url=http%3A%2F%2F205.217.46.81%3A9091%2FQueryServlet%2Fdas%2FHuman_Apr_2003%2Ffeatures%3Fsegment%3Dchr21%3A32664811%2C33127378%3Btype%3DAffy_u133_probes%3Bminmin%3D0%3Bmaxmax%3D46976537");
    } catch (Exception e) {
      e.printStackTrace();
    }
    BookmarkList bl = new BookmarkList(bm);
    
    editor.openDialog(bl);
//    editor.submit_button.setEnabled(false);
    editor.cancel_button.setEnabled(false);
  }
}

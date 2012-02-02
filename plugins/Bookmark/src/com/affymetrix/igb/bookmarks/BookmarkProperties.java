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

import com.affymetrix.genometryImpl.util.DisplayUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;

public final class BookmarkProperties {
  
  /** The name of the JFrame. */
  public static final String TITLE = "Bookmark Properties";

  private final JPanel central_component = new JPanel();

  private final JPanel main_box = new JPanel();

  private final BookmarkTableComponent ucb_editor = new BookmarkTableComponent();
  
  private final JFrame frame = new JFrame(TITLE);

  private BookmarkList the_bookmark_list = null;
  
  private DefaultTreeModel tree_model = null;

   /** Creates a new instance of BookmarkListEditor. */
  public BookmarkProperties(DefaultTreeModel t) {
    this.tree_model = t;
        
    // Setting the layout to BorderLayout allows the JTable in the JScrollPane to
    // re-size itself dynamically correctly.
    central_component.setLayout(new BorderLayout());

    Box top_box = new Box(BoxLayout.Y_AXIS);
    main_box.setLayout(new BorderLayout());
    main_box.add(top_box, BorderLayout.NORTH);
    main_box.add(central_component, BorderLayout.CENTER);

    frame.getContentPane().add(main_box);
    
    Rectangle pos = PreferenceUtils.retrieveWindowLocation(TITLE, new Rectangle(400, 400));
    if (pos != null) {
      PreferenceUtils.setWindowSize(frame, pos);
    }

   frame.addWindowListener( new WindowAdapter() {
			@Override
      public void windowClosing(WindowEvent evt) {
        saveWindowLocation();
      }
   });
 }
  
  // Writes the window location to the persistent preferences.
  private void saveWindowLocation() {
    PreferenceUtils.saveWindowLocation(frame, TITLE);
  }
  
  public void openDialog(BookmarkList bl) {
    this.setBookmarkList(bl);
    frame.doLayout();
    frame.repaint();
    
    DisplayUtils.bringFrameToFront(frame);
  }

  private void setBookmarkList(BookmarkList bookmark_list) {
    this.the_bookmark_list = bookmark_list;
    Object o = null;
    central_component.removeAll();
    if (bookmark_list != null) {o = bookmark_list.getUserObject();}
    if (o instanceof Bookmark) {
      Bookmark bm = (Bookmark) o;
      central_component.add(ucb_editor.getComponent());

      this.setBookmark(bm);
    }
  }

  private void setBookmark(Bookmark bm) {
    ucb_editor.setGUIFromBookmark(bm);
  }
  
  public void setIconImage(Image image) {
    if (image != null) {frame.setIconImage(image);}
  }
}

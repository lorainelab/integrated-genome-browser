package com.affymetrix.igb.bookmarks;

import java.awt.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableCellEditor;

/**
 * A JTable-based GUI editor for a Bookmark (or any URL).
 * This just has some fields for seeing and setting various parameters,
 * but does not have a "submit" button.  You need to supply the
 * "submit" button, etc.
 * Call {@link #setGUIFromBookmark(Bookmark)} to update the display.
 * Call {@link #setBookmarkFromGUI(Bookmark)} when a "submit" button is pressed.
 * @author  ed
 */
public final class BookmarkTableComponent {
  private static final boolean DEBUG = true;

  private final JPanel main_box = new JPanel();
  
  private final BookmarkTableModel data_model;
  private final JTable table;
  
  public BookmarkTableComponent() {
    main_box.setLayout(new BorderLayout());

    data_model = new BookmarkTableModel();
    table = new JTable(data_model);

    JScrollPane scrollpane = new JScrollPane(table);

    scrollpane.setMinimumSize(new java.awt.Dimension(700, 300));

    main_box.add(scrollpane, BorderLayout.CENTER);
  }
  
  public Component getComponent() {
    return main_box;
  }
    
  public void stopEditing() {
    TableCellEditor ed = table.getCellEditor();
    if (ed != null) {
      ed.stopCellEditing();
    }
  }

  public void cancelEditing() {
    TableCellEditor ed = table.getCellEditor();
    if (ed != null) {
      ed.cancelCellEditing();
    }
  }
  
  public void setGUIFromBookmark(Bookmark bm) {
    cancelEditing(); // just in case!
    if (bm == null) {
      data_model.setValuesFromMap(Collections.<String,String[]>emptyMap());
    } else {
      URL url = bm.getURL();
      String url_base = bm.getURL().toExternalForm();
      int index = url_base.indexOf('?');
      if (index > 0) {
        url_base = url_base.substring(0, index);
      }

      data_model.setValuesFromMap(Bookmark.parseParameters(url));
    }
  }  
}
 

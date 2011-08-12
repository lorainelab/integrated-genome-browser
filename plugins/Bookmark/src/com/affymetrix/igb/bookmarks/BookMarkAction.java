/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.MenuListener;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;

import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;

public final class BookMarkAction implements ActionListener, MenuListener {
   private final static boolean DEBUG = false;

  private final JRPMenu bookmark_menu;
  private final JRPMenuItem add_pos_markMI;
  private final JRPMenuItem add_data_markMI;
  private final JRPMenuItem exportMI;
  private final JRPMenuItem importMI;
  private final JRPMenuItem clearMI;
  private final JRPMenuItem manage_bookmarksMI;
  private final JRPMenuItem open_bookmark_tabMI;
  private final Map<Object,Component> component_hash = new HashMap<Object,Component>();
  private final BookmarkList main_bookmark_list = new BookmarkList("Bookmarks");
  private final JRPMenu main_bm_menu;
  private BookmarkManagerView bmv = null;
  private IGBService igbService;

  private static JFileChooser static_chooser = null;

  public BookMarkAction(IGBService _igbService, JRPMenu bm_menu) {
	igbService = _igbService;
    bookmark_menu = bm_menu;
    bookmark_menu.addMenuListener(this);
    add_pos_markMI = new JRPMenuItem("Bookmark_addPositionBookmark", BUNDLE.getString("addPositionBookmark"), KeyEvent.VK_P);
    add_pos_markMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Bookmarks16.gif"));
    add_data_markMI = new JRPMenuItem("Bookmark_addPositionAndDataBookmark", BUNDLE.getString("addPosition&DataBookmark"), KeyEvent.VK_G);
	add_data_markMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Bookmarks16.gif"));
    exportMI = new JRPMenuItem("Bookmark_exportBookmarks", BUNDLE.getString("exportBookmarks"), KeyEvent.VK_E);
    exportMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif"));
    importMI = new JRPMenuItem("Bookmark_importBookmarks", BUNDLE.getString("importBookmarks"), KeyEvent.VK_I);
    importMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Import16.gif"));
    clearMI = new JRPMenuItem("Bookmark_clearBookmarks", BUNDLE.getString("clearBookmarks"), KeyEvent.VK_C);
    manage_bookmarksMI = new JRPMenuItem("Bookmark_manageBookmarks", BUNDLE.getString("manageBookmarks"), KeyEvent.VK_M);
    open_bookmark_tabMI = new JRPMenuItem("Bookmark_openBookmarkTab", BUNDLE.getString("openBookmarkTab"), KeyEvent.VK_O);

    add_pos_markMI.addActionListener(this);
    add_data_markMI.addActionListener(this);
    importMI.addActionListener(this);
    exportMI.addActionListener(this);
    clearMI.addActionListener(this);
    manage_bookmarksMI.addActionListener(this);
    open_bookmark_tabMI.addActionListener(this);
    MenuUtil.addToMenu(bm_menu, add_pos_markMI);
    MenuUtil.addToMenu(bm_menu, add_data_markMI);
    bm_menu.addSeparator();
    MenuUtil.addToMenu(bm_menu, manage_bookmarksMI);
    MenuUtil.addToMenu(bm_menu, open_bookmark_tabMI);
    // export/import/clear are better done with the bookmark manager
    //MenuUtil.addToMenu(bm_menu, exportMI);
    //MenuUtil.addToMenu(bm_menu, importMI);
    //MenuUtil.addToMenu(bm_menu, clearMI);
    bm_menu.addSeparator();

    main_bm_menu = bm_menu;
    component_hash.put(main_bookmark_list, main_bm_menu);

    addDefaultBookmarks();
    buildMenus(main_bm_menu, main_bookmark_list);
  }

  public void setBmv(BookmarkManagerView bmv) {
	this.bmv = bmv;
	bmv.setBList(main_bookmark_list);
  }

  private void updateBookmarkManager() {
    if (bmv != null) bmv.setBList(main_bookmark_list);
  }

  public static File getBookmarksFile() {
    String app_dir = PreferenceUtils.getAppDataDirectory();
    File f = new File(app_dir, "bookmarks.html");
    return f;
  }

  /**
   *  Loads bookmarks from the file specified by {@link #getBookmarksFile()}.
   *  If loading succeeds, also creates a backup copy of that bookmark list
   *  in a new file with the same name, but "~" added at the end.
   */
	private void addDefaultBookmarks() {
		File f = getBookmarksFile();
		if (!f.exists()) {
			return;
		}

		String filename = f.getAbsolutePath();
		try {
			Logger.getLogger(BookMarkAction.class.getName()).log(Level.INFO, "Loading bookmarks from file {0}", filename);
			BookmarksParser.parse(main_bookmark_list, f);

			if (main_bookmark_list != null && main_bookmark_list.getChildCount() != 0) {
				File f2 = new File(filename + "~");
				try {
					Logger.getLogger(BookMarkAction.class.getName()).log(Level.INFO, "Creating backup bookmarks file: {0}", f2);
					BookmarkList.exportAsHTML(main_bookmark_list, f2, igbService.getAppName(), igbService.getAppVersion());
				} catch (Exception e) {
					Logger.getLogger(BookMarkAction.class.getName()).log(Level.SEVERE, "Error while trying to create backup bookmarks file: {0}", f2);
				}
			}

		} catch (FileNotFoundException fnfe) {
			Logger.getLogger(BookMarkAction.class.getName()).log(Level.SEVERE, "Could not load bookmarks. File not found or not readable: {0}", filename);
		} catch (IOException ioe) {
			Logger.getLogger(BookMarkAction.class.getName()).log(Level.SEVERE, "Could not load bookmarks from file {0}", filename);
		}

	}

  /** Will save the current bookmarks into the file that was specified
   *  by {@link #getBookmarksFile()}.
   *  @return true for sucessfully saving the file
   */
  public boolean autoSaveBookmarks() {
    boolean saved = false;
    if (main_bookmark_list == null) {
      return saved;
    }
    File f = getBookmarksFile();
    String filename = f.getAbsolutePath();
    try {
      Logger.getLogger(BookMarkAction.class.getName()).log(Level.INFO, "Saving bookmarks to file {0}", filename);
      File parent_dir = f.getParentFile();
      if (parent_dir != null) {
        parent_dir.mkdirs();
      }
      BookmarkList.exportAsHTML(main_bookmark_list, f, igbService.getAppName(), igbService.getAppVersion());
      saved = true;
    } catch (FileNotFoundException fnfe) {
      Logger.getLogger(BookMarkAction.class.getName()).log(Level.SEVERE, "Could not auto-save bookmarks to {0}", filename);
    } catch (IOException ioe) {
      Logger.getLogger(BookMarkAction.class.getName()).log(Level.SEVERE, "Error while saving bookmarks to {0}", filename);
    }
    return saved;
  }


  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == add_pos_markMI) {
      bookmarkCurrentPosition(false);
    }
    else if (src == add_data_markMI) {
      bookmarkCurrentPosition(true);
    }
    else if (src == exportMI) {
      exportBookmarks(main_bookmark_list, igbService.getFrame());
    }
    else if (src == importMI) {
      importBookmarks(main_bookmark_list, igbService.getFrame());
      updateBookmarkManager();
      //rebuildMenus(); // menus get rebuilt when the menu item is opened
    }
    else if (src == clearMI) {
      removeAllBookmarks();
    }
    else if (src == manage_bookmarksMI) {
        igbService.setTabStateAndMenu(bmv, TabState.COMPONENT_STATE_WINDOW);
      }
    else if (src == open_bookmark_tabMI) {
        igbService.setTabStateAndMenu(bmv, TabState.COMPONENT_STATE_RIGHT_TAB);
      }
    else if (src instanceof BookmarkJMenuItem) {
      BookmarkJMenuItem item = (BookmarkJMenuItem) src;
      Bookmark bm = item.getBookmark();
      try {
        BookmarkController.viewBookmark(igbService, bm);
      } catch (Exception e) {
        ErrorHandler.errorPanel("Problem viewing bookmark", e);
      }
    } else if (DEBUG) {
      System.out.println("Got an action event from an unknown source: "+src);
      System.out.println("command: "+evt.getActionCommand());
    }
  }

  void removeAllBookmarks() {
    removeAllBookmarkMenuItems();
    main_bookmark_list.removeAllChildren();
    updateBookmarkManager();
  }

  void removeAllBookmarkMenuItems() {
    Iterator<Component> iter = component_hash.values().iterator();
    while (iter.hasNext()) {
      Component comp = iter.next();
      if (comp == main_bm_menu) {
        // component_hash contains a mapping of main_bookmark_list to main_bm_menu.
        // That is the only JRPMenu we do not want to remove from its parent.
        continue;
      }
      if (comp instanceof JRPMenuItem) {
        JRPMenuItem item = (JRPMenuItem) comp;
        ActionListener[] listeners = item.getActionListeners();
        for (int i=0; i<listeners.length; i++) {
          item.removeActionListener(listeners[i]);
        }
      } else { // if not a JRPMenuItem, should be a JSeparator
      }
      Container cont = comp.getParent();
      if (cont != null) {cont.remove(comp);}
    }
    component_hash.clear();
    component_hash.put(main_bookmark_list, main_bm_menu);
  }


  private void buildMenus(JRPMenu pp, BookmarkList bl) {
    JRPMenu bl_menu = (JRPMenu) component_hash.get(bl);
    if (bl_menu == null) {
      bl_menu = addBookmarkListMenu(pp, bl);
    }
    @SuppressWarnings("unchecked")
	Enumeration<BookmarkList> e = bl.children();
    while (e.hasMoreElements()) {
      BookmarkList node = e.nextElement();
      Object o = node.getUserObject();
      if (o instanceof String) {
        buildMenus(bl_menu, node);
      } else if (o instanceof Bookmark) {
        addBookmarkMI(bl_menu, (Bookmark) o);
      } else if (o instanceof Separator) {
        addSeparator(bl_menu, (Separator) o);
      }
    }
  }

  private JRPMenuItem addBookmarkMI(JRPMenu parent_menu, Bookmark bm) {
    JRPMenuItem markMI = (JRPMenuItem) component_hash.get(bm);
    if (markMI != null) {
      return markMI;
    }
    markMI = new BookmarkJMenuItem(getIdFromName(bm.getName()), bm);
    component_hash.put(bm, markMI);
    parent_menu.add(markMI);
    markMI.addActionListener(this);
    return markMI;
  }

  private String getIdFromName(String name) {
    String id = "";
    try {
    	id = "Bookmark_" + URLEncoder.encode("UTF-8", name);
    }
    catch (Exception x) {}
    return id;
  }

  private JRPMenu addBookmarkListMenu(JRPMenu parent_menu, BookmarkList bm_list) {
    JRPMenu sub_menu = (JRPMenu) component_hash.get(bm_list);
    if (sub_menu != null) {
      return sub_menu;
    }
    sub_menu = new JRPMenu(getIdFromName(bm_list.getName()),bm_list.getName());
    component_hash.put(bm_list,  sub_menu);
    parent_menu.add(sub_menu);
    return sub_menu;
  }

  private JSeparator addSeparator(JRPMenu parent_menu, Separator s) {
    JSeparator jsep = (JSeparator) component_hash.get(s);
    if (jsep != null) {
      return null;
    }
    jsep = new JSeparator();
    component_hash.put(s, jsep);
    parent_menu.add(jsep);
    return jsep;
  }

	public File getLoadDirectory() {
		return FileTracker.DATA_DIR_TRACKER.getFile();
	}

	public void setLoadDirectory(File file) {
		FileTracker.DATA_DIR_TRACKER.setFile(file);
	}

  /**
   *  Tries to import bookmarks into Unibrow.
   *  Makes use of {@link BookmarksParser#parse(BookmarkList, File)}.
   */
  public void importBookmarks(BookmarkList bookmark_list, JFrame frame) {
    JFileChooser chooser = getJFileChooser();
    chooser.setCurrentDirectory(getLoadDirectory());
    int option = chooser.showOpenDialog(frame);
    if (option == JFileChooser.APPROVE_OPTION) {
      setLoadDirectory(chooser.getCurrentDirectory());
      try {
        File fil = chooser.getSelectedFile();
        BookmarksParser.parse(bookmark_list, fil);
        if (DEBUG) {
          System.out.println("Imported bookmarks: ");
          bookmark_list.printText(System.out);
        }
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel(frame, "Error", "Error importing bookmarks", ex);
      }
    }
  }


  /** Gets a static re-usable file chooser that prefers "html" files. */
  private JFileChooser getJFileChooser() {
    if (static_chooser == null) {
      static_chooser = new JFileChooser();
      static_chooser.setCurrentDirectory(getLoadDirectory());
      UniFileFilter filter = new UniFileFilter(
        new String[] {"html", "htm", "xhtml"}, "HTML Files");
      static_chooser.addChoosableFileFilter(filter);
    }
    static_chooser.rescanCurrentDirectory();
    return static_chooser;
  }

  public void exportBookmarks(BookmarkList main_bookmark_list, JFrame frame) {
    if (main_bookmark_list == null || main_bookmark_list.getChildCount()==0) {
      ErrorHandler.errorPanel(frame, "Error", "No bookmarks to save", (Exception)null);
      return;
    }
    JFileChooser chooser = getJFileChooser();
    chooser.setCurrentDirectory(getLoadDirectory());
    int option = chooser.showSaveDialog(frame);
    if (option == JFileChooser.APPROVE_OPTION) {
      try {
    	setLoadDirectory(chooser.getCurrentDirectory());
        File fil = chooser.getSelectedFile();
        String full_path = fil.getCanonicalPath();

        if ((! full_path.endsWith(".html"))
         && (! full_path.endsWith(".htm"))
         && (! full_path.endsWith(".xhtml"))) {
          fil = new File(full_path + ".html");
        }

        if (DEBUG) {System.out.println("bookmark file chosen: " + fil);}
        BookmarkList.exportAsHTML(main_bookmark_list, fil, igbService.getAppName(), igbService.getAppVersion());
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel(frame, "Error", "Error exporting bookmarks", ex);
      }
    }
  }

	private void bookmarkCurrentPosition(boolean include_sym_and_props) {
		if (include_sym_and_props && !BookmarkController.hasSymmetriesOrGraphs()){
			ErrorHandler.errorPanel("Error: No Symmetries or graphs to bookmark.");
			return;
		}
		Bookmark bookmark = null;
		try {
			bookmark = BookmarkController.getCurrentBookmark(include_sym_and_props, igbService.getVisibleSpan());
		}
	    catch (MalformedURLException m) {
	    	ErrorHandler.errorPanel("Couldn't add bookmark", m);
	    	return;
	    }
		if (bookmark == null) {
			ErrorHandler.errorPanel("Error", "Nothing to bookmark");
			return;
		}
		String default_name = bookmark.getName();
		String bookmark_name = (String) JOptionPane.showInputDialog(bmv,
				"Enter name for bookmark", "Input",
				JOptionPane.PLAIN_MESSAGE, null, null, default_name);
		if (bookmark_name == null) {
			if (DEBUG) {
				System.out.println("bookmark action cancelled");
			}
		} else {
			if (bookmark_name.trim().length() == 0) {
				bookmark_name = default_name;
			}
			if (DEBUG) {
				System.out.println("bookmark name: " + bookmark_name);
			}
			bookmark.setName(bookmark_name);
			addBookmark(bookmark);
		}
	}

  private JRPMenuItem addBookmark(Bookmark bm) {
    JRPMenuItem markMI = null;
    JRPMenu parent_menu = (JRPMenu) component_hash.get(main_bookmark_list);
    if (parent_menu == null) {
      ErrorHandler.errorPanel("Couldn't add bookmark. Lost reference to menu");
      return null;
    }
    addBookmarkMI(parent_menu, bm);
    BookmarkList bl = main_bookmark_list.addBookmark(bm);

    updateBookmarkManager();
    if (bmv != null) {
    	bmv.addBookmarkToHistory(bl);
    }
    return markMI;
  }


  /** Does nothing. */
  public void menuCanceled(javax.swing.event.MenuEvent e) {
  }

  /** Does nothing. */
  public void menuDeselected(javax.swing.event.MenuEvent e) {
  }

  /** Every time the menu is selected (thus opened) re-build the bookmark menu items.
   *  Thus if the bookmarks have been changed by the bookmark manager, we will
   *  adapt to that now.
   *  Slow? Yes.  Too slow?  Not really.
   */
  public void menuSelected(javax.swing.event.MenuEvent e) {
    if (e.getSource()==bookmark_menu) {
      rebuildMenus();
    }
  }

  private void rebuildMenus() {
    removeAllBookmarkMenuItems();
    buildMenus(main_bm_menu, main_bookmark_list);
  }
}

/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkList;
import com.affymetrix.igb.bookmarks.BookmarkMenuItem;
import com.affymetrix.igb.bookmarks.BookmarksParser;
import com.affymetrix.igb.bookmarks.Separator;
import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.lorainelab.igb.services.IgbService;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BookmarkActionManager implements ActionListener, TreeModelListener {

    private final static boolean DEBUG = false;
    private final Map<Object, Component> componentHash = new HashMap<>();
    private final JMenu main_bm_menu;
    private final BookmarkList main_bookmark_list;
    private IgbService igbService;
    private static BookmarkActionManager instance;
    private static final Logger logger = LoggerFactory.getLogger(BookmarkActionManager.class);

    public static void init(IgbService _igbService, JMenu bm_menu, BookmarkList main_bookmark_list) {
        if (instance == null) {
            instance = new BookmarkActionManager(_igbService, bm_menu, main_bookmark_list);
        }
    }

    public static synchronized BookmarkActionManager getInstance() {
        return instance;
    }

    public BookmarkActionManager(IgbService _igbService, JMenu bm_menu, BookmarkList main_bm_list) {
        igbService = _igbService;
        main_bm_menu = bm_menu;
        main_bookmark_list = main_bm_list;
        componentHash.put(main_bookmark_list, main_bm_menu);

        addDefaultBookmarks();
        buildMenus(main_bm_menu, main_bookmark_list);
    }

    public static File getBookmarksFile() {
        String app_dir = PreferenceUtils.getAppDataDirectory();
        File f = new File(app_dir, "bookmarks.html");
        return f;
    }

    /**
     * Loads bookmarks from the file specified by {@link #getBookmarksFile()}.
     * If loading succeeds, also creates a backup copy of that bookmark list in
     * a new file with the same name, but "~" added at the end.
     */
    private void addDefaultBookmarks() {
        File f = getBookmarksFile();
        if (!f.exists()) {
            return;
        }

        String filename = f.getAbsolutePath();
        try {
            logger.info("Loading bookmarks from file {}", filename);
            BookmarksParser.parse(main_bookmark_list, f);
            saveBookmarks(new File(filename + "~"));
        } catch (FileNotFoundException fnfe) {
            logger.error("Could not auto-save bookmarks to {}", filename);
        } catch (IOException ioe) {
            logger.error("Error while saving bookmarks to {}", filename);
        }
    }

    /**
     * Will save the current bookmarks into the file that was specified by
     * {@link #getBookmarksFile()}.
     *
     * @return true for sucessfully saving the file
     */
    public void autoSaveBookmarks() {
        File f = getBookmarksFile();
        String filename = f.getAbsolutePath();
        try {
            saveBookmarks(f);
        } catch (FileNotFoundException fnfe) {
            logger.error("Could not auto-save bookmarks to {}", filename);
        } catch (IOException ioe) {
            logger.error("Error while saving bookmarks to {}", filename);
        }
    }

    private void saveBookmarks(File f) throws IOException {
        if (f == null) {
            logger.error("File variable null");
            return;
        }

        if (main_bookmark_list != null) {
            String filename = f.getAbsolutePath();
            File parent_dir = f.getParentFile();
            if (parent_dir != null) {
                parent_dir.mkdirs();
            }
            logger.info("Saving bookmarks to file {}", filename);
            BookmarkList.exportAsHTML(main_bookmark_list, f);
        }
    }

    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if (src instanceof BookmarkMenuItem) {
            BookmarkMenuItem item = (BookmarkMenuItem) src;
            Bookmark bm = item.getBookmark();
            try {
                BookmarkController.viewBookmark(igbService, bm);
            } catch (Exception e) {
                ErrorHandler.errorPanel("Problem viewing bookmark", e, Level.WARNING);
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("Got an action event from an unknown source: " + src);
            logger.debug("command: " + evt.getActionCommand());
        }
    }

    private void removeAllBookmarkMenuItems() {
        for (Component comp : componentHash.values()) {
            if (comp == main_bm_menu) {
                // component_hash contains a mapping of main_bookmark_list to main_bm_menu.
                // That is the only JMenu we do not want to remove from its parent.
                continue;
            }
            if (comp instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) comp;
                ActionListener[] listeners = item.getActionListeners();
                for (ActionListener listener : listeners) {
                    item.removeActionListener(listener);
                }
            } else { // if not a JMenuItem, should be a JSeparator
            }
            Container cont = comp.getParent();
            if (cont != null) {
                cont.remove(comp);
            }
        }
        componentHash.clear();
        componentHash.put(main_bookmark_list, main_bm_menu);
    }

    /**
     * Generate bookmark menus by passed bookmark list.
     *
     * @param pp
     * @param bl
     */
    private void buildMenus(JMenu pp, BookmarkList bl) {
        JMenu listMenu = (JMenu) componentHash.get(bl);
        if (listMenu == null) {
            listMenu = addBookmarkListMenu(pp, bl);
        }
        @SuppressWarnings("unchecked")
        Enumeration<BookmarkList> e = bl.children();
        while (e.hasMoreElements()) {
            BookmarkList node = e.nextElement();
            Object o = node.getUserObject();
            if (o instanceof String) {
                buildMenus(listMenu, node);
            } else if (o instanceof Bookmark) {
                addBookmarkMI(listMenu, (Bookmark) o);
            } else if (o instanceof Separator) {
                addSeparator(listMenu, (Separator) o);
            }
        }

    }

    /**
     * Add passed bookmark to passed menu.
     *
     * @param parentMenu
     * @param bookmark
     * @return
     */
    private JMenuItem addBookmarkMI(JMenu parentMenu, Bookmark bookmark) {
        JMenuItem menuItem = (JMenuItem) componentHash.get(bookmark);
        if (menuItem != null) {
            return menuItem;
        }
        menuItem = new BookmarkMenuItem(bookmark);
        componentHash.put(bookmark, menuItem);
        parentMenu.add(menuItem);
        menuItem.addActionListener(this);
        return menuItem;
    }

    /**
     * Add passed bookmark list to passed menu.
     *
     * @param parent_menu
     * @param bm_list
     * @return
     */
    private JMenu addBookmarkListMenu(JMenu parent_menu, BookmarkList bm_list) {
        JMenu sub_menu = (JMenu) componentHash.get(bm_list);
        if (sub_menu != null) {
            return sub_menu;
        }
        sub_menu = new JMenu(bm_list.getName());
        componentHash.put(bm_list, sub_menu);
        parent_menu.add(sub_menu);
        return sub_menu;
    }

    /**
     * Add s separator list to passed menu.
     *
     * @param parent_menu
     * @param s
     * @return
     */
    private JSeparator addSeparator(JMenu parent_menu, Separator s) {
        JSeparator jsep = (JSeparator) componentHash.get(s);
        if (jsep != null) {
            return null;
        }
        jsep = new JSeparator();
        componentHash.put(s, jsep);
        parent_menu.add(jsep);
        return jsep;
    }

    private void rebuildMenus() {
        removeAllBookmarkMenuItems();
        buildMenus(main_bm_menu, main_bookmark_list);
    }

    public SeqSpan getVisibleSpan() {
        return igbService.getSeqMapView().getVisibleSpan();
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        Object[] children = e.getChildren();
        for (Object child : children) {
            if (child instanceof BookmarkList) {
                BookmarkList node = (BookmarkList) child;
                Object o = node.getUserObject();
                if (o instanceof String) {
                    Component comp = componentHash.get(node);
                    if (comp instanceof JMenu) {
                        ((JMenu) comp).setText(node.getName());
                    }
                } else if (o instanceof Bookmark) {
                    //addBookmarkMI(bl_menu, (Bookmark) o);
                    Component comp = componentHash.get(o);
                    if (comp != null && comp instanceof BookmarkMenuItem) {
                        ((BookmarkMenuItem) comp).setText(((Bookmark) o).getName());
                    }
                } else if (o instanceof Separator) {

                }
            }
        }
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        rebuildMenus();
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        rebuildMenus();
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        rebuildMenus();
    }
}

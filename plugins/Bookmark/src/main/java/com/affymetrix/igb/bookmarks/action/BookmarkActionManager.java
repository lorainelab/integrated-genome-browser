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

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.bookmarks.*;
import com.affymetrix.igb.osgi.service.IGBService;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JSeparator;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BookmarkActionManager implements ActionListener, TreeModelListener {

    private final static boolean DEBUG = false;
    private final Map<Object, Component> component_hash = new HashMap<Object, Component>();
    private final JRPMenu main_bm_menu;
    private final BookmarkList main_bookmark_list;
    private IGBService igbService;
    private static BookmarkActionManager instance;
    private static final Logger logger = LoggerFactory.getLogger(BookmarkActionManager.class);

    public static void init(IGBService _igbService, JRPMenu bm_menu, BookmarkList main_bookmark_list) {
        if (instance == null) {
            instance = new BookmarkActionManager(_igbService, bm_menu, main_bookmark_list);
        }
    }

    public static synchronized BookmarkActionManager getInstance() {
        return instance;
    }

    public BookmarkActionManager(IGBService _igbService, JRPMenu bm_menu, BookmarkList main_bm_list) {
        igbService = _igbService;
        main_bm_menu = bm_menu;
        main_bookmark_list = main_bm_list;
        component_hash.put(main_bookmark_list, main_bm_menu);

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

    private void saveBookmarks(File f) throws FileNotFoundException, IOException {
        if (f == null) {
            logger.error("File variable null");
            return;
        }

        if (main_bookmark_list != null && main_bookmark_list.getChildCount() != 0) {
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
        if (src instanceof BookmarkJMenuItem) {
            BookmarkJMenuItem item = (BookmarkJMenuItem) src;
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
                for (int i = 0; i < listeners.length; i++) {
                    item.removeActionListener(listeners[i]);
                }
            } else { // if not a JRPMenuItem, should be a JSeparator
            }
            Container cont = comp.getParent();
            if (cont != null) {
                cont.remove(comp);
            }
        }
        component_hash.clear();
        component_hash.put(main_bookmark_list, main_bm_menu);
    }

    /**
     * Generate bookmark menus by passed bookmark list.
     *
     * @param pp
     * @param bl
     */
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

    /**
     * Add passed bookmark to passed menu.
     *
     * @param parent_menu
     * @param bm
     * @return
     */
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
        } catch (Exception x) {
        }
        return id;
    }

    /**
     * Add passed bookmark list to passed menu.
     *
     * @param parent_menu
     * @param bm_list
     * @return
     */
    private JRPMenu addBookmarkListMenu(JRPMenu parent_menu, BookmarkList bm_list) {
        JRPMenu sub_menu = (JRPMenu) component_hash.get(bm_list);
        if (sub_menu != null) {
            return sub_menu;
        }
        sub_menu = new JRPMenu(getIdFromName(bm_list.getName()), bm_list.getName());
        component_hash.put(bm_list, sub_menu);
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
                    Component comp = component_hash.get(node);
                    if (comp instanceof JRPMenu) {
                        ((JRPMenu) comp).setText(node.getName());
                    }
                } else if (o instanceof Bookmark) {
                    //addBookmarkMI(bl_menu, (Bookmark) o);
                    Component comp = component_hash.get(o);
                    if (comp != null && comp instanceof BookmarkJMenuItem) {
                        ((BookmarkJMenuItem) comp).setText(((Bookmark) o).getName());
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

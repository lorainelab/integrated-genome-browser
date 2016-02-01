/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bookmarks;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import static com.affymetrix.igb.bookmarks.BookmarkManagerView.BUNDLE;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.util.Set;
import org.lorainelab.igb.menu.api.MenuBarParentProvider;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true)
public class BookmarkParentMenu implements MenuBarParentProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BookmarkParentMenu.class);
    private static final String ADD_BOOKMARK_ICON = "bookmark-new.png";
    private static final String IMPORT_BOOKMARKS_ICON = "go-bottom.png";
    private static final int MENU_WEIGHT = 10;
    private IgbService igbService;

    @Override
    public MenuItem getParentMenuItem() {
        Set<MenuItem> children = Sets.newLinkedHashSet();
        children.add(getAddBookmarkMenuItem());
        children.add(getImportBookmarkActionMenuItem());
//        MenuUtil.addToMenu(bookmarkMenu, new JRPMenuItem("Bookmark_export", ExportBookmarkAction.getAction()));
//        MenuUtil.addToMenu(bookmarkMenu, new JRPMenuItem("Bookmark_clipboard", CopyBookmarkAction.getAction()));
        MenuItem parentMenuItem = new MenuItem(BUNDLE.getString("bookmarksMenu"), children);
        parentMenuItem.setMnemonic('b');
        return parentMenuItem;
    }

    private MenuItem getAddBookmarkMenuItem() {
        MenuItem addBookMarkActionMenu = new MenuItem(BUNDLE.getString("addBookmark"), (Void t) -> {
            BookmarkEditor.run(igbService.getSeqMapView().getVisibleSpan());
            return t;
        });
        try (InputStream resourceAsStream = BookmarkParentMenu.class.getClassLoader().getResourceAsStream(ADD_BOOKMARK_ICON)) {
            addBookMarkActionMenu.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        addBookMarkActionMenu.setWeight(1);
        return addBookMarkActionMenu;
    }

    private MenuItem getImportBookmarkActionMenuItem() {
        MenuItem importBookMarksActionMenuItem = new MenuItem(BUNDLE.getString("importBookmarks"), (Void t) -> {
            BookmarkManagerView.getSingleton().importBookmarks();
            return t;
        });
        try (InputStream resourceAsStream = BookmarkParentMenu.class.getClassLoader().getResourceAsStream(IMPORT_BOOKMARKS_ICON)) {
            importBookMarksActionMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        importBookMarksActionMenuItem.setWeight(2);
        return importBookMarksActionMenuItem;
    }

    @Override
    public int getWeight() {
        return MENU_WEIGHT;
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

}

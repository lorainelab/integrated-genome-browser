package com.lorainelab.igb.session;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.affymetrix.igb.bookmarks.service.BookmarkService;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.google.common.base.Charsets;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URLDecoder;
import java.util.ResourceBundle;
import java.util.prefs.InvalidPreferencesFormatException;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;

@Component(name = LoadSessionAction.COMPONENT_NAME, immediate = true, provide = {IgbMenuItemProvider.class, GenericAction.class})
public class LoadSessionAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "LoadSessionAction";
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private static final int MENU_POSITION = 8;
    private static final long serialVersionUID = 1l;
    private IgbService igbService;
    private BookmarkService bookmarkService;

    final public static boolean IS_MAC
            = System.getProperty("os.name").toLowerCase().contains("mac");

    public LoadSessionAction() {
        super(BUNDLE.getString("loadSession"), BUNDLE.getString("openSessionTooltip"),
                "16x16/actions/load_session.png", "22x22/actions/load_session.png",
                KeyEvent.VK_L, null, true);
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (IS_MAC) {
            FileDialog fd = new FileDialog(igbService.getApplicationFrame());
            fd.setDirectory(System.getProperty("user.home"));
            fd.setFile("*.xml");
            fd.setLocation(50, 50);
            fd.setVisible(true);

            if (fd.getFile() != null) {
                File selectedFile = new File(fd.getDirectory(), fd.getFile());
                try {
                    loadSession(selectedFile);
                } catch (InvalidPreferencesFormatException ipfe) {
                    ErrorHandler.errorPanel("ERROR", "Invalid preferences format:\n" + ipfe.getMessage()
                            + "\n\nYou can only load a session from a file that was created with save session.");
                } catch (Exception x) {
                    ErrorHandler.errorPanel("ERROR", "Error loading session from file", x);
                }
            }

        } else {
            JFileChooser chooser = PreferenceUtils.getJFileChooser();
            int option = chooser.showOpenDialog(igbService.getApplicationFrame().getContentPane());
            if (option == JFileChooser.APPROVE_OPTION) {
                try {
                    loadSession(chooser.getSelectedFile());
                } catch (InvalidPreferencesFormatException ipfe) {
                    ErrorHandler.errorPanel("ERROR", "Invalid preferences format:\n" + ipfe.getMessage()
                            + "\n\nYou can only load a session from a file that was created with save session.");
                } catch (Exception x) {
                    ErrorHandler.errorPanel("ERROR", "Error loading session from file", x);
                }
            }
        }
    }

    public void loadSession(File f) throws Exception {
        PreferenceUtils.importPreferences(f);
        igbService.loadState();
        String bk_url = PreferenceUtils.getSessionPrefsNode().get("bookmark", "");
        if (bk_url.length() <= 0) {
            StringBuilder buffer = new StringBuilder();
            int j = 0;
            while (true) {
                String sb_bk_url = PreferenceUtils.getSessionPrefsNode().get("bookmark" + j++, "");
                if (sb_bk_url.length() <= 0) {
                    bk_url = buffer.toString();
                    break;
                }
                buffer.append(sb_bk_url);
            }
        }

        String url = URLDecoder.decode(bk_url, Charsets.UTF_8.displayName());
        if (url != null && url.trim().length() > 0) {
            Bookmark bookmark = new Bookmark(null, "", url);
            bookmarkService.loadBookmark(bookmark);
        }
        PreferenceUtils.getSessionPrefsNode().removeNode();
    }

    @Reference
    public void setBookmarkService(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @Override
    public String getParentMenuName() {
        return "file";
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return new JRPMenuItem("Bookmark_loadSession", this);
    }

    @Override
    public int getMenuItemWeight() {
        return MENU_POSITION;
    }

}

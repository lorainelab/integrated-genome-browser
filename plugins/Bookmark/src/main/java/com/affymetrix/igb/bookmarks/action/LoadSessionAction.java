package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import com.affymetrix.igb.service.api.IGBService;
import com.google.common.base.Charsets;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;  
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URLDecoder;
import java.util.prefs.InvalidPreferencesFormatException;
import javax.swing.JFileChooser;

public class LoadSessionAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private IGBService igbService;
    private static LoadSessionAction ACTION;
    final public static boolean IS_MAC
            = System.getProperty("os.name").toLowerCase().contains("mac");

    public static void createAction(IGBService igbService) {
        ACTION = new LoadSessionAction(igbService);
    }

    public static LoadSessionAction getAction() {
        return ACTION;
    }

    private LoadSessionAction(IGBService igbService) {
        super(BookmarkManagerView.BUNDLE.getString("loadSession"), BookmarkManagerView.BUNDLE.getString("openSessionTooltip"),
                "16x16/actions/load_session.png", "22x22/actions/load_session.png",
                KeyEvent.VK_L, null, true);
        this.igbService = igbService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (IS_MAC) {
            FileDialog fd = new FileDialog(igbService.getFrame());
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
            int option = chooser.showOpenDialog(igbService.getFrame().getContentPane());
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
            BookmarkController.viewBookmark(igbService, new Bookmark(null, "", url));
        }
        PreferenceUtils.getSessionPrefsNode().removeNode();
    }
}

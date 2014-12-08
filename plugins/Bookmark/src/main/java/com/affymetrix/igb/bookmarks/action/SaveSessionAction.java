package com.affymetrix.igb.bookmarks.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkManagerView;
import com.affymetrix.igb.osgi.service.IGBService;
import com.google.common.base.Charsets;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;

public class SaveSessionAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private IGBService igbService;
    final public static boolean IS_MAC
            = System.getProperty("os.name").toLowerCase().contains("mac");

    FilenameFilter fileNameFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".xml");
        }
    };

    private static SaveSessionAction ACTION;

    public static void createAction(IGBService igbService) {
        ACTION = new SaveSessionAction(igbService);
    }

    public static SaveSessionAction getAction() {
        return ACTION;
    }

    private SaveSessionAction(IGBService igbService) {
        super(BookmarkManagerView.BUNDLE.getString("saveSession"), BookmarkManagerView.BUNDLE.getString("saveSessionTooltip"),
                "16x16/actions/save_session.png", "22x22/actions/save_session.png",
                KeyEvent.VK_S, null, true);
        this.igbService = igbService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
        String dateString = sdf.format(today);
        String defaultFileName = "igbSession-" + dateString + ".xml";

        if (IS_MAC) {
            showFileDialog(defaultFileName);
        } else {
            showJFileChooser(defaultFileName);
        }

    }

    private void showFileDialog(String defaultFileName) {
        FileDialog dialog = new FileDialog(igbService.getFrame(), "Save Session", FileDialog.SAVE);
        dialog.setFilenameFilter(fileNameFilter);
        dialog.setFile(defaultFileName);
        dialog.setVisible(true);
        String fileS = dialog.getFile();
        if (fileS != null) {
            File sessionFile = new File(dialog.getDirectory(), dialog.getFile());
            saveSession(sessionFile);
        }
    }

    private void showJFileChooser(String defaultFileName) {
        JFileChooser chooser = PreferenceUtils.getJFileChooser();
        File sessionFile = new File(System.getProperty("user.home") + "/" + defaultFileName);
        chooser.setSelectedFile(sessionFile);
        int option = chooser.showSaveDialog(igbService.getFrame().getContentPane());
        if (option == JFileChooser.APPROVE_OPTION) {
            saveSession(chooser.getSelectedFile());
        }
    }

    public void saveSession(File f) {
        try {
            igbService.saveState();
            Bookmark bookmark = BookmarkController.getCurrentBookmark(true,
                    igbService.getSeqMapView().getVisibleSpan());
            if (bookmark != null) {
                String bk = URLEncoder.encode(bookmark.getURL().toString(), Charsets.UTF_8.displayName());
                if (bk.length() < Preferences.MAX_VALUE_LENGTH) {
                    PreferenceUtils.getSessionPrefsNode().put("bookmark", bk);
                } else {
                    int j = 0;
                    for (int i = 0; i < bk.length(); i += Preferences.MAX_VALUE_LENGTH) {
                        String sb_bk = bk.substring(i, Math.min(bk.length(), i + Preferences.MAX_VALUE_LENGTH));
                        PreferenceUtils.getSessionPrefsNode().put("bookmark" + j++, sb_bk);
                    }
                }
            }
            Preferences topNode = PreferenceUtils.getTopNode();
            Preferences toolBarPref = PreferenceUtils.getToolbarNode();
            Preferences keyStrokePref = PreferenceUtils.getKeystrokesNode();

            Map<String, Object> toolBarKeyvalue = PreferenceUtils.getEntryMapFromNode(toolBarPref);
            Map<String, Object> keyStrokeKeyvalue = PreferenceUtils.getEntryMapFromNode(keyStrokePref);

            toolBarPref.removeNode();
            keyStrokePref.removeNode();

            PreferenceUtils.exportPreferences(topNode, f);

            topNode.node(toolBarPref.name());
            topNode.node(keyStrokePref.name());

            PreferenceUtils.addEntryToNode(PreferenceUtils.getToolbarNode(), toolBarKeyvalue);
            PreferenceUtils.addEntryToNode(PreferenceUtils.getKeystrokesNode(), keyStrokeKeyvalue);            

            PreferenceUtils.getSessionPrefsNode().removeNode();
        } catch (Exception x) {
            ErrorHandler.errorPanel("ERROR", "Error saving session to file", x);
        }
    }
}

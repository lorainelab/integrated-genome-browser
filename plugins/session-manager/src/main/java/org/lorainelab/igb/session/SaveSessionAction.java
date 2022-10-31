package org.lorainelab.igb.session;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.FileTracker;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.model.Bookmark;
import com.affymetrix.igb.bookmarks.service.BookmarkService;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javafx.stage.FileChooser;
import org.lorainelab.igb.javafx.FileChooserUtil;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

@Component(name = SaveSessionAction.COMPONENT_NAME, immediate = true, provide = {MenuBarEntryProvider.class, GenericAction.class})
public class SaveSessionAction extends GenericAction implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SaveSessionAction.class);
    public static final String COMPONENT_NAME = "SaveSessionAction";
    private static final String SAVE_SESSION_ICON = "save_session.png";
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private static final long serialVersionUID = 1L;
    private IgbService igbService;
    private BookmarkService bookmarkService;
    private final int TOOLBAR_INDEX = 1;
    private static final int MENU_POSITION = 50;

    public SaveSessionAction() {
        super(BUNDLE.getString("saveSession"), BUNDLE.getString("saveSessionTooltip"),
                "16x16/actions/save_session.png", "22x22/actions/save_session.png",
                KeyEvent.VK_S, null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
        String dateString = sdf.format(today);
        String defaultFileName = "igbSession-" + dateString + ".xml";
        showJFileChooser(defaultFileName);
    }

    private void showJFileChooser(String defaultFileName) {
        // IGBF-1151: JFileChooser displays JavaFX style dialog. 
        // Instead we want OS Native file choooser (Windows and Linux). 
        // Thus we are using FileChooser for opening a dialog.
        int extensionIndex;
        String filePath;
        File fil;
        boolean extSet = false;
        String file_ext;
        FileTracker fileTracker = FileTracker.DATA_DIR_TRACKER;
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML File", "*.xml"); 
        fileTracker.setFile(new File(System.getProperty("user.home")));
        FileChooserUtil fileChooser = FileChooserUtil.build() ;
        java.util.Optional<File> selectedFile = fileChooser
                    .setContext(fileTracker.getFile())
                    .setDefaultFileName(defaultFileName)
                    .setTitle("Save Session")
                    .setFileExtensionFilters(Lists.newArrayList(extFilter))
                    .saveFilesFromFxChooser();
        
        FileChooser.ExtensionFilter ext = fileChooser.getSelectedFileExtension();
        if (selectedFile.isPresent()) {
            fileTracker.setFile(selectedFile.get().getParentFile());
            try {
                fil = selectedFile.get();
                filePath = fil.getAbsolutePath().replace("*.", "");

                file_ext = filePath.substring(filePath.lastIndexOf(".") + 1);
                extensionIndex = filePath.indexOf(file_ext);

                if (extensionIndex <= 0) {
                    for (String extension : ext.getExtensions()) {
                       if (filePath.endsWith(extension.replace("*", ""))) {
                           extSet = true;
                           break;
                       }
                    }
                    if (!extSet) {
                       filePath = filePath.concat(ext.getExtensions().get(0).replace("*", ""));
                    }
                   fil = new File(filePath);
                } else {
                   fil = new File(filePath.substring(0, extensionIndex + file_ext.length()));
                }
                
                saveSession(fil);
            } catch (Exception ex) {
                com.affymetrix.genometry.util.ErrorHandler.errorPanel("Error exporting session", ex, Level.SEVERE);
            }
        }
    }

    public void saveSession(File f) {
        try {
            igbService.saveState();
            Optional<Bookmark> bookmark = bookmarkService.getCurrentBookmark();
            if (bookmark.isPresent()) {
                String bk = URLEncoder.encode(bookmark.get().getURL().toString(), Charsets.UTF_8.displayName());
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

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference(optional = false)
    public void setBookmarkService(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @Override
    public java.util.Optional<List<MenuItem>> getMenuItems() {
        MenuItem menuItem = new MenuItem(BUNDLE.getString("saveSession"), (Void t) -> {
            actionPerformed(null);
            return t;
        });
        try (InputStream resourceAsStream = SaveSessionAction.class.getClassLoader().getResourceAsStream(SAVE_SESSION_ICON)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_POSITION);
        return java.util.Optional.of(Arrays.asList(menuItem));
    }

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.FILE;
    }
}

package com.affymetrix.igb.action;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.prefs.PreferencesPanel;
import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.prefs.InvalidPreferencesFormatException;
import javax.swing.JFileChooser;

public class ImportPreferencesAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final ImportPreferencesAction ACTION = new ImportPreferencesAction();
    private final static String IMPORT_ACTION_COMMAND = PreferencesPanel.WINDOW_NAME + " / " + BUNDLE.getString("ImportPreferences");

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ImportPreferencesAction getAction() {
        return ACTION;
    }

    private ImportPreferencesAction() {
        super(BUNDLE.getString("ImportPreferences"), null, null,/*"16x16/actions/pref_import.png",*/
                null, KeyEvent.VK_I, null, true);
        putValue(ACTION_COMMAND_KEY, IMPORT_ACTION_COMMAND);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        super.actionPerformed(ae);
        JFileChooser chooser = GeneralUtils.getJFileChooser();
        int option = chooser.showOpenDialog(PreferencesPanel.getSingleton());
        if (option == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                PreferenceUtils.importPreferences(f);
            } catch (InvalidPreferencesFormatException ipfe) {
                ErrorHandler.errorPanel("ERROR", "Invalid preferences format:\n" + ipfe.getMessage()
                        + "\n\nYou can only IMPORT preferences from a file that was created with EXPORT.  "
                        + "In particular, you cannot import the file 'igb_prefs.xml' that was "
                        + "used in earlier versions of this program.");
            } catch (Exception e) {
                ErrorHandler.errorPanel("ERROR", "Error importing preferences from file", e);
            }
        }
        PreferencesPanelProvider[] components = PreferencesPanel.getSingleton().getPrefEditorComponents();
        for (PreferencesPanelProvider component : components) {
            component.refresh();
        }
    }
}

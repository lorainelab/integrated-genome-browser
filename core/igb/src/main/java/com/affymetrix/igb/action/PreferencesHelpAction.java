package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.prefs.PreferencesPanel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class PreferencesHelpAction extends HelpActionA {

    private static final long serialVersionUID = 1L;
    private static final PreferencesHelpAction ACTION = new PreferencesHelpAction();
    private final static String HELP_ACTION_COMMAND = PreferencesPanel.WINDOW_NAME + " / " + BUNDLE.getString("PreferencesHelp");

    public static PreferencesHelpAction getAction() {
        return ACTION;
    }

    private PreferencesHelpAction() {
        super(BUNDLE.getString("PreferencesHelp"), null, null, null, /*"16x16/apps/help-browser.png","22x22/apps/help-browser.png",*/ KeyEvent.VK_G, null, true);
        putValue(ACTION_COMMAND_KEY, HELP_ACTION_COMMAND);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        super.actionPerformed(ae);
        showHelpForPanel(PreferencesPanel.getSingleton(), PreferencesPanel.getSingleton());
    }
}

package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.NoToolbarActions;
import com.lorainelab.igb.services.window.preferences.IPrefEditorComponent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;

@aQute.bnd.annotation.component.Component(name = PreferencesHelpTabAction.COMPONENT_NAME, immediate = true, provide = {NoToolbarActions.class})
public class PreferencesHelpTabAction extends HelpActionA implements NoToolbarActions{

    public static final String COMPONENT_NAME = "PreferencesHelpTabAction";
    private static final long serialVersionUID = 1L;
    private static final PreferencesHelpTabAction ACTION = new PreferencesHelpTabAction();
    private final static String HELP_TAB_ACTION_COMMAND = PreferencesPanel.WINDOW_NAME + " / " + BUNDLE.getString("PreferencesHelpForCurrentTab");

    public static PreferencesHelpTabAction getAction() {
        return ACTION;
    }

    private PreferencesHelpTabAction() {
        super(BUNDLE.getString("PreferencesHelpForCurrentTab"), null, null, null, /*"16x16/actions/tab-new.png","22x22/actions/tab-new.png",*/ KeyEvent.VK_C, null, true);
        putValue(ACTION_COMMAND_KEY, HELP_TAB_ACTION_COMMAND);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        super.actionPerformed(ae);
        showHelpForTab();
    }

    private void showHelpForTab() {
        Component c = PreferencesPanel.getSingleton().getSelectedTabComponent();
        if (c instanceof IPrefEditorComponent) {
            IPrefEditorComponent pec = (IPrefEditorComponent) c;
            showHelpForPanel(PreferencesPanel.getSingleton(), pec);
        } else {
            JOptionPane.showMessageDialog(PreferencesPanel.getSingleton(), "No help available for this tab",
                    "No Help", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

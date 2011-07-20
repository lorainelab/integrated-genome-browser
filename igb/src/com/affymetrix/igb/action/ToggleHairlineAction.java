package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.UnibrowHairline;
import java.util.prefs.PreferenceChangeEvent;

/**
 *
 * @author hiralv
 */
public class ToggleHairlineAction extends javax.swing.AbstractAction implements java.util.prefs.PreferenceChangeListener{
	
	private static final long serialVersionUID = 1;
	private static final ToggleHairlineAction ACTION = new ToggleHairlineAction();

	private ToggleHairlineAction() {
		super("Keep zoom stripe in view");
		//this.putValue(MNEMONIC_KEY, java.awt.event.KeyEvent.VK_H);
		/* TODO: This is only correct for English Locale" */
		//this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);

		this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, UnibrowHairline.default_keep_hairline_in_view));
		PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
	}

	public static ToggleHairlineAction getAction() {
		return ACTION;
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		PreferenceUtils.getTopNode().putBoolean(
				UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, (Boolean)getValue(SELECTED_KEY));
	}

	public void preferenceChange(PreferenceChangeEvent pce) {
		if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }
		if (pce.getKey().equals(UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW)) {
			actionPerformed(null);
        }
	}

	
}

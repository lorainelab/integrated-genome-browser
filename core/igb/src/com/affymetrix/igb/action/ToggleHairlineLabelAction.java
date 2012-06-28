package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.UnibrowHairline;

import java.awt.event.KeyEvent;
import java.util.prefs.PreferenceChangeListener;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ToggleHairlineLabelAction.java 11366 2012-05-02 14:57:55Z anuj4159 $
 */
public class ToggleHairlineLabelAction extends GenericAction implements PreferenceChangeListener {
	private static final long serialVersionUID = 1;
	private static final ToggleHairlineLabelAction ACTION = new ToggleHairlineLabelAction();

	private ToggleHairlineLabelAction() {
		super(BUNDLE.getString("toggleHairlineLabel"), null, "16x16/apps/office-calender.png", null, KeyEvent.VK_H);
		/* TODO: This is only correct for English Locale" */
		this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);

		this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				UnibrowHairline.PREF_HAIRLINE_LABELED, UnibrowHairline.default_show_hairline_label));
		PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ToggleHairlineLabelAction getAction() {
		return ACTION;
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		PreferenceUtils.getTopNode().putBoolean(
				UnibrowHairline.PREF_HAIRLINE_LABELED, (Boolean)getValue(SELECTED_KEY));
	}

	public void preferenceChange(java.util.prefs.PreferenceChangeEvent pce) {
		if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }
		if (pce.getKey().equals(UnibrowHairline.PREF_HAIRLINE_LABELED)) {
			this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				UnibrowHairline.PREF_HAIRLINE_LABELED, UnibrowHairline.default_show_hairline_label));
        }
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}

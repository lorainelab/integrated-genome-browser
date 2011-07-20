package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.UnibrowHairline;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ToggleHairlineLabelAction extends AbstractAction {
	private static final long serialVersionUID = 1;
	private static final ToggleHairlineLabelAction ACTION = new ToggleHairlineLabelAction();

	private ToggleHairlineLabelAction() {
		super(BUNDLE.getString("toggleHairlineLabel"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_H);
		/* TODO: This is only correct for English Locale" */
		this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);

		this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				UnibrowHairline.PREF_HAIRLINE_LABELED, UnibrowHairline.default_show_hairline_label));
	}

	public static ToggleHairlineLabelAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		PreferenceUtils.getTopNode().putBoolean(
				UnibrowHairline.PREF_HAIRLINE_LABELED, (Boolean)getValue(SELECTED_KEY));
	}

}

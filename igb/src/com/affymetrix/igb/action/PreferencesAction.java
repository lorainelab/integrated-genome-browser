package com.affymetrix.igb.action;

import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.util.List;
import com.affymetrix.igb.Application;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.prefs.PreferencesPanel;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class PreferencesAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final PreferencesAction ACTION = new PreferencesAction();

	public static PreferencesAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		Application igb = Application.getSingleton();
		List<TierLabelGlyph> tier_label_glyphs = null;
		if (igb != null) {
			tier_label_glyphs = igb.getMapView().getTierManager().getSelectedTierLabels();
		}

		PreferencesPanel p = PreferencesPanel.getSingleton();
		p.setTab(PreferencesPanel.TAB_NUM_TIERS);
		p.tpvGUI.tpv.setTier_label_glyphs(tier_label_glyphs);
		p.getFrame().setVisible(true);
	}

	@Override
	public String getText() {
		return BUNDLE.getString("preferences");
	}

	@Override
	public String getIconPath() {
		return null;
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_E;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}

package com.affymetrix.igb.action;

import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.util.List;
import com.affymetrix.igb.Application;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.prefs.PreferencesPanel;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.prefs.TierPrefsView;

/**
 *
 * @author sgblanch
 * @version $Id$
 * Modified by nick
 */
public class PreferencesAction extends GenericAction {

	private static final long serialVersionUID = 1l;
	private static final PreferencesAction ACTION = new PreferencesAction();

	public static PreferencesAction getAction() {
		return ACTION;
	}

	private PreferencesAction() {
		super(BUNDLE.getString("preferences"), null, "toolbarButtonGraphics/general/Preferences16.gif", KeyEvent.VK_E, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		Application igb = Application.getSingleton();
		List<TierLabelGlyph> tier_label_glyphs = null;
		if (igb != null) {
			tier_label_glyphs = igb.getMapView().getTierManager().getSelectedTierLabels();
		}

		PreferencesPanel p = PreferencesPanel.getSingleton();
		p.setTab(PreferencesPanel.TAB_TIER_PREFS_VIEW);
		((TierPrefsView)p.tpvGUI.tdv).setTier_label_glyphs(tier_label_glyphs);
		p.getFrame().setVisible(true);
	}
}

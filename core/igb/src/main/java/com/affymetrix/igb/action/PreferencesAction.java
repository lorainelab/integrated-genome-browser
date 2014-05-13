package com.affymetrix.igb.action;

import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.util.List;
import com.affymetrix.igb.Application;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.prefs.PreferencesPanel;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: PreferencesAction.java 11361 2012-05-02 14:46:42Z anuj4159 $
 * Modified by nick
 */
public class PreferencesAction extends GenericAction {

	private static final long serialVersionUID = 1l;
	private static final PreferencesAction ACTION = new PreferencesAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static PreferencesAction getAction() {
		return ACTION;
	}

	private PreferencesAction() {
		super(BUNDLE.getString("Preferences"), BUNDLE.getString("preferencesTooltip"),
				"16x16/actions/preferences.png",
				"22x22/actions/preferences.png",
				KeyEvent.VK_E, null, true);
		this.ordinal = -9006100;
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
//		p.setTab(PreferencesPanel.TAB_TIER_PREFS_VIEW);
//		((TierPrefsView)p.tpvGUI.tdv).setTier_label_glyphs(tier_label_glyphs);
		p.getFrame().setVisible(true);
	}
}

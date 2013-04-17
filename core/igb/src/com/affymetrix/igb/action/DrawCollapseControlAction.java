
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.AbstractAction;
/**
 *
 * @author hiralv
 */
public class DrawCollapseControlAction extends GenericAction implements PreferenceChangeListener{
	private static final long serialVersionUID = 1L;
	private static final DrawCollapseControlAction ACTION = new DrawCollapseControlAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static DrawCollapseControlAction getAction() {
		return ACTION;
	}

	private DrawCollapseControlAction() {
		super(BUNDLE.getString("drawCollapseControl"), "16x16/actions/blank_placeholder.png", null);
		this.putValue(SELECTED_KEY, IGBStateProvider.getDrawCollapseState());
		this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				PreferenceUtils.SHOW_COLLAPSE_OPTION, PreferenceUtils.default_show_collapse_option));
		PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean b = (Boolean)getValue(SELECTED_KEY);
		IGBStateProvider.setDrawCollapseControl(b);
		((IGB) IGB.getSingleton()).getMapView().getSeqMap().updateWidget();
		ACTION.putValue(AbstractAction.SELECTED_KEY, IGBStateProvider.getDrawCollapseState());
		PreferenceUtils.getTopNode().putBoolean(
				PreferenceUtils.SHOW_COLLAPSE_OPTION, (Boolean)getValue(SELECTED_KEY));
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	public void preferenceChange(PreferenceChangeEvent pce) {
		if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }
		if (pce.getKey().equals(PreferenceUtils.SHOW_COLLAPSE_OPTION)) {
			this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				PreferenceUtils.SHOW_COLLAPSE_OPTION, PreferenceUtils.default_show_collapse_option));
			IGBStateProvider.setDrawCollapseControl((Boolean)(this.getValue(SELECTED_KEY)));
			((IGB) IGB.getSingleton()).getMapView().getSeqMap().updateWidget();
        }
	}
}

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.IGB;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.util.prefs.PreferenceChangeListener;
/**
 *
 * @author hiralv
 */
public class ToggleEdgeMatchingAction extends GenericAction implements PreferenceChangeListener{
	private static final long serialVersionUID = 1;
	private static final ToggleEdgeMatchingAction ACTION = new ToggleEdgeMatchingAction();
	private SeqMapView map_view = IGB.getSingleton().getMapView();

	private ToggleEdgeMatchingAction(){
		super(BUNDLE.getString("toggleEdgeMatching"), KeyEvent.VK_M);
		this.putValue(SELECTED_KEY, map_view.getEdgeMatching());
		this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				PreferenceUtils.SHOW_EDGEMATCH_OPTION, PreferenceUtils.default_show_edge_match));
		PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ToggleEdgeMatchingAction getAction(){
		return ACTION;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		map_view.setEdgeMatching((Boolean)(this.getValue(SELECTED_KEY)));
		ACTION.putValue(AbstractAction.SELECTED_KEY, map_view.getEdgeMatching());
		PreferenceUtils.getTopNode().putBoolean(
				PreferenceUtils.SHOW_EDGEMATCH_OPTION, (Boolean)getValue(SELECTED_KEY));
	}
	
	@Override
	public void preferenceChange(java.util.prefs.PreferenceChangeEvent pce) {
		if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }
		if (pce.getKey().equals(PreferenceUtils.SHOW_EDGEMATCH_OPTION)) {
			this.putValue(SELECTED_KEY, PreferenceUtils.getBooleanParam(
				PreferenceUtils.SHOW_EDGEMATCH_OPTION, PreferenceUtils.default_show_edge_match));
			map_view.setEdgeMatching((Boolean)(this.getValue(SELECTED_KEY)));
        }
	}
}

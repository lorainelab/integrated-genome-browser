package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.tiers.TrackConstants;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class DrawCollapseControlAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final DrawCollapseControlAction ACTION = new DrawCollapseControlAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        PreferenceUtils.saveToPreferences(TrackConstants.PREF_DRAW_COLLAPSE_ICON, TrackConstants.default_draw_collapse_icon, ACTION);
    }

    public static DrawCollapseControlAction getAction() {
        return ACTION;
    }

    private DrawCollapseControlAction() {
        super(BUNDLE.getString("drawCollapseControl"), "16x16/actions/blank_placeholder.png", null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        boolean b = (Boolean) getValue(SELECTED_KEY);
        IGBStateProvider.setDrawCollapseControl(b);
        ((IGB) IGB.getSingleton()).getMapView().getSeqMap().updateWidget();
    }

    @Override
    public boolean isToggle() {
        return true;
    }
}

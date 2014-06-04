package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.KeyEvent;

/**
 *
 * @author hiralv
 */
public class ToggleEdgeMatchingAction extends GenericAction {

    private static final long serialVersionUID = 1;
    private static final ToggleEdgeMatchingAction ACTION = new ToggleEdgeMatchingAction();

    private ToggleEdgeMatchingAction() {
        super(BUNDLE.getString("toggleEdgeMatching"), null, "16x16/actions/blank_placeholder.png", null, KeyEvent.VK_M);
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        PreferenceUtils.saveToPreferences(PreferenceUtils.SHOW_EDGEMATCH_OPTION, PreferenceUtils.default_show_edge_match, ACTION);
    }

    public static ToggleEdgeMatchingAction getAction() {
        return ACTION;
    }

}

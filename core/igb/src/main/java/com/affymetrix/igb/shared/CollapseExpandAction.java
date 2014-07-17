package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.action.CollapseAction;
import com.affymetrix.igb.action.ExpandAction;
import com.affymetrix.igb.action.SeqMapToggleAction;
import com.affymetrix.igb.action.SeqMapViewActionA;

/**
 * Toggles between a {@link CollapseAction} and a {@link ExpandAction}.
 */
public class CollapseExpandAction extends SeqMapToggleAction {

    private static final long serialVersionUID = 1L;
    private static final CollapseExpandAction ACTION
            = new CollapseExpandAction(
                    ExpandAction.getAction(),
                    CollapseAction.getAction()
            );

    /**
     * Load the class and, so, run the static code which creates a singleton.
     * Add it to the {@link GenericActionHolder}. This is in lieu of the static
     * {@link #getAction} method used by other actions.
     */
    public static void createSingleton() {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        ACTION.setEnabled(CollapseAction.getAction().isEnabled()
                || ExpandAction.getAction().isEnabled());
    }

    protected CollapseExpandAction(SeqMapViewActionA one, SeqMapViewActionA two) {
        super(one, two);
        this.ordinal = Math.min(one.getOrdinal(), two.getOrdinal()) - 1;
    }

}

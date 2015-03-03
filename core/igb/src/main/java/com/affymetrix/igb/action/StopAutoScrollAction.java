package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
public class StopAutoScrollAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1l;
    private static StopAutoScrollAction ACTION = new StopAutoScrollAction();

    private StopAutoScrollAction() {
        super(BUNDLE.getString("stopAutoScroll"), BUNDLE.getString("stopAutoscrollTooltip"), "16x16/actions/autoscroll_stop.png",
                "22x22/actions/autoscroll_stop.png", 0);
        setEnabled(false);
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static StopAutoScrollAction getAction() {
        return ACTION;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        getSeqMapView().getAutoScroll().stop();
        setEnabled(false);
        StartAutoScrollAction.getAction().setEnabled(true);
        getSeqMapView().getAutoLoadAction().loadData();
    }
    
    @Override
    public boolean isToolbarAction() {
        return false;
    }
}

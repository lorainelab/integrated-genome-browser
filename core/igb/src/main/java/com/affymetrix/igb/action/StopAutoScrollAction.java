package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.NoToolbarActions;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 */
@Component(name = StopAutoScrollAction.COMPONENT_NAME, immediate = true, provide = NoToolbarActions.class)
public class StopAutoScrollAction extends SeqMapViewActionA implements NoToolbarActions {

    public static final String COMPONENT_NAME = "StopAutoScrollAction";
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
}

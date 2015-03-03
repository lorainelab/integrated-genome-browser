package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.NoToolbarActions;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author hiralv
 */
@Component(name = StartAutoScrollAction.COMPONENT_NAME, immediate = true, provide = NoToolbarActions.class)
public class StartAutoScrollAction extends SeqMapViewActionA implements NoToolbarActions {

    public static final String COMPONENT_NAME = "StartAutoScrollAction";
    private static final long serialVersionUID = 1l;
    private static StartAutoScrollAction ACTION = new StartAutoScrollAction();

    private StartAutoScrollAction() {
        super(BUNDLE.getString("startAutoScroll"), BUNDLE.getString("autoscrollTooltip"), "16x16/actions/autoscroll.png",
                "22x22/actions/autoscroll.png", 0);
        setEnabled(true);
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static StartAutoScrollAction getAction() {
        return ACTION;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        start();
    }

    public void start() {
        // Calculate start, end and bases per pixels
        Rectangle2D.Double cbox = getTierMap().getViewBounds();
        int start_pos = (int) cbox.x;
        // Temporary fix to avoid npe. 
        if (getSeqMapView().getViewSeq() == null) {
            return;
        }
        int end_pos = getSeqMapView().getViewSeq().getLength();

        getSeqMapView().getAutoScroll().configure(this.getTierMap(), start_pos, end_pos);
        getSeqMapView().getAutoScroll().start(this.getTierMap());
        setEnabled(false);
        StopAutoScrollAction.getAction().setEnabled(true);
    }

//	@Override
//	public boolean isEnabled(){
//		return getSeqMapView().getViewSeq() != null;
//	}
}

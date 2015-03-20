package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.ContinuousAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.event.ActionEvent;

public class ScrollLeftAction extends SeqMapViewActionA implements ContinuousAction {

    private static final long serialVersionUID = 1L;
    private static ScrollLeftAction ACTION = new ScrollLeftAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ScrollLeftAction getAction() {
        return ACTION;
    }

    public ScrollLeftAction() {
        super("Scroll Left", BUNDLE.getString("leftGreenArrowTooltip"), "16x16/actions/go-previous.png",
                "22x22/actions/go-previous.png", 0);
        this.ordinal = -4007010;
        setKeyStrokeBinding("ctrl LEFT");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        AffyTieredMap seqmap = getSeqMapView().getSeqMap();
        int[] visible = seqmap.getVisibleRange();
        seqmap.scroll(NeoAbstractWidget.X, visible[0] - (visible[1] - visible[0]) / 10);
        seqmap.updateWidget();
        getSeqMapView().getAutoLoadAction().loadData();
    }
}

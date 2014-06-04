package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.ContinuousAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.event.ActionEvent;

public class ScrollDownAction extends SeqMapViewActionA implements ContinuousAction {

    private static final long serialVersionUID = 1L;
    private static final ScrollDownAction ACTION = new ScrollDownAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ScrollDownAction getAction() {
        return ACTION;
    }

    public ScrollDownAction() {
        super("Scroll Down", "16x16/actions/go-down.png",
                "22x22/actions/go-down.png");
        this.ordinal = -4007040;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        AffyTieredMap seqmap = getSeqMapView().getSeqMap();
        int[] visible = seqmap.getVisibleOffset();
        seqmap.scroll(NeoAbstractWidget.Y, visible[0] + (visible[1] - visible[0]) / 10);
        seqmap.updateWidget();
    }
}

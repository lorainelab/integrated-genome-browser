package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

public class ZoomOutXAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final ZoomOutXAction ACTION = new ZoomOutXAction();
    private static final ZoomOutXAction ICON_ONLY_ACTION = new ZoomOutXAction("");

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ZoomOutXAction getAction() {
        return ACTION;
    }

    public static ZoomOutXAction getIconOnlyAction() {
        return ICON_ONLY_ACTION;
    }

    public ZoomOutXAction() {
        super("Zoom Out Horizontally",
                "16x16/actions/list-remove.png", null
        );
        this.ordinal = -4004020;
        setKeyStrokeBinding("ctrl shift LEFT");
    }

    public ZoomOutXAction(String label) {
        super(label,
                "16x16/actions/list-remove.png", null
        );
        setKeyStrokeBinding("ctrl shift LEFT");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        AffyTieredMap seqmap = getSeqMapView().getSeqMap();
        Adjustable adj = seqmap.getZoomer(NeoMap.X);
        adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
        getSeqMapView().getAutoLoadAction().loadData();
    }
}

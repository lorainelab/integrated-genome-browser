package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

public class ZoomInXAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final ZoomInXAction ACTION = new ZoomInXAction();
    private static final ZoomInXAction ICON_ONLY_ACTION = new ZoomInXAction("");

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ZoomInXAction getAction() {
        return ACTION;
    }

    public static ZoomInXAction getIconOnlyAction() {
        return ICON_ONLY_ACTION;
    }

    public ZoomInXAction() {
        super("Zoom In Horizontally",
                "16x16/actions/list-add.png", null
        );
        this.ordinal = -4004010;
        setKeyStrokeBinding("ctrl shift RIGHT");
    }

    public ZoomInXAction(String label) {
        super(label,
                "16x16/actions/list-add.png", null
        );
        setKeyStrokeBinding("ctrl shift RIGHT");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        AffyTieredMap seqmap = getSeqMapView().getSeqMap();
        Adjustable adj = seqmap.getZoomer(NeoMap.X);
        adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
        getSeqMapView().getAutoLoadAction().loadData();
    }
}

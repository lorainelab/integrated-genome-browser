package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

public class ZoomInYAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final ZoomInYAction ACTION = new ZoomInYAction();
    private static final ZoomInYAction ICON_ONLY_ACTION = new ZoomInYAction("");

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ZoomInYAction getAction() {
        return ACTION;
    }

    public static ZoomInYAction getIconOnlyAction() {
        return ICON_ONLY_ACTION;
    }

    public ZoomInYAction() {
        super("Zoom In Vertically",
                "16x16/actions/list-add.png", null
        );
        this.ordinal = -4004110;
    }

    public ZoomInYAction(String label) {
        super(label,
                "16x16/actions/list-add.png", null
        );
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        AffyTieredMap seqmap = getSeqMapView().getSeqMap();
        Adjustable adj = seqmap.getZoomer(NeoMap.Y);
        adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
    }
}

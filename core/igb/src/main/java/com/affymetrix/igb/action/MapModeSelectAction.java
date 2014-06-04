package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * button action for SeqMapView modes
 */
public class MapModeSelectAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1l;

    public MapModeSelectAction(String id) {
        super(
                BUNDLE.getString(MapMode.MapSelectMode.name() + "Button"),
                BUNDLE.getString(MapMode.MapSelectMode.name() + "Tip"),
                "16x16/actions/arrow.png", null, //"22x22/actions/arrow.png",
                KeyEvent.VK_UNDEFINED
        );
        this.id = id;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        getSeqMapView().setMapMode(MapMode.MapSelectMode);
    }
}

package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * button action for SeqMapView modes
 */
public class MapModeScrollAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;

    public MapModeScrollAction(String id) {
        super(
                BUNDLE.getString(MapMode.MapScrollMode.name() + "Button"),
                BUNDLE.getString(MapMode.MapScrollMode.name() + "Tip"),
                "16x16/actions/open_hand.png", null, //"22x22/actions/open_hand.png",
                KeyEvent.VK_UNDEFINED
        );
        this.id = id;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        getSeqMapView().setMapMode(MapMode.MapScrollMode);
    }
}

package org.lorainelab.igb.track.operations;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.ThreadUtils;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;

import static com.affymetrix.igb.shared.Selections.*;

/**
 * Puts all selected graphs in separate tiers by setting the combo state of each
 * graph's state to null.
 */
public class SplitGraphsAction extends GenericAction {

    private static final long serialVersionUID = 1L;

    public SplitGraphsAction(IgbService igbService) {
        super("Split", null, null);
        this.igbService = igbService;
    }

    private final IgbService igbService;

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);

        for (GraphGlyph gg : graphGlyphs) {
            igbService.getSeqMapView().split(gg);
        }
        //igbService.getSeqMapView().postSelections();
        updateDisplay();
        //update the Data Management Table <Ivory Blakley> IGBF-201
        igbService.refreshDataManagementView();  
    }

    private void updateDisplay() {
        ThreadUtils.runOnEventQueue(() -> {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
            igbService.getSeqMapView().updatePanel(true, true);
        });
    }

}

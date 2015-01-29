package com.lorainelab.igb.track.operations;

import com.lorainelab.igb.track.operations.api.OperationsPanel;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.igb.service.api.IgbService;
import static com.affymetrix.igb.shared.Selections.graphGlyphs;
import static com.affymetrix.igb.shared.Selections.isAnyJoined;


/**
 *
 * @author hiralv
 */
public class GraphOperationsImpl extends OperationsPanel {

    private javax.swing.JButton combineB, splitB;
     boolean is_listening = true; // used to turn on and off listening to GUI events

    GraphOperationsImpl(IgbService igbServvie, FileTypeCategory[] categories) {
        super(igbServvie, categories);
    }

    @Override
    protected void initComponents(IgbService igbS) {
        combineB = new javax.swing.JButton(new CombineGraphsAction(igbS));
        splitB = new javax.swing.JButton(new SplitGraphsAction(igbS));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getBtPanel());
        getBtPanel().setLayout(layout);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(combineB)
                                .addComponent(splitB))));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(combineB)
                        .addComponent(splitB)));

        getSingleTrackLabel().setText("Single-Graph:");
        getMultiTrackLabel().setText("Multi-Graph:");
    }

    @Override
    public void setPanelEnabled(boolean enable) {
        super.setPanelEnabled(enable);
        isListening = false;

        combineB.setEnabled(enable && graphGlyphs.size() > 1 && !isAnyJoined());
        splitB.setEnabled(enable && isAnyJoined());

        isListening = true;
    }
}

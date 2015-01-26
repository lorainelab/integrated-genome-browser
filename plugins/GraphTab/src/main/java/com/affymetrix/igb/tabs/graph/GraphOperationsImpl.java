package com.affymetrix.igb.tabs.graph;

import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.shared.OperationsImpl;
import static com.affymetrix.igb.shared.Selections.*;

/**
 *
 * @author hiralv
 */
public class GraphOperationsImpl extends OperationsImpl {

    boolean is_listening = true; // used to turn on and off listening to GUI events
    private javax.swing.JButton combineB, splitB;

    GraphOperationsImpl(IGBService igbS) {
        super(igbS);
    }

    @Override
    protected void initComponents(IGBService igbS) {
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
        is_listening = false;

        combineB.setEnabled(enable && graphGlyphs.size() > 1 && !isAnyJoined());
        splitB.setEnabled(enable && isAnyJoined());

        is_listening = true;
    }
}

package org.lorainelab.igb.track.operations;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import static com.affymetrix.igb.shared.Selections.graphGlyphs;
import static com.affymetrix.igb.shared.Selections.isAnyJoined;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.track.operations.api.OperationsPanel;
import org.osgi.service.component.annotations.ReferenceCardinality;

import javax.swing.GroupLayout;
import javax.swing.JButton;

/**
 *
 * @author hiralv
 */
@Component(name = GraphOperationsImpl.COMPONENT_NAME, immediate = true, service = GraphOperationsImpl.class)
public class GraphOperationsImpl extends OperationsPanel {

    public static final String COMPONENT_NAME = "GraphOperationsImpl";
    private JButton combineB, splitB;
    boolean is_listening = true; // used to turn on and off listening to GUI events
    private IgbService igbService;

    public GraphOperationsImpl() {
        super();
        categories = new FileTypeCategory[]{FileTypeCategory.Graph, FileTypeCategory.Mismatch};
    }

    @Activate
    public void activate() {
        initComponents(igbService);
        init(igbService);
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    protected void initComponents(IgbService igbS) {
        combineB = new javax.swing.JButton(new CombineGraphsAction(igbS));
        splitB = new javax.swing.JButton(new SplitGraphsAction(igbS));

        GroupLayout layout = new GroupLayout(getBtPanel());
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

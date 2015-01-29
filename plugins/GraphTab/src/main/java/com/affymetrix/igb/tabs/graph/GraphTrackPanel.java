package com.affymetrix.igb.tabs.graph;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.service.api.IgbTabPanelI;
import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.TrackViewPanel;
import static com.affymetrix.igb.shared.Selections.*;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;
import com.lorainelab.igb.track.operations.api.OperationsPanelService;

/**
 *
 * @author hiralv
 */
@Component(name = GraphTrackPanel.COMPONENT_NAME, provide = IgbTabPanelI.class, immediate = true)
public class GraphTrackPanel extends TrackViewPanel {

    public static final String COMPONENT_NAME = "GraphTrackPanel";
    private static final long serialVersionUID = 1L;
    public static final java.util.ResourceBundle BUNDLE = java.util.ResourceBundle.getBundle("graph");
    private static final int TAB_POSITION = 2;
    private IgbService igbService;
    
    
    public GraphTrackPanel() {
        super(BUNDLE.getString("graphTab"), BUNDLE.getString("graphTab"), BUNDLE.getString("graphTooltip"), false, TAB_POSITION);
        getCustomButton().setText("Thresholding...");
        getClearButton().setText("Clear Graph");
        getSaveButton().setText("Save Graph");
        getDeleteButton().setText(("Delete Graph"));
    }

    //this sort of initialization hack is only needed until there is time to create proper service dependency
    private void init() {
        StylePanelImpl stylePanel = new StylePanelImpl(igbService) {

            @Override
            protected void setStyles() {
                styles.addAll(Selections.graphStyles);
            }

            @Override
            protected boolean isAnyFloat() {
                return Selections.isAnyFloat();
            }
        };
        YScaleAxisGUI yscaleAxis = new YScaleAxisGUI(igbService);
        GraphPanelImpl graphPanel = new GraphPanelImpl(igbService);

        addPanel(stylePanel);
        addPanel(yscaleAxis);
        addPanel(graphPanel);

    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
        init();
    }

    @Reference(optional = false)
    public void setOperationsPanel(OperationsPanelService operationsPanelService) {
        this.addPanel(operationsPanelService.getGraphOperationsPanel());
    }

    @Override
    protected void selectAllButtonActionPerformedA(java.awt.event.ActionEvent evt) {
        SelectAllAction.getAction().execute(FileTypeCategory.Graph, FileTypeCategory.Mismatch);
    }

    @Override
    protected void selectAllButtonReset() {

    }

    @Override
    protected void customButtonActionPerformedA(java.awt.event.ActionEvent evt) {
        ThresholdingAction.getAction().actionPerformed(evt);
    }

    @Override
    protected void clearButtonReset() {
        javax.swing.JButton clearButton = getClearButton();
        clearButton.setEnabled(graphSyms.size() > 0);
    }

    @Override
    protected void saveButtonReset() {
        javax.swing.JButton saveButton = getSaveButton();
        saveButton.setEnabled(graphSyms.size() > 0);
    }

    @Override
    protected void deleteButtonReset() {
        javax.swing.JButton deleteButton = getDeleteButton();
        deleteButton.setEnabled(graphSyms.size() > 0);
    }

    @Override
    protected void restoreButtonReset() {
        javax.swing.JButton restoreButton = getRestoreButton();
        restoreButton.setEnabled(graphSyms.size() > 0);
    }

    @Override
    protected void customButtonReset() {
        getCustomButton().setEnabled(!graphSyms.isEmpty());
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }
}

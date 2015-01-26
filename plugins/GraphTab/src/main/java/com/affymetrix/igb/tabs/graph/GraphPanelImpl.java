package com.affymetrix.igb.tabs.graph;

import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.style.GraphType;
import com.affymetrix.genometry.style.HeatMap;
import com.affymetrix.genometry.style.DynamicStyleHeatMap;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.shared.Actions;
import com.affymetrix.igb.shared.Selections;
import static com.affymetrix.igb.shared.Selections.*;

public class GraphPanelImpl extends GraphPanel implements Selections.RefreshSelectionListener {

    private static final long serialVersionUID = 1L;
    protected IGBService igbService;

    public GraphPanelImpl(IGBService _igbService) {
        super();
        igbService = _igbService;
        resetAll();
        Selections.addRefreshSelectionListener(this);
    }

    private void updateDisplay() {
        updateDisplay(true, true);
    }

    private void updateDisplay(final boolean preserveX, final boolean preserveY) {
        ThreadUtils.runOnEventQueue(() -> {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
            igbService.getSeqMapView().updatePanel(preserveX, preserveY);
        });
    }

    private void refreshView() {
        ThreadUtils.runOnEventQueue(() -> igbService.getSeqMap().updateWidget());
    }

    @Override
    protected void labelCheckBoxActionPerformedA(ActionEvent evt) {
        final JCheckBox labelCheckBox = getLabelCheckBox();
        boolean b = labelCheckBox.isSelected();
        for (GraphState state : graphStates) {
            state.setShowLabel(b);
        }
        updateDisplay();
    }

    @Override
    protected void floatCheckBoxActionPerformedA(ActionEvent evt) {
        final JCheckBox floatCheckBox = getFloatCheckBox();
        Actions.setFloatTier(floatCheckBox.isSelected(), evt);
    }

    @Override
    protected void YAxisCheckBoxActionPerformedA(ActionEvent evt) {
        final JCheckBox YAxisCheckBox = getYAxisCheckBox();
        boolean b = YAxisCheckBox.isSelected();
        for (GraphState state : graphStates) {
            state.setShowAxis(b);
        }
        updateDisplay();
    }

    @Override
    protected void buttonGroup1ActionPerformedA(ActionEvent evt) {
        GraphType selectedMode = null;
        if (getGraphStyleLineRadioButton().isSelected()) {
            selectedMode = GraphType.LINE_GRAPH;
        }
        if (getGraphStyleBarRadioButton().isSelected()) {
            selectedMode = GraphType.EMPTY_BAR_GRAPH;
        }
        if (getGraphStyleFilledBarRadioButton().isSelected()) {
            selectedMode = GraphType.FILL_BAR_GRAPH;
        }
        if (getGraphStyleStairStepRadioButton().isSelected()) {
            selectedMode = GraphType.STAIRSTEP_GRAPH;
        }
        if (getGraphStyleDotRadioButton().isSelected()) {
            selectedMode = GraphType.DOT_GRAPH;
        }
        if (getGraphStyleMinMaxAvgRadioButton().isSelected()) {
            selectedMode = GraphType.MINMAXAVG;
        }
        if (getGraphStyleHeatMapRadioButton().isSelected()) {
            selectedMode = GraphType.HEAT_MAP;
        }

        for (GraphState state : graphStates) {
            state.setGraphStyle(selectedMode);
        }

        buttonGroup1Reset();
        graphStyleHeatMapComboBoxReset();
        // TODO : Need to create method in igbService to change graph type.
        updateDisplay();
    }

    @Override
    protected void graphStyleHeatMapComboBoxActionPerformedA(ActionEvent evt) {
        if (graphStates.isEmpty() || !is_listening) {
            return;
        }
        JComboBox heatMapComboBox = getGraphStyleHeatMapComboBox();
        String name = (String) heatMapComboBox.getSelectedItem();
        if (name == null) {
            return;
        }
        if (HeatMap.FOREGROUND_BACKGROUND.equals(name)) {
            //					gl.setShowGraph(true);
            graphStates.stream().filter(state -> state.getGraphStyle() == GraphType.HEAT_MAP).filter(state -> !(state.getHeatMap() instanceof DynamicStyleHeatMap)).forEach(state -> {
                state.setHeatMap(new DynamicStyleHeatMap(HeatMap.FOREGROUND_BACKGROUND, state.getTierStyle(), 0.0f, 0.5f));
            });
        } else {
            HeatMap hm = HeatMap.getStandardHeatMap(name);
            if (hm != null) {
                //						gl.setShowGraph(true);
                graphStates.stream().filter(state -> state.getGraphStyle() == GraphType.HEAT_MAP).forEach(state -> {
//						gl.setShowGraph(true);
                    state.setHeatMap(hm);
                });
            }
        }
        refreshView();
    }

    @Override
    protected void labelCheckBoxReset() {
        JCheckBox labelCheckBox = getLabelCheckBox();
        labelCheckBox.setEnabled(isAllGraph());
        boolean allLabel = isAllGraph();
        for (GraphState state : graphStates) {
            if (!state.getShowLabel()) {
                allLabel = false;
                break;
            }
        }
        labelCheckBox.setSelected(allLabel);
    }

    @Override
    protected void floatCheckBoxReset() {
        JCheckBox floatCheckBox = getFloatCheckBox();

        boolean allFloat = isAllGraph();
        for (GraphState state : graphStates) {
            if (!state.getTierStyle().isFloatTier()) {
                allFloat = false;
                break;
            }
        }

        floatCheckBox.setEnabled(isAllGraph() && !isAnyJoined()
                // Floating is not allowed if only one track loaded
                && ((igbService != null && igbService.getVisibleTierGlyphs() != null && igbService.getVisibleTierGlyphs().size() > 2) || allFloat)
        );
        floatCheckBox.setSelected(allFloat);
    }

    @Override
    protected void YAxisCheckBoxReset() {
        JCheckBox yAxisCheckBox = getYAxisCheckBox();
        yAxisCheckBox.setEnabled(isAllGraph());
        boolean allYAxis = isAllGraph();
        for (GraphState state : graphStates) {
            if (!state.getShowAxis()) {
                allYAxis = false;
                break;
            }
        }
        yAxisCheckBox.setSelected(isAllGraph() && allYAxis);
    }

    @Override
    protected void buttonGroup1Reset() {
        getGraphStyleLineRadioButton().setEnabled(isAllGraph());
        getGraphStyleBarRadioButton().setEnabled(isAllGraph());
        getGraphStyleFilledBarRadioButton().setEnabled(isAllGraph());
        getGraphStyleStairStepRadioButton().setEnabled(isAllGraph());
        getGraphStyleDotRadioButton().setEnabled(isAllGraph());
        getGraphStyleMinMaxAvgRadioButton().setEnabled(isAllGraph());
        getGraphStyleHeatMapRadioButton().setEnabled(isAllGraph());
        if (isAllGraph()) {
            GraphType graphType = null;
            boolean graphTypeSet = false;
            for (GraphState state : graphStates) {
                if (graphType == null && !graphTypeSet) {
                    graphType = state.getGraphStyle();
                    graphTypeSet = true;
                } else if (graphType != state.getGraphStyle()) {
                    graphType = null;
                    break;
                }
            }
            if (graphType == null || isAllGraphStyleLocked()) {
                unselectGraphStyle();
                if (isAllGraphStyleLocked()) {
                    getGraphStyleLineRadioButton().setEnabled(false);
                    getGraphStyleBarRadioButton().setEnabled(false);
                    getGraphStyleFilledBarRadioButton().setEnabled(false);
                    getGraphStyleStairStepRadioButton().setEnabled(false);
                    getGraphStyleDotRadioButton().setEnabled(false);
                    getGraphStyleMinMaxAvgRadioButton().setEnabled(false);
                    getGraphStyleHeatMapRadioButton().setEnabled(false);
                }
            } else {
                if (graphType == GraphType.HEAT_MAP) {
                    getGraphStyleHeatMapRadioButton().setText("");
                    getGraphStyleHeatMapComboBox().setVisible(true);
                } else {
                    getGraphStyleHeatMapRadioButton().setText("Heat Map");
                    getGraphStyleHeatMapComboBox().setVisible(false);
                }
                switch (graphType) {
                    case LINE_GRAPH:
                        getGraphStyleLineRadioButton().setSelected(true);
                        break;
                    case EMPTY_BAR_GRAPH:
                        getGraphStyleBarRadioButton().setSelected(true);
                        break;
                    case STAIRSTEP_GRAPH:
                        getGraphStyleStairStepRadioButton().setSelected(true);
                        break;
                    case DOT_GRAPH:
                        getGraphStyleDotRadioButton().setSelected(true);
                        break;
                    case MINMAXAVG:
                        getGraphStyleMinMaxAvgRadioButton().setSelected(true);
                        break;
                    case HEAT_MAP:
                        getGraphStyleHeatMapRadioButton().setSelected(true);
                        break;
                }
            }
        } else {
            unselectGraphStyle();
            getGraphStyleHeatMapRadioButton().setText("Heat Map");
            getGraphStyleHeatMapComboBox().setVisible(false);
        }
    }

    @Override
    protected void graphStyleHeatMapComboBoxReset() {
        JComboBox heatMapComboBox = getGraphStyleHeatMapComboBox();
        if (isAllGraph()) {
            boolean allHeatMap = true;
            HeatMap heatMap = null;
            boolean heatMapSet = false;
            for (GraphState state : graphStates) {
                if (state.getGraphStyle() != GraphType.HEAT_MAP) {
                    allHeatMap = false;
                    break;
                }
                if (heatMap == null && !heatMapSet) {
                    heatMap = state.getHeatMap();
                    heatMapSet = true;
                } else if (heatMap != state.getHeatMap()) {
                    heatMap = null;
                    break;
                }
            }
            if (allHeatMap) {
                heatMapComboBox.setEnabled(true);
                if (heatMap == null) {
                    heatMapComboBox.setSelectedIndex(-1);
                } else {
                    heatMapComboBox.setSelectedItem(heatMap.getName());
                }
            } else {
                heatMapComboBox.setEnabled(false);
            }
        } else {
            heatMapComboBox.setEnabled(false);
        }
    }

    @Override
    public void selectionRefreshed() {
        resetAll();
    }
}

package com.affymetrix.igb.tabs.graph;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.common.ExtensionPointListener;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleContext;

public class Activator extends WindowActivator implements org.osgi.framework.BundleActivator {

    static FileTypeCategory[] categories = new FileTypeCategory[]{FileTypeCategory.Graph, FileTypeCategory.Mismatch};

    @Override
    protected IGBTabPanel getPage(BundleContext bundleContext, IGBService igbService) {

        GraphTrackPanel tabPanel = new GraphTrackPanel(igbService) {
            @Override
            protected void selectAllButtonActionPerformedA(java.awt.event.ActionEvent evt) {
                SelectAllAction.getAction().execute(FileTypeCategory.Graph, FileTypeCategory.Mismatch);
            }
        };

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

        final GraphOperationsImpl trackOperation = new GraphOperationsImpl(igbService) {
            @Override
            protected boolean addThisOperator(Operator operator) {
                for (FileTypeCategory category : categories) {
                    if (operator.getOperandCountMin(category) > 0) {
                        return true;
                    }
                }
                return false;
            }
        };

        ExtensionPointHandler<Operator> operatorExtensionPoint = ExtensionPointHandler.getOrCreateExtensionPoint(bundleContext, Operator.class);
        operatorExtensionPoint.addListener(new ExtensionPointListener<Operator>() {

            @Override
            public void addService(Operator operator) {
                trackOperation.addOperator(operator);
            }

            @Override
            public void removeService(Operator operator) {
                trackOperation.removeOperator(operator);
            }
        });

        tabPanel.addPanel(stylePanel);
        tabPanel.addPanel(yscaleAxis);
        tabPanel.addPanel(graphPanel);
        tabPanel.addPanel(trackOperation);

        return tabPanel;
    }
}

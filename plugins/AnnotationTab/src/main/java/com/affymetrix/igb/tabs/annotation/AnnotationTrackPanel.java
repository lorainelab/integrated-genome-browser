package com.affymetrix.igb.tabs.annotation;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.service.api.IgbTabPanelI;
import com.affymetrix.igb.shared.OperationsImpl;
import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.igb.shared.Selections;
import static com.affymetrix.igb.shared.Selections.annotSyms;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.shared.TrackViewPanel;

/**
 *
 * @author hiralv
 */
@Component(name = AnnotationTrackPanel.COMPONENT_NAME, provide = IgbTabPanelI.class)
public class AnnotationTrackPanel extends TrackViewPanel {

    public static final String COMPONENT_NAME = "AnnotationTrackPanel";
    private static final long serialVersionUID = 1L;
    public static final java.util.ResourceBundle BUNDLE = java.util.ResourceBundle.getBundle("annotation");
    private static FileTypeCategory[] categories = new FileTypeCategory[]{FileTypeCategory.Annotation, FileTypeCategory.Alignment, FileTypeCategory.ProbeSet};
    private static final int TAB_POSITION = 1;
    private OperationsImpl trackOperation;
    private IGBService igbService;

    public AnnotationTrackPanel() {
        super(BUNDLE.getString("annotationTab"), BUNDLE.getString("annotationTab"), BUNDLE.getString("annotationTooltip"), false, TAB_POSITION);
    }

    private void init() {
        getCustomButton().setText("Other Options...");
        StylePanelImpl stylePanel = new StylePanelImpl(igbService) {

            @Override
            protected void setStyles() {
                styles.addAll(Selections.annotStyles);
                styles.addAll(Selections.axisStyles);
            }

            @Override
            protected boolean isAnyFloat() {
                return false;
            }
        };
        AnnotationPanelImpl annotationPanel = new AnnotationPanelImpl(igbService);

        trackOperation = new OperationsImpl(igbService) {
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
        this.addPanel(stylePanel);
        this.addPanel(annotationPanel);
        this.addPanel(trackOperation);
    }

    @Reference(multiple = true, unbind = "removeTrackOperator", optional = true, dynamic = true)
    public void addTrackOperator(Operator operator) {
        trackOperation.addOperator(operator);
    }

    @Reference(optional = false)
    public void setIgbService(IGBService igbService) {
        this.igbService = igbService;
        init();
    }

    public void removeTrackOperator(Operator operator) {
        trackOperation.removeOperator(operator);
    }

    @Override
    protected void selectAllButtonReset() {

    }

    @Override
    protected void selectAllButtonActionPerformedA(java.awt.event.ActionEvent evt) {
        SelectAllAction.getAction().execute(categories);
    }

    @Override
    protected void customButtonActionPerformedA(java.awt.event.ActionEvent evt) {
        igbService.openPreferencesOtherPanel();
    }

    @Override
    protected void clearButtonReset() {
        javax.swing.JButton clearButton = getClearButton();
        clearButton.setEnabled(annotSyms.size() > 0);
    }

    @Override
    protected void saveButtonReset() {
        javax.swing.JButton saveButton = getSaveButton();
        saveButton.setEnabled(annotSyms.size() > 0);
    }

    @Override
    protected void deleteButtonReset() {
        javax.swing.JButton deleteButton = getDeleteButton();
        deleteButton.setEnabled(annotSyms.size() > 0);
    }

    @Override
    protected void restoreButtonReset() {
        javax.swing.JButton restoreButton = getRestoreButton();
        restoreButton.setEnabled(annotSyms.size() > 0);
    }

    @Override
    protected void customButtonReset() {

    }

    @Override
    public boolean isEmbedded() {
        return true;
    }

}

package com.affymetrix.igb.tabs.annotation;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.OperationsImpl;
import com.affymetrix.igb.shared.Selections;
import static com.affymetrix.igb.shared.Selections.annotSyms;
import com.affymetrix.igb.shared.StylePanelImpl;
import com.affymetrix.igb.shared.TrackViewPanel;
import static com.affymetrix.igb.tabs.annotation.AnnotationTrackPanel.COMPONENT_NAME;
import com.affymetrix.igb.osgi.service.IGBTabPanelI;
import java.util.ResourceBundle;

/**
 *
 * @author hiralv
 */
@Component(name = COMPONENT_NAME, provide = {IGBTabPanelI.class})
public class AnnotationTrackPanel extends TrackViewPanel {

    public static final String COMPONENT_NAME = "AnnotationTrackPanel";
    private static final long serialVersionUID = 1L;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("annotation");
    private static final int TAB_POSITION = 1;
    private static final FileTypeCategory[] categories = new FileTypeCategory[]{FileTypeCategory.Annotation, FileTypeCategory.Alignment, FileTypeCategory.ProbeSet};

    private IGBService igbService;
    private OperationsImpl trackOperation;
    private StylePanelImpl stylePanel;

    @Activate
    public void activate() {
        super.activate(BUNDLE.getString("annotationTab"), BUNDLE.getString("annotationTab"), BUNDLE.getString("annotationTooltip"), false, TAB_POSITION);
        getCustomButton().setText("Other Options...");
        init();
        addPanel(stylePanel);
        addPanel(this);
        addPanel(trackOperation);
    }

    public IGBService getIgbService() {
        return igbService;
    }

    @Reference
    public void setIgbService(IGBService igbService) {
        this.igbService = igbService;
    }

    @Reference(multiple = true, unbind = "removeService", service = Operator.class)
    public void addService(Operator operator) {
        trackOperation.addOperator(operator);
    }

    public void removeService(Operator operator) {
        trackOperation.removeOperator(operator);
    }

    private void init() {
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

        stylePanel = new StylePanelImpl(igbService) {

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
    }

    @Override
    protected void selectAllButtonReset() {

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

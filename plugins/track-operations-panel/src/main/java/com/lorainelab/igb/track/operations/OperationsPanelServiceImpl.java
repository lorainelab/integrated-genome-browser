package com.lorainelab.igb.track.operations;

import com.lorainelab.igb.track.operations.api.OperationsPanel;
import com.lorainelab.igb.track.operations.api.OperationsPanelService;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.igb.service.api.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = OperationsPanelServiceImpl.COMPONENT_NAME, immediate = true)
public class OperationsPanelServiceImpl implements OperationsPanelService {

    private static final Logger logger = LoggerFactory.getLogger(OperationsPanelServiceImpl.class);
    public static final String COMPONENT_NAME = "OperationsPanelServiceImpl";
    private static FileTypeCategory[] annotaionFileTypeCategories = new FileTypeCategory[]{FileTypeCategory.Annotation, FileTypeCategory.Alignment, FileTypeCategory.ProbeSet};
    private static FileTypeCategory[] graphFileTypeCategories = new FileTypeCategory[]{FileTypeCategory.Graph, FileTypeCategory.Mismatch};

    private IgbService igbService;
    private OperationsPanel annotationOperationsPanel;
    private OperationsPanel graphOperationsPanel;

    public OperationsPanelServiceImpl() {
        logger.info("Starting OperationsPanelService");
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
        annotationOperationsPanel = new OperationsPanel(igbService, annotaionFileTypeCategories);
        graphOperationsPanel = new GraphOperationsImpl(igbService, graphFileTypeCategories);
    }

    @Reference(multiple = true, unbind = "removeTrackOperator", optional = true, dynamic = true)
    public void addTrackOperator(Operator operator) {
        annotationOperationsPanel.addOperator(operator);
    }

    public void removeTrackOperator(Operator operator) {
        annotationOperationsPanel.removeOperator(operator);
    }

    @Override
    public OperationsPanel getAnnotationOperationsPanel() {
        return annotationOperationsPanel;
    }

    @Override
    public OperationsPanel getGraphOperationsPanel() {
        return graphOperationsPanel;
    }

}

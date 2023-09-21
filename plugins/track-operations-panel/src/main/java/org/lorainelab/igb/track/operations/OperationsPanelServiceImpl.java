package org.lorainelab.igb.track.operations;

import org.lorainelab.igb.track.operations.api.OperationsPanel;
import org.lorainelab.igb.track.operations.api.OperationsPanelService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
    private AnnotationOperationsImpl annotationOperationsImpl;
    private GraphOperationsImpl graphOperationsImpl;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setAnnotationOperationsImpl(AnnotationOperationsImpl annotationOperationsImpl) {
        this.annotationOperationsImpl = annotationOperationsImpl;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setGraphOperationsImpl(GraphOperationsImpl graphOperationsImpl) {
        this.graphOperationsImpl = graphOperationsImpl;
    }

    @Override
    public OperationsPanel getAnnotationOperationsPanel() {
        return annotationOperationsImpl;
    }

    @Override
    public OperationsPanel getGraphOperationsPanel() {
        return graphOperationsImpl;
    }

}

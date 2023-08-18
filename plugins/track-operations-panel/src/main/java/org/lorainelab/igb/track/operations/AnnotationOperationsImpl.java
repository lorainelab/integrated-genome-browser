/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.track.operations;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.track.operations.api.OperationsPanel;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dcnorris
 */
@Component(name = AnnotationOperationsImpl.COMPONENT_NAME, immediate = true, service = AnnotationOperationsImpl.class)
public class AnnotationOperationsImpl extends OperationsPanel {

    public static final String COMPONENT_NAME = "AnnotationOperationsImpl";
    private volatile List<Operator> initializationHolder;
    private IgbService igbService;

    public AnnotationOperationsImpl() {
        categories = new FileTypeCategory[]{FileTypeCategory.Annotation, FileTypeCategory.Alignment, FileTypeCategory.ProbeSet};
        initializationHolder = new ArrayList<>();
    }

    @Activate
    public void activate() {
        init(igbService);
        initializationHolder.forEach(this::addOperator);
        initializationHolder.clear();
    }

    @Reference(unbind = "removeTrackOperator", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addTrackOperator(Operator operator) {
        if (igbService == null) {
            initializationHolder.add(operator);
        } else {
            addOperator(operator);
        }
    }

    public void removeTrackOperator(Operator operator) {
        if (igbService == null) {
            initializationHolder.remove(operator);
        } else {
            removeOperator(operator);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }
}
